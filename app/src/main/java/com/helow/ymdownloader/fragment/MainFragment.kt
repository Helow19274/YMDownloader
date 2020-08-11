package com.helow.ymdownloader.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.snackbar.Snackbar
import com.helow.ymdownloader.DownloadWorker
import com.helow.ymdownloader.R
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {
    private lateinit var prefs: SharedPreferences
    private lateinit var snackbar: Snackbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        setHasOptionsMenu(true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences("notifications", Context.MODE_PRIVATE)
        if (requireActivity().intent.action == Intent.ACTION_SEND)
            url.setText(requireActivity().intent.getStringExtra(Intent.EXTRA_TEXT))

        if (requireContext().contentResolver.persistedUriPermissions.isEmpty()) {
            button.isEnabled = false
            snackbar = Snackbar.make(view, "Не установлена папка загрузки, установите в настройках", Snackbar.LENGTH_INDEFINITE)
                .setAction("Настройки") {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToSettingsFragment())
                }
            snackbar.show()
        }

        button.setOnClickListener {
            if (url.text.isNullOrBlank() or !url.text!!.startsWith("https://music.yandex.ru/"))
                return@setOnClickListener Snackbar.make(view, "Некорректная ссылка", Snackbar.LENGTH_SHORT)
                    .setAction("Закрыть") {}
                    .show()

            val notificationId = prefs.getInt("notification_id", 1)

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf("notification_id" to notificationId, "url" to url.text.toString()))
                .build()
            WorkManager.getInstance(requireContext()).enqueue(request)

            prefs.edit(commit = true) {
                putInt("notification_id", notificationId + 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::snackbar.isInitialized && snackbar.isShown)
            snackbar.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> findNavController().navigate(
                MainFragmentDirections.actionMainFragmentToSettingsFragment()
            )
        }
        return super.onOptionsItemSelected(item)
    }
}