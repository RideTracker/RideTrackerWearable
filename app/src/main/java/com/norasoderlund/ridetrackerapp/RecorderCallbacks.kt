package com.norasoderlund.ridetrackerapp

interface RecorderCallbacks {
    fun onLocationUpdate(event: RecorderLocationEvent);
    fun onStateInfoEvent(event: RecorderStateInfoEvent);
    fun onDurationEvent(event: RecorderDurationEvent);
    fun onSpeedEvent(event: RecorderSpeedEvent);
    //fun onSpeedStatsEvent(event: RecorderSpeedStatsEvent);
    fun onDistanceEvent(event: RecorderDistanceEvent);
    fun onElevationEvent(event: RecorderElevationEvent);
    fun onSessionEndEvent(event: RecorderSessionEndEvent);
}