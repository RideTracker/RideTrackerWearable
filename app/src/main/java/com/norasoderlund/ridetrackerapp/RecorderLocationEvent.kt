package com.norasoderlund.ridetrackerapp

import com.google.android.gms.location.LocationResult

data class RecorderLocationEvent(val result: LocationResult, val recording: Boolean)
