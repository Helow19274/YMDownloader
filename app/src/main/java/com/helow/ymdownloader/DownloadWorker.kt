package com.helow.ymdownloader

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.helow.ymdownloader.api.Api
import com.helow.ymdownloader.model.Album
import com.helow.ymdownloader.model.PartialArtist
import com.helow.ymdownloader.model.PlayList
import com.helow.ymdownloader.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.MessageDigest

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notificationId = inputData.getInt("notification_id", 1)
    private val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
    private val service = Api.service
    private lateinit var file: DocumentFile

    override suspend fun doWork(): Result {
        try {
            val data = inputData.keyValueMap
            val uri = applicationContext.contentResolver.persistedUriPermissions.firstOrNull()?.uri ?: return Result.failure()
            file = DocumentFile.fromTreeUri(applicationContext, uri)!!
            when {
                "track" in data -> downloadTrack(data["track"] as Int)
                "album" in data -> downloadAlbum(data["album"] as Int)
                "artist" in data -> downloadArtist(data["artist"] as Int)
                "playlists" in data -> downloadPlaylist(data["users"] as String, data["playlists"] as Int)
            }
        } catch (e: Exception) {
            toast(e.localizedMessage.orEmpty(), true)
            return Result.failure()
        }
        return Result.success()
    }

    private suspend fun downloadTrack(trackId: Int) {
        val track = try {
            service.getTrack(trackId).track
        } catch (e: Exception) {
            return toast("Некорректная ссылка")
        }
        val title = getTrackTitle(track)
        updateNotification(0, 1, title)
        saveFile(title, track)
        updateNotification(1, 1, title)
    }

    private suspend fun downloadAlbum(albumId: Int) {
        val album = try {
            service.getAlbum(albumId)
        } catch (e: Exception) {
            return toast("Некорректная ссылка")
        }

        val title = getAlbumTitle(album)
        var counter = 0
        updateNotification(counter, album.trackCount, title)
        file = file.findFile(title) ?: file.createDirectory(title)!!

        album.volumes.forEachIndexed { index, volume ->
            if (album.volumes.size > 1)
                file = file.findFile("Диск ${index + 1}") ?: file.createDirectory("Диск ${index + 1}")!!

            volume.forEach {
                val track = service.getTrack(it.id).track
                saveFile(getTrackTitle(track), track)
                updateNotification(++counter, album.trackCount, title)
            }

            if (album.volumes.size > 1)
                file = file.parentFile!!
        }
    }

    private suspend fun downloadArtist(artistId: Int) {
        val artist = try {
            service.getArtist(artistId)
        } catch (e: Exception) {
            return toast("Некорректная ссылка")
        }

        var counter = 0
        updateNotification(counter, artist.artist.counts.tracks, artist.artist.name)
        file = file.findFile(artist.artist.name) ?: file.createDirectory(artist.artist.name)!!

        artist.albumIds.forEach { albumId ->
            val album = service.getAlbum(albumId)
            val title = getAlbumTitle(album)
            file = file.findFile(title) ?: file.createDirectory(title)!!

            album.volumes.forEachIndexed { index, volume ->
                if (album.volumes.size > 1)
                    file = file.findFile("Диск ${index + 1}") ?: file.createDirectory("Диск ${index + 1}")!!

                volume.forEach {
                    val track = service.getTrack(it.id).track
                    saveFile(getTrackTitle(track), track)
                    updateNotification(++counter, artist.artist.counts.tracks, artist.artist.name)
                }

                if (album.volumes.size > 1)
                    file = file.parentFile!!
            }
            file = file.parentFile!!
        }
    }

    private suspend fun downloadPlaylist(owner: String, playlistId: Int) {
        val playlist = try {
            service.getPlaylist(owner, playlistId).playlist
        } catch (e: Exception) {
            return toast("Некорректная ссылка или приватный плейлист")
        }

        var counter = 0
        val title = getPlaylistTitle(playlist)
        updateNotification(counter, playlist.trackCount, title)
        file = file.findFile(title) ?: file.createDirectory(title)!!

        playlist.tracks.forEach {
            val track = service.getTrack(it.id).track
            saveFile(getTrackTitle(track), track)
            updateNotification(++counter, playlist.trackCount, title)
        }
        file = file.parentFile!!
    }

    private suspend fun updateNotification(progress: Int, max: Int, title: String? = "Загрузка") {
        val notification = NotificationCompat.Builder(applicationContext, "downloads").apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setOnlyAlertOnce(true)
            setContentTitle(title)
            if (progress != max) {
                setContentText("$progress из $max")
                setProgress(max, progress, false)
                addAction(android.R.drawable.ic_delete, "Отмена", intent)
            }
            else
                setContentText("Загружен(о) $max трек(а/ов)")
        }

        setForeground(ForegroundInfo(notificationId, notification.build()))
    }

    private suspend fun saveFile(title: String, track: Track) {
        if (file.findFile("$title.mp3") == null) {
            val info = service.getInfo(track.storageDir)
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest("XGRlBW9FXlekgbPrRHuSiA${info.path}${info.s}".toByteArray())
            val hash = String.format("%032x", BigInteger(1, digest))

            withContext(Dispatchers.IO) {
                val uri = file.createFile("audio/mpeg", title)!!.uri
                val data = service.getFile(info.host, hash, info.ts, info.path).bytes()

                applicationContext.contentResolver.openOutputStream(uri)!!.apply {
                    write(data)
                    flush()
                    close()
                }
            }
        }
    }

    private fun getTrackTitle(track: Track): String {
        var title = "${getArtists(track.artists)} - ${track.title}"
        if (!track.version.isNullOrBlank())
            title += " (${track.version})"
        return title.replace("?", "")
    }

    private fun getAlbumTitle(album: Album): String = "${getArtists(album.artists)} - ${album.title}".replace("?", "")

    private fun getPlaylistTitle(playlist: PlayList): String = "${playlist.owner.name} - ${playlist.title}".replace("?", "")

    private fun getArtists(artists: List<PartialArtist>): String = artists.joinToString { it.name }

    private fun toast(message: String, long: Boolean = false) {
        Handler(Looper.getMainLooper()).post { toast(applicationContext, message, long) }
    }
}
