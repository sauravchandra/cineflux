package com.cineflux.data.torrent

import android.content.Context
import com.cineflux.data.db.DownloadDao
import com.cineflux.data.db.DownloadEntity
import com.cineflux.data.model.Movie
import com.cineflux.data.model.TorrentInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val torrentEngine: TorrentEngine,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    val liveProgress: StateFlow<Map<String, DownloadProgress>>
        get() = torrentEngine.downloads

    fun startDownload(movie: Movie, torrent: TorrentInfo): Long {
        val magnetUrl = torrent.magnetUrl(movie.title)
        val infoHash = TorrentEngine.extractInfoHash(magnetUrl)

        var downloadId = -1L
        scope.launch {
            val existing = downloadDao.getByInfoHash(infoHash)
            if (existing != null) {
                downloadId = existing.id
                return@launch
            }

            downloadId = downloadDao.insert(
                DownloadEntity(
                    tmdbId = movie.tmdbId,
                    title = movie.title,
                    posterUrl = movie.posterUrl,
                    quality = torrent.quality,
                    magnetUrl = magnetUrl,
                    infoHash = infoHash,
                    totalBytes = torrent.sizeBytes,
                    status = DownloadEntity.STATUS_DOWNLOADING
                )
            )

            DownloadService.addDownload(context, magnetUrl)
        }

        return downloadId
    }

    fun pauseDownload(infoHash: String) {
        DownloadService.pause(context, infoHash)
    }

    fun resumeDownload(infoHash: String) {
        DownloadService.resume(context, infoHash)
    }

    fun removeDownload(infoHash: String) {
        DownloadService.remove(context, infoHash)
    }

    suspend fun getDownload(id: Long): DownloadEntity? = downloadDao.getById(id)
}
