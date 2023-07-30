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
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.getCurrentExerciseInfo
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.resumeExercise
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.norasoderlund.ridetrackerapp.dao.SessionDao
import com.norasoderlund.ridetrackerapp.database.SessionDatabase
import com.norasoderlund.ridetrackerapp.entities.Session
import com.norasoderlund.ridetrackerapp.entities.SessionLocation
import com.norasoderlund.ridetrackerapp.presentation.MainActivity
import com.norasoderlund.ridetrackerapp.utils.getFormattedDistance
import com.norasoderlund.ridetrackerapp.utils.getFormattedDuration
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import java.util.UUID
import kotlin.math.round

class Recorder {
    internal val id = UUID.randomUUID().toString();

    private lateinit var activity: MainActivity;
    private lateinit var healthClient: HealthServicesClient;

    private var database: SessionDatabase? = null;
    internal var dao: SessionDao? = null;

    internal var currentSession: Session? = null;
    internal var currentSessionIndex: Int = 0;

    private var callbacks: MutableList<RecorderCallbacks> = mutableListOf();

    internal var lastLocationEvent: RecorderLocationEvent? = null;
    internal var lastSpeedEvent: RecorderSpeedEvent? = null;
    internal var lastStateInfoEvent: RecorderStateInfoEvent? = null;
    internal var lastDurationEvent: RecorderDurationEvent? = null;
    internal var lastDistanceEvent: RecorderDistanceEvent? = null;
    internal var lastElevationEvent: RecorderElevationEvent? = null;
    internal var lastSpeedStatsEvent: RecorderSpeedStatsEvent? = null;

    internal fun hasCallback(callback: RecorderCallbacks): Boolean {
        return callbacks.contains(callback);
    }

    internal fun addCallback(callback: RecorderCallbacks) {
        if(callbacks.contains(callback))
            throw Error("Cannot add callback that is already added.");

        /*if(lastLocationEvent != null) callback.onLocationUpdate(lastLocationEvent!!);
        if(lastSpeedEvent != null) callback.onSpeedEvent(lastSpeedEvent!!);
        if(lastStateInfoEvent != null) callback.onStateInfoEvent(lastStateInfoEvent!!);
        if(lastDurationEvent != null) callback.onDurationEvent(lastDurationEvent!!);
        if(lastDistanceEvent != null) callback.onDistanceEvent(lastDistanceEvent!!);
        if(lastElevationEvent != null) callback.onElevationEvent(lastElevationEvent!!);*/

        callbacks.add(callback);
    }

    internal fun removeCallback(callback: RecorderCallbacks) {
        if(!callbacks.contains(callback))
            throw Error("Cannot remove callback that hasn't been added.");

        callbacks.remove(callback);
    }

    internal var started: Boolean = false;

    private var startedTimestamp: Long = 0L;
    private var accumulatedDuration: Long = 0;

    @SuppressLint("MissingPermission")
    constructor(activity: MainActivity, healthClient: HealthServicesClient) {
        this.activity = activity;
        this.healthClient = healthClient;
    }

    internal fun startDatabase() {
        database = Room.databaseBuilder(activity.applicationContext, SessionDatabase::class.java, "sessions").allowMainThreadQueries().fallbackToDestructiveMigration().build();

        dao = database!!.getDao();

        dao!!.clearEmptySessions();
    }

    internal fun clearDatabase() {
        if(dao == null)
            throw Error("Cannot clear database before it's been started.");

        dao!!.clear();
    }

    internal fun startExerciseUpdates(resume: Boolean = false) {
        println("Recorder: startExerciseUpdates");

        if(dao == null)
            throw Error("Cannot start exercise updates before database has been started.");

        val lastSession = dao!!.getLastSession();

        if(resume) {
            currentSessionIndex = if(lastSession.isNotEmpty()) lastSession.first().index + 1 else 0;
            currentSession = Session(UUID.randomUUID().toString(), currentSessionIndex, Calendar.getInstance().timeInMillis);

            println("Recorder: starting new session with index $currentSessionIndex, without starting exercise");

            dao!!.createSession(currentSession!!);
        }
        else if(lastSession.isNotEmpty()) {
            currentSessionIndex = lastSession.first().index + 1;
            currentSession = Session(UUID.randomUUID().toString(), currentSessionIndex, Calendar.getInstance().timeInMillis);

            dao!!.createSession(currentSession!!);

            println("Recorder: starting new session with index $currentSessionIndex");

            setExerciseCallback();

            val exerciseConfig = ExerciseConfig(ExerciseType.BIKING, setOf(DataType.LOCATION, DataType.DISTANCE_TOTAL, DataType.ELEVATION_GAIN_TOTAL, DataType.SPEED, DataType.SPEED_STATS), false, true);
            healthClient.exerciseClient.startExerciseAsync(exerciseConfig);
        }
        else {
            println("Recorder: starting new session because database was just cleared or there was none.");

            currentSessionIndex = 0;

            currentSession = Session(UUID.randomUUID().toString(), currentSessionIndex, Calendar.getInstance().timeInMillis);

            dao!!.createSession(currentSession!!);

            setExerciseCallback();

            val exerciseConfig = ExerciseConfig(ExerciseType.BIKING, setOf(DataType.LOCATION, DataType.DISTANCE_TOTAL, DataType.ELEVATION_GAIN_TOTAL, DataType.SPEED, DataType.SPEED_STATS), false, true);
            healthClient.exerciseClient.startExerciseAsync(exerciseConfig);
        }
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

                            val sessionEndEvent = RecorderSessionEndEvent(currentSession!!.id);
                            callbacks.forEach { it.onSessionEndEvent(sessionEndEvent) }

                            currentSession = null;
                            currentSessionIndex++;

                            accumulatedDuration += time - startedTimestamp;
                            startedTimestamp = 0;
                        }

                        ExerciseState.USER_RESUMING -> {
                            println("Recording: user resuming");

                            println("Recorder: starting new session because of user pause resumed.");

                            currentSession = Session(UUID.randomUUID().toString(), currentSessionIndex, Calendar.getInstance().timeInMillis);
                            dao!!.createSession(currentSession!!);

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

                lastStateInfoEvent = RecorderStateInfoEvent(started, exerciseStateInfo, lastStateInfoEvent?.stateInfo);

                callbacks.forEach { it.onStateInfoEvent(lastStateInfoEvent!!); }
            }

            val locationUpdates = latestMetrics.getData(DataType.LOCATION);

            if(locationUpdates.isNotEmpty()) {
                if(started && currentSession != null) {
                    activity.lifecycleScope.launch {
                        locationUpdates.forEach { location ->
                            println(String.format("New location: latitude %.4f longitude %.4f altitude %f", location.value.latitude, location.value.longitude, 0.0));

                            val sessionLocation = SessionLocation(currentSession!!.id, location.value.latitude, location.value.longitude, 0.0, location.timeDurationFromBoot.toMillis())

                            dao?.addLocation(sessionLocation);
                        };
                    }
                }

                val location = locationUpdates.last();

                println("Last longitude " + location.value.latitude + " longitude " + location.value.longitude + " bearing " + location.value.bearing);

                lastLocationEvent = RecorderLocationEvent(location.value.latitude, location.value.longitude, location.value.bearing);
                callbacks.forEach { it.onLocationUpdate(lastLocationEvent!!); }
            }

            val distanceUpdate = latestMetrics.getData(DataType.DISTANCE_TOTAL);

            if(distanceUpdate != null && lastDistanceEvent?.meters != distanceUpdate?.total) {
                lastDistanceEvent = RecorderDistanceEvent(distanceUpdate!!.total, distanceUpdate!!.total / 1000, getFormattedDistance(distanceUpdate!!.total));
                callbacks.forEach { it.onDistanceEvent(lastDistanceEvent!!); }
            }

            val elevationGainUpdate = latestMetrics.getData(DataType.ELEVATION_GAIN_TOTAL);

            if(elevationGainUpdate != null && lastElevationEvent?.meters != elevationGainUpdate?.total) {
                lastElevationEvent = RecorderElevationEvent(elevationGainUpdate!!.total, elevationGainUpdate!!.total / 1000, getFormattedDistance(elevationGainUpdate!!.total));
                callbacks.forEach { it.onElevationEvent(lastElevationEvent!!); }
            }

            val speedUpdates = latestMetrics.getData(DataType.SPEED);

            if(speedUpdates.isNotEmpty()) {
                val speed = speedUpdates.last();

                println("Current speed " + speed.value + " m/s");

                lastSpeedEvent = RecorderSpeedEvent(speed.value);
                callbacks.forEach { it.onSpeedEvent(lastSpeedEvent!!); }
            }

            val speedStateUpdate = latestMetrics.getData(DataType.SPEED_STATS);

            if(speedStateUpdate != null && speedStateUpdate!!.average != lastSpeedStatsEvent?.average) {
                lastSpeedStatsEvent = RecorderSpeedStatsEvent(speedStateUpdate.average);
                //callbacks.forEach { it.onSpeedStatsEvent(lastSpeedStatsEvent!!); }
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

    internal fun restoreSessionLocations(): Map<Session, List<SessionLocation>> {
        if(dao == null)
            throw Error("Cannot restore session locations before database is started.");

        val sessions = dao!!.getSessions();

        return sessions.associateWith { session -> dao!!.getSessionLocations(session.id) };
    }

    internal fun toggle() {
        if(!started)
            return resume();

        if(lastStateInfoEvent?.stateInfo?.state == ExerciseState.USER_PAUSED)
            return resume();

        pause();
    }

    @SuppressLint("MissingPermission")
    internal fun resume() {
        println("Recorder: resume triggered");

        val calendar = Calendar.getInstance();
        startedTimestamp = calendar.time.time;

        if(!started) {
            println("Recorder: start triggered");

            started = true;

            lastStateInfoEvent = RecorderStateInfoEvent(started, lastStateInfoEvent?.stateInfo, lastStateInfoEvent?.stateInfo);
            callbacks.forEach { it.onStateInfoEvent(lastStateInfoEvent!!); }

            durationRunnable.run();

            return;
        }

        activity.lifecycleScope.launch {
            println("Recorder: resume exercise");

            healthClient.exerciseClient.resumeExercise();
        }
    }

    internal fun pause() {
        if(!started)
            throw Error("Paused was called before the recorder was started.");

        println("Recorder: pause triggered");

        activity.lifecycleScope.launch {
            println("Recorder: pause exercise");

            healthClient.exerciseClient.pauseExercise();
        }
    }

    internal fun finish() {
        if(!started)
            throw Error("Finish was called before the recorder was started.");

        if(lastStateInfoEvent?.stateInfo?.state != ExerciseState.USER_PAUSED) {
            println("Recorder: cannot finish activity because it's not user paused yet.");

            return;
        }

        activity.setFinishView();

        activity.lifecycleScope.launch {
            println("Recorder: ending the exercise");

            healthClient.exerciseClient.endExercise();
        }
    }
}
