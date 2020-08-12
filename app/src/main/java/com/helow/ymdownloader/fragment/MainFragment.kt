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
import com.helow.ymdownloader.R
import com.helow.ymdownloader.toast
import kotlinx.android.synthetic.main.fragment_main.*
import java.net.URL

class MainFragment : Fragment() {
    private lateinit var preferences: SharedPreferences
    private lateinit var snackBar: Snackbar
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

        if (requireActivity().intent.action == Intent.ACTION_SEND)
            url.setText(requireActivity().intent.getStringExtra(Intent.EXTRA_TEXT))

        button.setOnClickListener {
            val text = url.text.toString().trim()
            if (!Patterns.WEB_URL.matcher(text).matches() || URL(text).host != "music.yandex.ru")
                return@setOnClickListener toast(requireContext(), "Некорректная ссылка")

            val notificationId = preferences.getInt("notification_id", 1)

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf("notification_id" to notificationId, "url" to url.text.toString()))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
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
            snackBar = Snackbar.make(requireView(), "Не установлена папка загрузки, установите в настройках", Snackbar.LENGTH_INDEFINITE)
                .setAction("Настройки") {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToSettingsFragment())
                }
            snackBar.show()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::snackBar.isInitialized && snackBar.isShown)
            snackBar.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> findNavController().navigate(MainFragmentDirections.actionMainFragmentToSettingsFragment())
        }
        return super.onOptionsItemSelected(item)
    }
}