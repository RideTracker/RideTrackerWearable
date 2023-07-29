package com.norasoderlund.ridetrackerapp

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.resumeExercise
import androidx.lifecycle.lifecycleScope
import com.norasoderlund.ridetrackerapp.dao.SessionDao
import com.norasoderlund.ridetrackerapp.database.SessionDatabase
import com.norasoderlund.ridetrackerapp.entities.SessionLocation
import com.norasoderlund.ridetrackerapp.presentation.MainActivity
import com.norasoderlund.ridetrackerapp.utils.getFormattedDuration
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import java.util.UUID

class Recorder {
    private lateinit var activity: MainActivity;
    private lateinit var healthClient: HealthServicesClient;

    private lateinit var database: SessionDatabase;
    private lateinit var dao: SessionDao;

    private val sessionId: String = UUID.randomUUID().toString();

    internal var lastActiveDuration: ExerciseUpdate.ActiveDurationCheckpoint? = null;
    internal var lastState: ExerciseState? = null;

    internal var callbacks: MutableList<RecorderCallbacks> = mutableListOf();

    internal var lastLocationEvent: RecorderLocationEvent? = null;
    internal var lastSpeedEvent: RecorderSpeedEvent? = null;
    internal var lastStateInfoEvent: RecorderStateInfoEvent? = null;
    internal var lastDurationEvent: RecorderDurationEvent? = null;

    internal var started: Boolean = false;

    private var startedTimestamp: Long = 0L;
    private var accumulatedDuration: Long = 0;

    @SuppressLint("MissingPermission")
    constructor(activity: MainActivity, healthClient: HealthServicesClient, database: SessionDatabase) {
        this.activity = activity;
        this.healthClient = healthClient;

        //this.database = database;
        //this.dao = database.getDao();

        //dao.create(Session(sessionId));
    }

    internal fun startExerciseUpdates() {
        println("Recorder: startExerciseUpdates");

        setExerciseCallback();

        val exerciseConfig = ExerciseConfig(ExerciseType.BIKING, setOf(DataType.LOCATION, DataType.SPEED), false, true);
        healthClient.exerciseClient.startExerciseAsync(exerciseConfig);
    }

    internal fun setExerciseCallback() {
        println("Recorder: setExerciseCallback");

        healthClient.exerciseClient.setUpdateCallback(excerciseUpdateCallback);
    }

    private val excerciseUpdateCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            println("Recorder: onExerciseUpdate");

            val exerciseStateInfo = update.exerciseStateInfo;
            val activeDuration = update.activeDurationCheckpoint;
            val latestMetrics = update.latestMetrics;
            val latestGoals = update.latestAchievedGoals;

            if(lastStateInfoEvent?.stateInfo?.state != exerciseStateInfo.state) {
                if(started && startedTimestamp != 0L) {
                    val calendar = Calendar.getInstance();
                    val time = calendar.time.time;

                    when(exerciseStateInfo.state) {
                        ExerciseState.USER_PAUSED -> {
                            println("Recording: user pausing");

                            accumulatedDuration += time - startedTimestamp;
                            startedTimestamp = 0;
                        }

                        ExerciseState.USER_RESUMING -> {
                            println("Recording: user resuming");

                            startedTimestamp = time;
                        }

                        ExerciseState.AUTO_PAUSED -> {
                            println("Recording: auto pausing");

                            accumulatedDuration += time - startedTimestamp;
                            startedTimestamp = 0;
                        }

                        ExerciseState.AUTO_RESUMING -> {
                            println("Recording: auto resuming");

                            startedTimestamp = time;
                        }

                        ExerciseState.ENDED -> {
                            println("Recording: ending");

                            accumulatedDuration += time - startedTimestamp;
                            startedTimestamp = 0;
                        }
                    }
                }

                lastStateInfoEvent = RecorderStateInfoEvent(started, exerciseStateInfo);

                callbacks.forEach { it.onStateInfoEvent(lastStateInfoEvent!!); }
            }

            val locationUpdates = latestMetrics.getData(DataType.LOCATION);

            if(locationUpdates.isNotEmpty()) {
                if(started) {
                    activity.lifecycleScope.launch {
                        val sessionLocations = locationUpdates.map { location ->
                            println(String.format("New location: latitude %.4f longitude %.4f altitude %f", location.value.latitude, location.value.longitude, 0.0));

                            //SessionLocation(sessionId, location.value.latitude, location.value.longitude, 0.0, location.timeDurationFromBoot.toMillis())
                        };

                        //sessionLocations.forEach { location ->
                        //    dao.addLocation(location);
                        //}
                    }
                }

                val location = locationUpdates.last();

                println("Last longitude " + location.value.latitude + " longitude " + location.value.longitude);

                lastLocationEvent = RecorderLocationEvent(location.value.latitude, location.value.longitude);
                callbacks.forEach { it.onLocationUpdate(lastLocationEvent!!); }
            }

            val speedUpdates = latestMetrics.getData(DataType.SPEED);

            if(speedUpdates.isNotEmpty()) {
                val speed = speedUpdates.last();

                println("Current speed " + speed.value + " m/s");

                lastSpeedEvent = RecorderSpeedEvent(speed.value);
                callbacks.forEach { it.onSpeedEvent(lastSpeedEvent!!); }
            }
        }


        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {
            println("Recorder: onLapSummaryReceived");

            // For ExerciseTypes that support laps, this is called when a lap is marked.
        }

        override fun onRegistered() {
            println("Recorder: onRegistered");
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            println("Recorder: onRegistrationFailed");
        }

        override fun onAvailabilityChanged(
            dataType: DataType<*, *>,
            availability: Availability
        ) {
            println("Recorder: onAvailabilityChanged");
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

    private val durationRunnable = object : Runnable {
        override fun run() {
            if(started && lastStateInfoEvent?.stateInfo?.state == ExerciseState.ACTIVE) {
                val calendar = Calendar.getInstance();
                var duration = accumulatedDuration;

                if(startedTimestamp != 0L)
                    duration += calendar.time.time - startedTimestamp;

                lastDurationEvent = RecorderDurationEvent(duration, getFormattedDuration(duration));
                callbacks.forEach { it -> it.onDurationEvent(lastDurationEvent!!) }
            }

            Handler(Looper.getMainLooper()).postDelayed(this, 1000);
        }
    }

    /*internal fun unregisterHealthClient() {
        healthClient.measureClient.unregisterMeasureCallbackAsync(DataType.Companion.HEART_RATE_BPM, heartRateCallback)
    }*/

    internal var lastLocation: SessionLocation? = null;

    internal fun toggle() {
        if(!started)
            return resume();

        if(lastStateInfoEvent?.stateInfo?.state == ExerciseState.USER_PAUSED)
            return resume();

        pause();
    }

    @SuppressLint("MissingPermission")
    internal fun resume() {
        val calendar = Calendar.getInstance();
        startedTimestamp = calendar.time.time;

        if(!started) {
            started = true;

            lastStateInfoEvent = RecorderStateInfoEvent(started, lastStateInfoEvent?.stateInfo);
            callbacks.forEach { it.onStateInfoEvent(lastStateInfoEvent!!); }

            durationRunnable.run();

            return;
        }

        activity.lifecycleScope.launch {
            healthClient.exerciseClient.resumeExercise();
        }
    }

    internal fun pause() {
        if(!started)
            throw Error("Paused was called before the recorder was started.");

        activity.lifecycleScope.launch {
            healthClient.exerciseClient.pauseExercise();
        }
    }
}
