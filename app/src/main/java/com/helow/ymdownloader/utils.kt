package com.helow.ymdownloader

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData

fun toast(context: Context, message: CharSequence, long: Boolean = false) =
    Toast.makeText(context, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

class NetworkConnection(context: Context) : LiveData<Boolean>() {
    companion object {
        @Volatile private var INSTANCE: NetworkConnection? = null

        fun getInstance(context: Context): NetworkConnection =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkConnection(context).also { INSTANCE = it }
            }
    }

    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            postValue(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            postValue(false)
        }
    }

    override fun onActive() {
        super.onActive()
        connectivityManager.registerDefaultNetworkCallback(callback)
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return postValue(false)
        postValue(listOf(NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_CELLULAR, NetworkCapabilities.TRANSPORT_ETHERNET).any { caps.hasTransport(it) })
    }

    override fun onInactive() {
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(callback)
    }
}