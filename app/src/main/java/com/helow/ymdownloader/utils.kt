package com.helow.ymdownloader

import android.content.Context
import android.widget.Toast

fun toast(context: Context, message: CharSequence, long: Boolean = false){
    val length = if (long)
        Toast.LENGTH_LONG
    else
        Toast.LENGTH_SHORT
    Toast.makeText(context, message, length).show()
}