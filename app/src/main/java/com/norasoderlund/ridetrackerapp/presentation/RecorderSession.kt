package com.norasoderlund.ridetrackerapp.presentation

data class RecorderSession(
    var id: String,
    var locations: MutableList<RecorderSessionLocation>,
    var battery: MutableList<RecorderSessionBattery>
)