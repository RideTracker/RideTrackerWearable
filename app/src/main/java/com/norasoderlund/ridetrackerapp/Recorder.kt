package com.norasoderlund.ridetrackerapp

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.norasoderlund.ridetrackerapp.presentation.MainActivity
import com.norasoderlund.ridetrackerapp.presentation.RecorderSession
import com.norasoderlund.ridetrackerapp.presentation.RecorderSessionLocation
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.floor

class Recorder {
    private lateinit var activity: MainActivity;

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient;

    @SuppressLint("MissingPermission")
    constructor(activity: MainActivity) {
        this.activity = activity;

        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    private var handler: Handler = Handler(Looper.getMainLooper());

    internal var started: Boolean = false;
    internal var paused: Boolean = false;
    internal var accumulatedElevation: Double = 0.0;
    internal var accumulatedDistance: Double = 0.0;
    internal var previousSessionMilliseconds: Long = 0;

    internal var sessions: MutableList<RecorderSession> = mutableListOf();

    internal var lastLocation: Location? = null;

    internal fun toggle() {
        if(!started || paused)
            return start();

        stop();
    }

    @SuppressLint("MissingPermission")
    internal fun start() {
        if(started && !paused)
            return;

        started = true;
        paused = false;

        sessions.add(RecorderSession(UUID.randomUUID().toString(), mutableListOf(), mutableListOf()));

        EventBus.getDefault().post(RecorderStateEvent(started, paused));

        handler.postDelayed(elapsedSecondsRunnable, 1000);

        fusedLocationProviderClient.requestLocationUpdates(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(3)).build(), locationCallback, Looper.getMainLooper());
    }

    internal fun stop() {
        if(!started || paused)
            return;

        paused = true;

        EventBus.getDefault().post(RecorderStateEvent(started, paused));

        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        previousSessionMilliseconds = 0;

        sessions.iterator().forEach { session ->
            if(session.locations.count() < 2)
                return;

            previousSessionMilliseconds += session.locations.last().timestamp - session.locations.first().timestamp;
        }
    }

    internal fun getElapsedMilliseconds(): Long {
        var milliseconds: Long = previousSessionMilliseconds;

        if(!paused && sessions.isNotEmpty() && sessions.last().locations.isNotEmpty()) {
            val calendar = Calendar.getInstance();

            milliseconds += calendar.timeInMillis - sessions.last().locations.first().timestamp;
        }

        return milliseconds;
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            println("onLocationResult");

            //if(result.lastLocation != null)
            //    lastLocation = result.lastLocation;

            for (location in result.locations) {
                println(String.format("Location update received at latitude %f longitude %f", location.latitude, location.longitude));

                if(!paused) {
                    sessions.last().locations.add(RecorderSessionLocation(location, location.time));

                    if (lastLocation != null) {
                        var elevation = location.altitude - lastLocation!!.altitude;
                        var distance = location.distanceTo(lastLocation!!);

                        accumulatedDistance += distance;

                        if (location.altitude > lastLocation!!.altitude && (!location.hasVerticalAccuracy() || elevation < location.verticalAccuracyMeters)) {
                            accumulatedElevation += elevation;
                        }
                    }

                    lastLocation = location;
                }
            }

            EventBus.getDefault().post(RecorderLocationEvent(result, !paused));
        }
    }

    private var elapsedSecondsRunnable = object : Runnable {
        override fun run() {
            if(!paused) {
                //elapsedSeconds++;

                EventBus.getDefault().post(RecorderElapsedSecondsEvent(getElapsedMilliseconds() / 1000, getFormattedElapsedTime()));

                handler.postDelayed(this, 1000);
            }
        }
    }

    internal fun getFormattedElapsedTime(): String {
        var secondsRemaining = (getElapsedMilliseconds() / 1000).toDouble();

        var hours = floor(secondsRemaining / (60 * 60));
        secondsRemaining -= hours * 60 * 60;

        var minutes = floor(secondsRemaining / 60);
        secondsRemaining -= minutes * 60;

        return String.format("%d:%02d:%02d", hours.toInt(), minutes.toInt(), secondsRemaining.toInt());
    }
}
