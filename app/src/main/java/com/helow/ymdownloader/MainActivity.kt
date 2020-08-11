package com.helow.ymdownloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(materialToolbar)
        navController = findNavController(R.id.fragment)

        materialToolbar.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            NotificationManagerCompat.from(this).createNotificationChannel(NotificationChannel("downloads", "Downloads", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
            })
    }

    override fun onSupportNavigateUp() = super.onSupportNavigateUp() || navController.navigateUp()
}