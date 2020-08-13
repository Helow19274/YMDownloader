package com.helow.ymdownloader.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.*
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.google.android.material.snackbar.Snackbar
import com.helow.ymdownloader.DownloadWorker
import com.helow.ymdownloader.NetworkConnection
import com.helow.ymdownloader.R
import com.helow.ymdownloader.api.Api
import com.helow.ymdownloader.toast
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.launch
import java.net.URL

class MainFragment : Fragment() {
    private lateinit var preferences: SharedPreferences
    private lateinit var dirSnackBar: Snackbar
    private lateinit var manager: WorkManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        setHasOptionsMenu(true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = requireContext().getSharedPreferences("notifications", Context.MODE_PRIVATE)
        manager = WorkManager.getInstance(requireContext())

        manager.getWorkInfosForUniqueWorkLiveData("Download").observe(viewLifecycleOwner) {
            manager.pruneWork()
        }

        NetworkConnection.getInstance(requireContext()).observe(viewLifecycleOwner) { connected ->
            if (connected) {
                offlineTextView.visibility = View.GONE
                if (requireContext().contentResolver.persistedUriPermissions.isNotEmpty()) {
                    button.isEnabled = true
                    url.isEnabled = true
                    val text = url.text
                    if (!text.isNullOrBlank())
                        analyzeUrl(text)
                    else {
                        button.isEnabled = false
                        type.text = null
                    }
                }
            }
            else {
                button.isEnabled = false
                url.isEnabled = false
                offlineTextView.visibility = View.VISIBLE
            }
        }

        url.addTextChangedListener {
            if (!it.isNullOrBlank())
                analyzeUrl(it)
            else {
                button.isEnabled = false
                type.text = null
            }
        }

        if (requireActivity().intent.action == Intent.ACTION_SEND)
            url.setText(requireActivity().intent.getStringExtra(Intent.EXTRA_TEXT))

        button.setOnClickListener {
            val notificationId = preferences.getInt("notification_id", 1)

            val data = Data.Builder()
                .putInt("notification_id", notificationId)
                .putAll(parseUrl(url.text.toString().trim())!!)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .build()
            manager.enqueueUniqueWork("Download", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            url.text = null

            val info = manager.getWorkInfosForUniqueWork("Download").get()
            if (info.size > 1)
                toast(requireContext(), "Загрузка добавлена в очередь, место: ${info.size}")

            preferences.edit(commit = true) {
                putInt("notification_id", notificationId + 1)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (requireContext().contentResolver.persistedUriPermissions.isEmpty()) {
            button.isEnabled = false
            url.isEnabled = false
            dirSnackBar = Snackbar.make(requireView(), "Не установлена папка загрузки, установите в настройках", Snackbar.LENGTH_INDEFINITE)
                .setAction("Настройки") {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToSettingsFragment())
                }
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            dirSnackBar.show()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::dirSnackBar.isInitialized && dirSnackBar.isShown)
            dirSnackBar.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> findNavController().navigate(MainFragmentDirections.actionMainFragmentToSettingsFragment())
        }
        return super.onOptionsItemSelected(item)
    }

    private fun parseUrl(text: String): Map<String, Any>? {
        if (!Patterns.WEB_URL.matcher(text).matches())
            return null

        val url = URL(text)
        if (url.host != "music.yandex.ru")
            return null

        val pathList = url.path.removePrefix("/").removeSuffix("/").split("/")
        if (pathList.size % 2 != 0)
            return null

        val pathIterator = pathList.iterator()
        val pathMap = mutableMapOf<String, Any>()
        while (pathIterator.hasNext()) {
            val k = pathIterator.next()
            val v = pathIterator.next()
            if (k == "users")
                pathMap[k] = v
            else
                pathMap[k] = v.toInt()
        }

        if (listOf("track", "album", "artist", "playlists").none { it in pathMap })
            return null

        if (listOf("playlists", "users").count { it in pathMap } == 1)
            return null

        return pathMap
    }

    private fun analyzeUrl(text: CharSequence) {
        val map = try {
            parseUrl(text.toString().trim())
        } catch (e: Exception) {
            type.text = "Некорректная ссылка"
            button.isEnabled = false
            return
        }
        if (map == null) {
            title.text = null
            type.text = "Некорректная ссылка"
            button.isEnabled = false
            return
        }

        lifecycleScope.launch {
            when {
                "track" in map -> {
                    val track = try {
                        Api.service.getTrack(map["track"] as Int).track
                    } catch (e: Exception) {
                        type.text = "Некорректная ссылка"
                        button.isEnabled = false
                        return@launch
                    }
                    title.text = DownloadWorker.getTrackTitle(track)
                    type.text = "Трек"
                }
                "album" in map -> {
                    val album = try {
                        Api.service.getAlbum(map["album"] as Int)
                    } catch (e: Exception) {
                        type.text = "Некорректная ссылка"
                        button.isEnabled = false
                        return@launch
                    }
                    title.text = DownloadWorker.getAlbumTitle(album)
                    type.text = "Альбом"
                }
                "artist" in map -> {
                    val artist = try {
                        Api.service.getArtist(map["artist"] as Int).artist
                    } catch (e: Exception) {
                        type.text = "Некорректная ссылка"
                        button.isEnabled = false
                        return@launch
                    }
                    title.text = artist.name
                    type.text = "Исполнитель"
                }
                "playlists" in map -> {
                    val playlist = try {
                        Api.service.getPlaylist(map["users"] as String, map["playlists"] as Int).playlist
                    } catch (e: Exception) {
                        type.text = "Некорректная ссылка или приватный плейлист"
                        button.isEnabled = false
                        return@launch
                    }
                    title.text = DownloadWorker.getPlaylistTitle(playlist)
                    type.text = "Плейлист"
                }
            }
            button.isEnabled = true
        }
    }
}