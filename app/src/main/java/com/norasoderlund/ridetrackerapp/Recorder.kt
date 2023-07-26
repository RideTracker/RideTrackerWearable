package com.norasoderlund.ridetrackerapp

import android.annotation.SuppressLint
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.norasoderlund.ridetrackerapp.presentation.MainActivity
import org.greenrobot.eventbus.EventBus
import kotlin.math.floor

class Recorder {
    private lateinit var activity: MainActivity;

    @SuppressLint("MissingPermission")
    constructor(activity: MainActivity) {
        this.activity = activity;

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        fusedLocationClient.requestLocationUpdates(LocationRequest.Builder(10 * 1000).setWaitForAccurateLocation(true).build(), locationCallback, Looper.getMainLooper());
    }

    private var handler: Handler = Handler(Looper.getMainLooper());

    internal var started: Boolean = false;
    internal var paused: Boolean = false;
    internal var elapsedSeconds: Int = 0;

    private var lastLocation: Location? = null;

    internal fun toggle() {
        if(!started || paused)
            return start();

        stop();
    }

    internal fun start() {
        if(started && !paused)
            return;

        started = true;
        paused = false;

        EventBus.getDefault().post(RecorderStateEvent(started, paused));

        handler.postDelayed(elapsedSecondsRunnable, 1000);
    }

    internal fun stop() {
        if(!started || paused)
            return;

        paused = true;

        EventBus.getDefault().post(RecorderStateEvent(started, paused));
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if(result.lastLocation != null)
                lastLocation = result.lastLocation;

            EventBus.getDefault().post(RecorderLocationEvent(result));

            for (location in result.locations) {
                println(String.format("Location update received at latitude %f longitude %f", location.latitude, location.longitude));
            }
        }
    }

    private var elapsedSecondsRunnable = object : Runnable {
        override fun run() {
            if(!paused) {
                elapsedSeconds++;

                EventBus.getDefault().post(RecorderElapsedSecondsEvent(elapsedSeconds, getFormattedElapsedTime()));

                handler.postDelayed(this, 1000);
            }
        }
    }

    internal fun getFormattedElapsedTime(): String {
        var secondsRemaining = elapsedSeconds.toDouble();

        var hours = floor(secondsRemaining / (60 * 60));
        secondsRemaining -= hours * 60 * 60;

        var minutes = floor(secondsRemaining / 60);
        secondsRemaining -= minutes * 60;

        return String.format("%d:%02d:%02d", hours.toInt(), minutes.toInt(), secondsRemaining.toInt());
    }
}
