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
    private val baseUri = context.contentResolver.persistedUriPermissions.firstOrNull()?.uri!!

    override suspend fun doWork(): Result {
        try {
            download()
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post { toast(applicationContext, e.localizedMessage.orEmpty(), true) }
            return Result.failure()
        }
        return Result.success()
    }

    private suspend fun download() {
        val url = inputData.getString("url") ?: throw RuntimeException()
        val track = try {
            service.getTrack(url.split("/").last())
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post { toast(applicationContext, "Некорректная ссылка: $url", true) }
            return
        }
        val title = getTitle(track.track)
        updateNotification(0, 1, title)
        val file = DocumentFile.fromTreeUri(applicationContext, baseUri)!!
        if (file.findFile("$title.mp3") == null) {
            val info = service.getInfo(track.track.storageDir)
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest("XGRlBW9FXlekgbPrRHuSiA${info.path}${info.s}".toByteArray())
            val hash = String.format("%032x", BigInteger(1, digest))

            withContext(Dispatchers.IO) {
                val uri = file.createFile("audio/mpeg", title)!!.uri
                val data = service.getFile(info.host, hash, info.ts, info.path).bytes()
                val stream = applicationContext.contentResolver.openOutputStream(uri)!!
                stream.write(data)
                stream.flush()
                stream.close()
            }
        }
        updateNotification(1, 1)
    }

    private suspend fun updateNotification(progress: Int, max: Int, title: String? = "Загрузка") {
        val notification = notificationTemplate.apply {
            setContentText("$progress из $max")
            setProgress(max, progress, false)
            setContentTitle(title)
        }

        setForeground(ForegroundInfo(notificationId, notification.build()))
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
}