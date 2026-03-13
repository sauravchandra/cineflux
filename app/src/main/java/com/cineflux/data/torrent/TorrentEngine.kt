package com.cineflux.data.torrent

import android.content.Context
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: com.cineflux.data.PreferencesManager
) {
    private var sessionManager: Any? = null
    private var scope: CoroutineScope? = null
    private val handles = ConcurrentHashMap<String, Any>()
    private val pendingMagnets = ConcurrentHashMap<String, String>()
    private var nativeAvailable = false

    private val _downloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadProgress>> = _downloads.asStateFlow()

    private val _finishedTorrents = MutableStateFlow<List<String>>(emptyList())
    val finishedTorrents: StateFlow<List<String>> = _finishedTorrents.asStateFlow()

    @Volatile
    private var isStarted = false

    val defaultSavePath: String
        get() {
            val path = preferencesManager.downloadPath
            val dir = File(path)
            if (!dir.exists()) dir.mkdirs()
            return dir.absolutePath
        }

    fun start() {
        if (isStarted) return
        try {
            val settings = org.libtorrent4j.swig.settings_pack()

            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.active_downloads.swigValue(), 5
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.active_seeds.swigValue(), 5
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.active_limit.swigValue(), 15
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.connections_limit.swigValue(), 500
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.max_peerlist_size.swigValue(), 3000
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.download_rate_limit.swigValue(), 0
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.upload_rate_limit.swigValue(), 0
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.max_out_request_queue.swigValue(), 1500
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.request_queue_time.swigValue(), 3
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.piece_timeout.swigValue(), 10
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.peer_timeout.swigValue(), 30
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.send_buffer_watermark.swigValue(), 1024 * 1024
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.send_buffer_watermark_factor.swigValue(), 150
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.send_buffer_low_watermark.swigValue(), 512 * 1024
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.mixed_mode_algorithm.swigValue(), 0
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.max_allowed_in_request_queue.swigValue(), 2000
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.unchoke_slots_limit.swigValue(), -1
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.max_queued_disk_bytes.swigValue(), 10 * 1024 * 1024
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.aio_threads.swigValue(), 4
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.checking_mem_usage.swigValue(), 256
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.suggest_mode.swigValue(), 1
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.peer_turnover.swigValue(), 0
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.max_failcount.swigValue(), 1
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.in_enc_policy.swigValue(), 1
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.out_enc_policy.swigValue(), 1
            )
            settings.set_int(
                org.libtorrent4j.swig.settings_pack.int_types.allowed_enc_level.swigValue(), 3
            )

            settings.set_bool(
                org.libtorrent4j.swig.settings_pack.bool_types.enable_dht.swigValue(), true
            )
            settings.set_bool(
                org.libtorrent4j.swig.settings_pack.bool_types.enable_lsd.swigValue(), true
            )
            settings.set_bool(
                org.libtorrent4j.swig.settings_pack.bool_types.announce_to_all_trackers.swigValue(), true
            )
            settings.set_bool(
                org.libtorrent4j.swig.settings_pack.bool_types.announce_to_all_tiers.swigValue(), true
            )
            settings.set_bool(
                org.libtorrent4j.swig.settings_pack.bool_types.allow_multiple_connections_per_ip.swigValue(), true
            )
            settings.set_bool(
                org.libtorrent4j.swig.settings_pack.bool_types.smooth_connects.swigValue(), false
            )
            settings.set_bool(
                org.libtorrent4j.swig.settings_pack.bool_types.no_atime_storage.swigValue(), true
            )
            settings.set_bool(
                org.libtorrent4j.swig.settings_pack.bool_types.validate_https_trackers.swigValue(), false
            )

            settings.set_str(
                org.libtorrent4j.swig.settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
                "router.bittorrent.com:6881,dht.transmissionbt.com:6881,router.utorrent.com:6881,dht.aelitis.com:6881"
            )

            val sp = org.libtorrent4j.SettingsPack(settings)
            val sm = org.libtorrent4j.SessionManager()
            sm.start(org.libtorrent4j.SessionParams(sp))
            sm.addListener(object : org.libtorrent4j.AlertListener {
                override fun types(): IntArray = intArrayOf(
                    org.libtorrent4j.alerts.AlertType.TORRENT_FINISHED.swig()
                )
                override fun alert(alert: org.libtorrent4j.alerts.Alert<*>) {
                    try {
                        if (alert.type() == org.libtorrent4j.alerts.AlertType.TORRENT_FINISHED) {
                            val msg = alert.message()
                            Log.i(TAG, "FINISHED alert: $msg")
                            val hashMatch = Regex("[0-9a-f]{40}").find(msg.lowercase())
                            if (hashMatch != null) {
                                val hash = hashMatch.value
                                _finishedTorrents.value = _finishedTorrents.value + hash
                            } else {
                                handles.keys.forEach { hash ->
                                    _finishedTorrents.value = _finishedTorrents.value + hash
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Alert error", e)
                    }
                }
            })
            sessionManager = sm
            nativeAvailable = true
            isStarted = true
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope?.launch { pollProgress() }
            Log.i(TAG, "TorrentEngine started (aggressive mode)")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start libtorrent4j", e)
            nativeAvailable = false
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        scope?.cancel()
        scope = null
        handles.clear()
        pendingMagnets.clear()
        try {
            (sessionManager as? org.libtorrent4j.SessionManager)?.stop()
        } catch (_: Exception) { }
        sessionManager = null
    }

    fun addDownload(magnetUrl: String, savePath: String = defaultSavePath): String {
        val infoHash = extractInfoHash(magnetUrl)
        Log.i(TAG, "addDownload: hash=$infoHash")

        if (!nativeAvailable) start()
        if (!nativeAvailable) {
            Log.w(TAG, "Native library unavailable")
            return infoHash
        }

        val saveDir = File(savePath)
        if (!saveDir.exists()) saveDir.mkdirs()

        pendingMagnets[infoHash] = magnetUrl

        scope?.launch {
            try {
                val sm = sessionManager as org.libtorrent4j.SessionManager
                Log.i(TAG, "Resolving metadata...")
                sm.download(magnetUrl, saveDir, org.libtorrent4j.swig.torrent_flags_t())
                Log.i(TAG, "Metadata resolved for $infoHash")

                val handle = sm.find(org.libtorrent4j.Sha1Hash.parseHex(infoHash))
                if (handle != null) {
                    handles[infoHash] = handle
                    pendingMagnets.remove(infoHash)
                    tuneHandle(handle)
                    Log.i(TAG, "Download active: ${handle.torrentFile()?.name() ?: "unknown"}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}", e)
                pendingMagnets.remove(infoHash)
            }
        }

        return infoHash
    }

    private fun tuneHandle(handle: org.libtorrent4j.TorrentHandle) {
        try {
            handle.setFlags(org.libtorrent4j.TorrentFlags.SEQUENTIAL_DOWNLOAD)
            handle.setFlags(org.libtorrent4j.TorrentFlags.AUTO_MANAGED)

            val numPieces = handle.torrentFile()?.numPieces() ?: return
            val boostPieces = minOf(numPieces, 20)
            for (i in 0 until boostPieces) {
                handle.piecePriority(i, org.libtorrent4j.Priority.TOP_PRIORITY)
            }
            for (i in maxOf(0, numPieces - 5) until numPieces) {
                handle.piecePriority(i, org.libtorrent4j.Priority.TOP_PRIORITY)
            }

            Log.i(TAG, "Handle tuned: sequential=true, pieces=$numPieces, boosted first 20 + last 5")
        } catch (e: Exception) {
            Log.w(TAG, "Tune failed: ${e.message}")
        }
    }

    fun pauseDownload(infoHash: String) {
        try {
            (handles[infoHash] as? org.libtorrent4j.TorrentHandle)?.pause()
        } catch (_: Exception) { }
    }

    fun resumeDownload(infoHash: String) {
        try {
            (handles[infoHash] as? org.libtorrent4j.TorrentHandle)?.resume()
        } catch (_: Exception) { }
    }

    fun removeDownload(infoHash: String, deleteFiles: Boolean = false) {
        try {
            val handle = handles[infoHash] as? org.libtorrent4j.TorrentHandle ?: return
            val sm = sessionManager as? org.libtorrent4j.SessionManager ?: return
            val flags = if (deleteFiles) {
                org.libtorrent4j.SessionHandle.DELETE_FILES
            } else {
                org.libtorrent4j.swig.remove_flags_t()
            }
            sm.remove(handle, flags)
            handles.remove(infoHash)
            pendingMagnets.remove(infoHash)
        } catch (_: Exception) { }
    }

    fun getFilePath(infoHash: String): String? {
        try {
            val handle = handles[infoHash] as? org.libtorrent4j.TorrentHandle ?: return null
            val ti = handle.torrentFile() ?: return null
            if (ti.numFiles() == 0) return null

            var largestIdx = 0
            var largestSize = 0L
            for (i in 0 until ti.numFiles()) {
                val size = ti.files().fileSize(i)
                if (size > largestSize) {
                    largestSize = size
                    largestIdx = i
                }
            }

            return "${handle.savePath()}/${ti.files().filePath(largestIdx)}"
        } catch (_: Exception) {
            return null
        }
    }

    private suspend fun pollProgress() {
        var logCounter = 0
        while (isStarted) {
            try {
                val sm = sessionManager as? org.libtorrent4j.SessionManager
                if (sm != null) {
                    pendingMagnets.forEach { (hash, _) ->
                        if (!handles.containsKey(hash)) {
                            try {
                                val h = sm.find(org.libtorrent4j.Sha1Hash.parseHex(hash))
                                if (h != null) {
                                    handles[hash] = h
                                    pendingMagnets.remove(hash)
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }

                val finished = _finishedTorrents.value.toSet()
                val map = handles.mapNotNull { (hash, handleObj) ->
                    if (hash in finished) {
                        return@mapNotNull hash to DownloadProgress(
                            infoHash = hash, name = "Completed", progress = 1f,
                            downloadRate = 0, uploadRate = 0,
                            totalBytes = 0, downloadedBytes = 0,
                            seeds = 0, peers = 0, state = TorrentState.FINISHED
                        )
                    }
                    val handle = handleObj as? org.libtorrent4j.TorrentHandle
                        ?: return@mapNotNull null
                    val status = try {
                        if (!handle.isValid()) {
                            handles.remove(hash)
                            return@mapNotNull null
                        }
                        handle.status()
                    } catch (_: Throwable) {
                        handles.remove(hash)
                        return@mapNotNull null
                    }
                    val progress = DownloadProgress(
                        infoHash = hash,
                        name = status.name() ?: "Unknown",
                        progress = status.progress(),
                        downloadRate = status.downloadRate(),
                        uploadRate = status.uploadRate(),
                        totalBytes = status.totalWanted(),
                        downloadedBytes = status.totalWantedDone(),
                        seeds = status.numSeeds(),
                        peers = status.numPeers(),
                        state = mapState(status.state())
                    )
                    if (logCounter % 5 == 0) {
                        Log.d(TAG, "${progress.name} ${(progress.progress * 100).toInt()}% " +
                            "${progress.downloadRate / 1024}KB/s S:${progress.seeds} P:${progress.peers} " +
                            "[${progress.state}]")
                    }
                    hash to progress
                }.toMap()
                _downloads.value = map
            } catch (_: Exception) { }
            logCounter++
            delay(1000)
        }
    }

    private fun mapState(state: org.libtorrent4j.TorrentStatus.State): TorrentState = when (state) {
        org.libtorrent4j.TorrentStatus.State.CHECKING_FILES,
        org.libtorrent4j.TorrentStatus.State.CHECKING_RESUME_DATA -> TorrentState.CHECKING
        org.libtorrent4j.TorrentStatus.State.DOWNLOADING_METADATA -> TorrentState.DOWNLOADING_METADATA
        org.libtorrent4j.TorrentStatus.State.DOWNLOADING -> TorrentState.DOWNLOADING
        org.libtorrent4j.TorrentStatus.State.FINISHED -> TorrentState.FINISHED
        org.libtorrent4j.TorrentStatus.State.SEEDING -> TorrentState.SEEDING
        else -> TorrentState.UNKNOWN
    }

    companion object {
        private const val TAG = "TorrentEngine"

        fun extractInfoHash(magnetUrl: String): String {
            val regex = Regex("btih:([a-fA-F0-9]{40})")
            return regex.find(magnetUrl)?.groupValues?.get(1)?.lowercase() ?: ""
        }
    }
}
