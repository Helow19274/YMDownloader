package com.helow.ymdownloader

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData

fun toast(context: Context, message: CharSequence, long: Boolean = false){
    val length = if (long)
        Toast.LENGTH_LONG
    else
        Toast.LENGTH_SHORT
    Toast.makeText(context, message, length).show()
}

class NetworkConnection(private val context: Context) : LiveData<Boolean>() {
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
        postValue(context.getSystemService<ConnectivityManager>()!!.activeNetworkInfo?.isConnected == true)
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    override fun onInactive() {
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(callback)
    }
}