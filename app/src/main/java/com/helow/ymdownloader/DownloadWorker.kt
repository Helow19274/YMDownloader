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
import com.helow.ymdownloader.model.Artist
import com.helow.ymdownloader.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.MessageDigest

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notificationId = inputData.getInt("notification_id", 1)
    private val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
    private val notificationTemplate = NotificationCompat.Builder(applicationContext, "downloads")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(android.R.drawable.ic_delete, "Отмена", intent)
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
            }
        } catch (e: Exception) {
            toast(e.localizedMessage.orEmpty(), true)
            return Result.failure()
        }
        return Result.success()
    }

    private suspend fun downloadTrack(trackId: Int) {
        val track = try {
            service.getTrack(trackId)
        } catch (e: Exception) {
            toast("Некорректная ссылка")
            return
        }
        val title = getTitle(track.track)
        updateNotification(0, 1, title)
        saveFile(title, track.track)
        updateNotification(1, 1, title)
    }

    private fun downloadAlbum(albumId: Int) {
        toast("Not implemented yet")
    }

    private fun downloadArtist(artistId: Int) {
        toast("Not implemented yet")
    }

    private suspend fun updateNotification(progress: Int, max: Int, title: String? = "Загрузка") {
        val notification = notificationTemplate.apply {
            setContentText("$progress из $max")
            setProgress(max, progress, false)
            setContentTitle(title)
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

    private fun getTitle(track: Track): String {
        var title = "${getArtists(track.artists)} - ${track.title}"
        if (track.version != null)
            title += " (${track.version})"
        return title.replace("?", "")
    }

    private fun getArtists(allArtists: List<Artist>): String {
        val artists = mutableListOf<String>()
        val composers = mutableListOf<String>()

        for (artist in allArtists)
            if (!artist.composer)
                artists.add(artist.name)
            else
                composers.add(artist.name)

        return if (artists.isNotEmpty())
            artists.joinToString()
        else
            composers.joinToString()
    }

    private fun toast(message: String, long: Boolean = false) {
        Handler(Looper.getMainLooper()).post { toast(applicationContext, message, long) }
    }
}