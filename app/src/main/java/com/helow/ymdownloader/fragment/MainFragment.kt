package com.helow.ymdownloader.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.*
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.google.android.material.snackbar.Snackbar
import com.helow.ymdownloader.DownloadWorker
import com.helow.ymdownloader.NetworkConnection
import com.helow.ymdownloader.R
import com.helow.ymdownloader.toast
import kotlinx.android.synthetic.main.fragment_main.*
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
                }
            }
            else {
                button.isEnabled = false
                url.isEnabled = false
                offlineTextView.visibility = View.VISIBLE
            }
        }

        if (requireActivity().intent.action == Intent.ACTION_SEND)
            url.setText(requireActivity().intent.getStringExtra(Intent.EXTRA_TEXT))

        button.setOnClickListener {
            val pathMap = try {
               parseUrl(url.text.toString().trim()) ?: return@setOnClickListener toast(requireContext(), "Некорректная ссылка")
            } catch (e: Exception) {
                return@setOnClickListener toast(requireContext(), "Некорректная ссылка")
            }

            val notificationId = preferences.getInt("notification_id", 1)

            val data = Data.Builder()
                .putInt("notification_id", notificationId)
                .putAll(pathMap)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .build()
            manager.enqueueUniqueWork("Download", ExistingWorkPolicy.APPEND_OR_REPLACE, request)

            val info = manager.getWorkInfosForUniqueWork("Download").get()
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

    private fun parseUrl(text: String): Map<String, Int>? {
        if (!Patterns.WEB_URL.matcher(text).matches())
            return null

        val url = URL(text)
        if (url.host != "music.yandex.ru")
            return null

        val pathList = url.path.removePrefix("/").removeSuffix("/").split("/")
        if (pathList.size % 2 != 0)
            return null

        val pathIterator = pathList.iterator()
        val pathMap = mutableMapOf<String, Int>()
        while (pathIterator.hasNext())
            pathMap[pathIterator.next()] = pathIterator.next().toInt()

        if (!listOf("track", "album", "artist").any { it in pathMap })
            return null

        return pathMap
    }
}