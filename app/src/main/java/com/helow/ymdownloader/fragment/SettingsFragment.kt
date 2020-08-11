package com.helow.ymdownloader.fragment

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.helow.ymdownloader.R
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : Fragment() {
    private lateinit var contentResolver: ContentResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentResolver = requireContext().contentResolver

        uri.setText(contentResolver.persistedUriPermissions.firstOrNull()?.uri?.lastPathSegment)
        uri.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(Intent.createChooser(intent, "Выберите папку"), 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            val currentDir = contentResolver.persistedUriPermissions.firstOrNull()?.uri
            if (currentDir != null && currentDir != data.data!!)
                contentResolver.releasePersistableUriPermission(currentDir, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(data.data!!, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            uri.setText(data.data!!.lastPathSegment)
        }
    }
}