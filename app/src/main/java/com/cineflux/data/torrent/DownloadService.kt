package com.cineflux.data.torrent

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cineflux.CineFluxApp
import com.cineflux.MainActivity
import com.cineflux.R
import com.cineflux.data.db.DownloadDao
import com.cineflux.data.db.DownloadEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var torrentEngine: TorrentEngine
    @Inject lateinit var downloadDao: DownloadDao

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification("CineFlux Downloads"))
        torrentEngine.start()
        observeFinished()
        syncProgress()
        resumePendingDownloads()
    }

    private fun resumePendingDownloads() {
        serviceScope.launch {
            try {
                val active = downloadDao.getActiveDownloads()
                android.util.Log.i("DownloadService", "Resume check: ${active.size} active download(s) (all statuses: ${active.map { "${it.title}:${it.status}" }})")
                active.forEach { download ->
                    if (download.filePath == null && download.totalBytes > 0) {
                        val videoPath = torrentEngine.getFilePath(download.infoHash)
                        if (videoPath != null) {
                            val file = java.io.File(videoPath)
                            if (file.length() >= download.totalBytes * 0.95) {
                                android.util.Log.i("DownloadService", "Already complete on disk: ${download.title}")
                                downloadDao.markCompleted(download.infoHash, videoPath)
                                return@forEach
                            }
                        }
                    }
                    if (download.magnetUrl.isNotBlank()) {
                        android.util.Log.i("DownloadService", "Resuming (skip check): ${download.title} hash=${download.infoHash.take(16)}")
                        torrentEngine.addDownload(download.magnetUrl, skipCheck = true)
                    } else {
                        android.util.Log.w("DownloadService", "Skip resume - no magnet: ${download.title}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadService", "Resume failed: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD -> {
                val magnetUrl = intent.getStringExtra(EXTRA_MAGNET_URL) ?: return START_STICKY
                val savePath = intent.getStringExtra(EXTRA_SAVE_PATH) ?: torrentEngine.defaultSavePath
                torrentEngine.addDownload(magnetUrl, savePath)
            }
            ACTION_PAUSE -> {
                val hash = intent.getStringExtra(EXTRA_INFO_HASH) ?: return START_STICKY
                torrentEngine.pauseDownload(hash)
                serviceScope.launch {
                    downloadDao.getByInfoHash(hash)?.let {
                        downloadDao.updateStatus(it.id, DownloadEntity.STATUS_PAUSED)
                    }
                }
            }
            ACTION_RESUME -> {
                val hash = intent.getStringExtra(EXTRA_INFO_HASH) ?: return START_STICKY
                torrentEngine.resumeDownload(hash)
                serviceScope.launch {
                    downloadDao.getByInfoHash(hash)?.let {
                        downloadDao.updateStatus(it.id, DownloadEntity.STATUS_DOWNLOADING)
                    }
                }
            }
            ACTION_REMOVE -> {
                val hash = intent.getStringExtra(EXTRA_INFO_HASH) ?: return START_STICKY
                val filePath = torrentEngine.getFilePath(hash)
                torrentEngine.removeDownload(hash, deleteFiles = true)
                serviceScope.launch {
                    val dl = downloadDao.getByInfoHash(hash)
                    if (dl != null) {
                        val pathToClean = filePath ?: dl.filePath
                        if (pathToClean != null) {
                            val video = java.io.File(pathToClean)
                            if (video.exists()) {
                                video.delete()
                                android.util.Log.i("DownloadService", "Deleted video: $pathToClean")
                            }
                            val parent = video.parentFile
                            if (parent != null && parent.absolutePath != torrentEngine.defaultSavePath) {
                                parent.deleteRecursively()
                                android.util.Log.i("DownloadService", "Deleted torrent dir: ${parent.absolutePath}")
                            }
                            val srt = java.io.File(video.parent ?: torrentEngine.defaultSavePath, video.nameWithoutExtension + ".srt")
                            if (srt.exists()) srt.delete()
                        }
                        downloadDao.delete(dl.id)
                    }
                    cleanOrphanedResumeData(hash)
                }
            }
        }
        return START_STICKY
    }

    private fun observeFinished() {
        serviceScope.launch {
            torrentEngine.finishedTorrents.collectLatest { hashes ->
                hashes.forEach { hash ->
                    try {
                        val filePath = torrentEngine.getFilePath(hash)
                        if (filePath != null) {
                            android.util.Log.i("DownloadService", "Marking completed: $hash -> $filePath")
                            downloadDao.markCompleted(hash, filePath)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DownloadService", "observeFinished failed for $hash: ${e.message}")
                    }
                }
            }
        }
    }

    private fun findVideoFile(dir: String): String? {
        val videoExts = listOf(".mkv", ".mp4", ".avi", ".mov", ".wmv")
        return java.io.File(dir).listFiles()
            ?.filter { f -> videoExts.any { f.name.endsWith(it, ignoreCase = true) } }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath
    }

    private fun syncProgress() {
        serviceScope.launch {
            while (isActive) {
                try {
                    torrentEngine.downloads.value.forEach { (hash, progress) ->
                        val status = when (progress.state) {
                            TorrentState.DOWNLOADING -> DownloadEntity.STATUS_DOWNLOADING
                            TorrentState.FINISHED, TorrentState.SEEDING -> DownloadEntity.STATUS_COMPLETED
                            TorrentState.PAUSED -> DownloadEntity.STATUS_PAUSED
                            TorrentState.ERROR -> DownloadEntity.STATUS_FAILED
                            else -> DownloadEntity.STATUS_DOWNLOADING
                        }

                        val isVerifying = progress.state == TorrentState.CHECKING ||
                                progress.state == TorrentState.DOWNLOADING_METADATA
                        val existing = downloadDao.getByInfoHash(hash)
                        if (existing != null) {
                            if (status == DownloadEntity.STATUS_COMPLETED && existing.filePath == null) {
                                val path = torrentEngine.getFilePath(hash)
                                if (path != null) downloadDao.markCompleted(hash, path)
                            }
                            if (!isVerifying) {
                                downloadDao.updateProgress(hash, progress.downloadedBytes, progress.totalBytes, status)
                            }
                        } else {
                            downloadDao.insert(DownloadEntity(
                                tmdbId = 0,
                                title = progress.name,
                                posterUrl = null,
                                quality = "",
                                magnetUrl = "",
                                infoHash = hash,
                                totalBytes = progress.totalBytes,
                                downloadedBytes = progress.downloadedBytes,
                                status = status
                            ))
                        }
                    }

                    val allDownloads = downloadDao.getActiveDownloads()
                    allDownloads.forEach { dl ->
                        if (dl.filePath == null) {
                            val path = torrentEngine.getFilePath(dl.infoHash)
                            if (path != null) {
                                val fileSize = java.io.File(path).length()
                                if (dl.totalBytes > 0 && fileSize >= dl.totalBytes * 0.95) {
                                    android.util.Log.i("DownloadService", "Sync: complete on disk: ${dl.title} (${fileSize / 1_000_000}MB)")
                                    downloadDao.markCompleted(dl.infoHash, path)
                                }
                            }
                        }
                    }
                    val activeCount = allDownloads.count { it.status != DownloadEntity.STATUS_COMPLETED }
                    if (activeCount > 0) {
                        updateNotification("Downloading $activeCount file(s)")
                    } else {
                        updateNotification("No active downloads")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DownloadService", "syncProgress failed: ${e.message}")
                }

                delay(2000)
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CineFluxApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("CineFlux")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun cleanOrphanedResumeData(hash: String) {
        try {
            val saveDir = java.io.File(torrentEngine.defaultSavePath)
            saveDir.listFiles()?.forEach { f ->
                if (f.name.contains(hash, ignoreCase = true) ||
                    f.name.endsWith(".resume") || f.name.endsWith(".parts")) {
                    f.delete()
                    android.util.Log.i("DownloadService", "Cleaned orphan: ${f.name}")
                }
            }
        } catch (_: Exception) { }
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        torrentEngine.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_ADD = "com.cineflux.action.ADD"
        const val ACTION_PAUSE = "com.cineflux.action.PAUSE"
        const val ACTION_RESUME = "com.cineflux.action.RESUME"
        const val ACTION_REMOVE = "com.cineflux.action.REMOVE"
        const val EXTRA_MAGNET_URL = "magnet_url"
        const val EXTRA_SAVE_PATH = "save_path"
        const val EXTRA_INFO_HASH = "info_hash"

        fun addDownload(context: Context, magnetUrl: String, savePath: String? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_ADD
                putExtra(EXTRA_MAGNET_URL, magnetUrl)
                savePath?.let { putExtra(EXTRA_SAVE_PATH, it) }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context, infoHash: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DownloadService::class.java).apply {
                    action = ACTION_PAUSE
                    putExtra(EXTRA_INFO_HASH, infoHash)
                }
            )
        }

        fun resume(context: Context, infoHash: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DownloadService::class.java).apply {
                    action = ACTION_RESUME
                    putExtra(EXTRA_INFO_HASH, infoHash)
                }
            )
        }

        fun remove(context: Context, infoHash: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DownloadService::class.java).apply {
                    action = ACTION_REMOVE
                    putExtra(EXTRA_INFO_HASH, infoHash)
                }
            )
        }
    }
}
