package com.norasoderlund.ridetrackerapp.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

fun getDeviceName(context: Context): String? {
    return try {
        Settings.Secure.getString(context.contentResolver, "bluetooth_name");
    }
    catch(exception: Exception) {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        if (model.lowercase(Locale.getDefault())
                .startsWith(manufacturer.lowercase(Locale.getDefault()))
        ) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }
}


private fun capitalize(s: String?): String {
    if (s.isNullOrEmpty()) {
        return ""
    }
    val first = s[0]
    return if (Character.isUpperCase(first)) {
        s
    } else {
        first.uppercaseChar().toString() + s.substring(1)
    }
}