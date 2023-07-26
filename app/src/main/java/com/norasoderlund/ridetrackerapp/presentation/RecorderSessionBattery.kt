package com.norasoderlund.ridetrackerapp.presentation

import android.location.Location

data class RecorderSessionBattery(
    var batteryLevel: Int,
    var batteryState: Int,
    var lowPowerMode: Boolean,
    var timestamp: Long
);