package com.norasoderlund.ridetrackerapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAccuracy
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.startExercise
import androidx.room.RoomDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.norasoderlund.ridetrackerapp.dao.SessionDao
import com.norasoderlund.ridetrackerapp.database.SessionDatabase
import com.norasoderlund.ridetrackerapp.entities.Session
import com.norasoderlund.ridetrackerapp.entities.SessionLocation
import com.norasoderlund.ridetrackerapp.presentation.MainActivity
import com.norasoderlund.ridetrackerapp.presentation.RecorderSession
import com.norasoderlund.ridetrackerapp.presentation.RecorderSessionLocation
import kotlinx.coroutines.guava.await
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.floor

class Recorder {
    private lateinit var activity: MainActivity;
    private lateinit var healthClient: HealthServicesClient;

    private lateinit var database: SessionDatabase;
    private lateinit var dao: SessionDao;

    private val sessionId: String = UUID.randomUUID().toString();

    @SuppressLint("MissingPermission")
    constructor(activity: MainActivity, healthClient: HealthServicesClient, database: SessionDatabase) {
        this.activity = activity;
        this.healthClient = healthClient;

        this.database = database;
        this.dao = database.getDao();

        dao.create(Session(sessionId));

        //healthClient.measureClient.registerMeasureCallback(DataType.Companion.HEART_RATE_BPM, heartRateCallback)

        healthClient.exerciseClient.setUpdateCallback(excerciseUpdateCallback);

        val exerciseConfig = ExerciseConfig(ExerciseType.BIKING, setOf(DataType.LOCATION, DataType.SPEED), true, true);

        healthClient.exerciseClient.startExerciseAsync(exerciseConfig);
    }

    private val excerciseUpdateCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val exerciseStateInfo = update.exerciseStateInfo;
            val activeDuration = update.activeDurationCheckpoint;
            val latestMetrics = update.latestMetrics;
            val latestGoals = update.latestAchievedGoals;

            val locationUpdates = latestMetrics.getData(DataType.LOCATION);

            if(locationUpdates.isNotEmpty()) {
                val sessionLocations = locationUpdates.map { location -> SessionLocation(sessionId, location.value.latitude, location.value.longitude, location.value.altitude, location.timeDurationFromBoot.toMillis()) };

                if(started && !paused) {
                    sessionLocations.forEach { location ->
                        dao.addLocation(location);
                    }
                }

                lastLocation = sessionLocations.last();

                EventBus.getDefault().post(RecorderLocationEvent(lastLocation!!));
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {
            // For ExerciseTypes that support laps, this is called when a lap is marked.
        }

        override fun onRegistered() {

        }

        override fun onRegistrationFailed(throwable: Throwable) {

        }

        override fun onAvailabilityChanged(
            dataType: DataType<*, *>,
            availability: Availability
        ) {
            // Called when the availability of a particular DataType changes.
            when {
                // Relates to Location/GPS.
                availability is LocationAvailability -> {

                }

                // Relates to another DataType.
                availability is DataTypeAvailability -> {

                }
            }
        }
    }

    /*internal fun unregisterHealthClient() {
        healthClient.measureClient.unregisterMeasureCallbackAsync(DataType.Companion.HEART_RATE_BPM, heartRateCallback)
    }*/

    private var handler: Handler = Handler(Looper.getMainLooper());

    internal var started: Boolean = false;
    internal var paused: Boolean = false;
    internal var accumulatedElevation: Double = 0.0;
    internal var accumulatedDistance: Double = 0.0;
    internal var previousSessionMilliseconds: Long = 0;

    internal var lastLocation: SessionLocation? = null;

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

        EventBus.getDefault().post(RecorderStateEvent(started, paused));
    }

    internal fun stop() {
        if(!started || paused)
            return;

        paused = true;

        EventBus.getDefault().post(RecorderStateEvent(started, paused));
    }

    /*private var elapsedSecondsRunnable = object : Runnable {
        override fun run() {
            if(!paused) {
                //elapsedSeconds++;

                EventBus.getDefault().post(RecorderElapsedSecondsEvent(getElapsedMilliseconds() / 1000, getFormattedElapsedTime()));

                handler.postDelayed(this, 1000);
            }
        }
    }*/

    internal fun getFormattedElapsedTime(): String {
        var secondsRemaining = (0 / 1000).toDouble();

        var hours = floor(secondsRemaining / (60 * 60));
        secondsRemaining -= hours * 60 * 60;

        var minutes = floor(secondsRemaining / 60);
        secondsRemaining -= minutes * 60;

        return String.format("%d:%02d:%02d", hours.toInt(), minutes.toInt(), secondsRemaining.toInt());
    }
}
