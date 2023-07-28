package com.norasoderlund.ridetrackerapp

interface RecorderCallbacks {
    fun onLocationUpdate(event: RecorderLocationEvent)
    fun onStateInfoEvent(event: RecorderStateInfoEvent)
    fun onDurationEvent(event: RecorderDurationEvent)
}