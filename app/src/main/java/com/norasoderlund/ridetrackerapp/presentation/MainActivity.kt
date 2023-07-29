/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.norasoderlund.ridetrackerapp.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.NO_EXERCISE_IN_PROGRESS
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.OTHER_APP_IN_PROGRESS
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.OWNED_EXERCISE_IN_PROGRESS
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.WarmUpConfig
import androidx.viewpager2.widget.ViewPager2
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.ambient.AmbientModeSupport.AmbientController
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.health.services.client.data.LocationAvailability
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.wear.ambient.AmbientModeSupport.AmbientCallbackProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.norasoderlund.ridetrackerapp.R
import com.norasoderlund.ridetrackerapp.Recorder
import com.norasoderlund.ridetrackerapp.RecorderCallbacks
import com.norasoderlund.ridetrackerapp.RecorderDistanceEvent
import com.norasoderlund.ridetrackerapp.RecorderDurationEvent
import com.norasoderlund.ridetrackerapp.RecorderElevationEvent
import com.norasoderlund.ridetrackerapp.RecorderLocationEvent
import com.norasoderlund.ridetrackerapp.RecorderSessionEndEvent
import com.norasoderlund.ridetrackerapp.RecorderSpeedEvent
import com.norasoderlund.ridetrackerapp.RecorderStateInfoEvent
import com.norasoderlund.ridetrackerapp.database.SessionDatabase
import com.norasoderlund.ridetrackerapp.presentation.theme.RideTrackerTheme
import com.norasoderlund.ridetrackerapp.utils.getFormattedDuration
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Calendar


class MainActivity : AppCompatActivity(), AmbientCallbackProvider, RecorderCallbacks {
    private var previousPage: Int = 1;
    private var isLoading: Boolean = true;

    internal lateinit var recorder: Recorder;

    private lateinit var ambientController: AmbientController;
    private lateinit var healthClient: HealthServicesClient;

    internal var initialLocation: LatLng = LatLng(0.0, 0.0);

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar);

        super.onCreate(savedInstanceState);

        ambientController = AmbientModeSupport.attach(this);
        ambientController.setAmbientOffloadEnabled(false);
        println("Is ambient enabled: " + ambientController.isAmbient);

        healthClient = HealthServices.getClient(this);

        recorder = Recorder(this, healthClient);

        val permissions = mapOf<String, Int>(
            Manifest.permission.ACCESS_FINE_LOCATION to ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION),
            Manifest.permission.ACCESS_COARSE_LOCATION to ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION to ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            Manifest.permission.BODY_SENSORS to ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS),
            Manifest.permission.ACTIVITY_RECOGNITION to ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION),
            Manifest.permission.WAKE_LOCK to ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK)
        );


        if (permissions.any { permission -> permission.value == PackageManager.PERMISSION_DENIED }) {
            setContentView(R.layout.permissions);

            ActivityCompat.requestPermissions(this, permissions.filter { permission -> permission.value == PackageManager.PERMISSION_DENIED }.keys.toTypedArray(), 1);
        }
        else {
            //setPagesView();
            setLocationView();
        }
    }

    override fun onStart() {
        super.onStart();
    }

    override fun onStop() {
        super.onStop();
    };

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MainActivityAmbientCallback();

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        println("results: " + grantResults[0].toString() + "_" + grantResults[1].toString());

        if(grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
            setContentView(R.layout.permissions);
        }
        else {
            setLocationView();
        }
    }

    var progressIndicatorRunnable = object : Runnable {
        override fun run() {
            val progressIndicator = findViewById<CircularProgressIndicator>(R.id.progressIndicator);

            if(progressIndicator != null) {
                if(isLoading) {
                    if (progressIndicator.progress < 50) {
                        progressIndicator.progress = (progressIndicator.progress + 1) % 100;
                    } else {
                        progressIndicator.progress = (progressIndicator.progress - 1) % 100;
                    }

                    progressIndicator.rotation = (progressIndicator.rotation + 3.6f) % 360;

                    Handler(Looper.getMainLooper()).postDelayed(this, 10);
                }
                else {
                    if (progressIndicator.progress != 100) {
                        progressIndicator.progress = progressIndicator.progress + 1;
                        progressIndicator.rotation = (progressIndicator.rotation + 3.6f) % 360;

                        Handler(Looper.getMainLooper()).postDelayed(this, 10);
                    }
                    else
                        Handler(Looper.getMainLooper()).postDelayed(onLoadingComplete, 100);
                }
            }
            else if(!isLoading)
                Handler(Looper.getMainLooper()).postDelayed(onLoadingComplete, 10);
        }
    }

    var onLoadingComplete = object : Runnable {
        override fun run() {
            setPagesView();

            println("onLoadingComplete");
        }
    }

    @SuppressLint("MissingPermission")
    private fun setLocationView() {
        lifecycleScope.launch {
            val activity: MainActivity = this@MainActivity;

            val exerciseInfo = healthClient.exerciseClient.getCurrentExerciseInfoAsync().await();

            when (exerciseInfo.exerciseTrackedStatus) {
                // Warn user before continuing, will stop the existing workout.
                OTHER_APP_IN_PROGRESS -> {
                    println("other exercise in progress");

                    setContentView(R.layout.permissions);
                }

                // This app has an existing workout.
                OWNED_EXERCISE_IN_PROGRESS -> {
                    println("owned exercise in progress");

                    setContentView(R.layout.location);
                    Handler(Looper.getMainLooper()).postDelayed(progressIndicatorRunnable, 100);

                    recorder = Recorder(activity, healthClient);
                    recorder.addCallback(activity);

                    recorder.setExerciseCallback();

                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener {
                                location: Location -> initialLocation = LatLng(location.latitude, location.longitude);
                        }
                        .addOnCompleteListener {
                            recorder.startDatabase();

                            recorder.startExerciseUpdates();

                            // Experimental... should in theory "start" the recorder without starting a new session
                            recorder.resume();

                            isLoading = false;
                        };
                }

                // Start a fresh workout.
                NO_EXERCISE_IN_PROGRESS -> {
                    println("no exercise in progress");

                    setContentView(R.layout.location);
                    Handler(Looper.getMainLooper()).postDelayed(progressIndicatorRunnable, 100);


                    //val warmUpConfig = WarmUpConfig(ExerciseType.BIKING, setOf(DataType.LOCATION));
                    //healthClient.exerciseClient.setUpdateCallback(excerciseUpdateCallback);


                    //healthClient.exerciseClient.prepareExerciseAsync(warmUpConfig).await();

                    //healthClient.exerciseClient.endExerciseAsync().await();

                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener {
                            location: Location -> initialLocation = LatLng(location.latitude, location.longitude);
                        }
                        .addOnCompleteListener {
                            recorder = Recorder(activity, healthClient);

                            recorder.startDatabase();
                            recorder.clearDatabase();

                            recorder.addCallback(activity);

                            //recorder.setExerciseCallback();
                            recorder.startExerciseUpdates();

                            isLoading = false;
                        };
                }
            }
        }
    }

    var clockRunnable = object : Runnable {
        override fun run() {
            val clockTextView = findViewById<TextView>(R.id.clockTextView);

            val calendar = Calendar.getInstance();
            val currentSecond = calendar.get(Calendar.SECOND);
            val timeUntilNextMinute = ((60 - currentSecond) * 1000).toLong().coerceAtLeast(1);

            if(clockTextView != null) {
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                val currentMinute = calendar.get(Calendar.MINUTE);

                clockTextView.text = String.format("%02d:%02d", currentHour, currentMinute);
            }

            Handler(Looper.getMainLooper()).postDelayed(this, timeUntilNextMinute);
        }
    }


    fun setPagesView() {
        println("setPagesView");

        //DataBindingUtil.setContentView<MainActivityBinding>(this, R.layout.map);
        setContentView(R.layout.map);

        val viewPager2 = findViewById<ViewPager2>(R.id.pager);
        viewPager2.adapter = PageAdapter(this);
        viewPager2.setCurrentItem(1, false);
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setPageIndicator(previousPage, false);
                setPageIndicator(position, true);

                previousPage = position;
            }
        });

        clockRunnable.run();

        /*findViewById<ImageButton>(R.id.recordingButton)?.setOnClickListener {
            val pausedTextIndicator = findViewById<TextView>(R.id.pausedTextIndicator);
            val pausedViewIndicator = findViewById<View>(R.id.pausedViewIndicator);

            if(isRecording) {
                isPaused = !isPaused;

                pausedTextIndicator?.text = if(isPaused) resources.getString(R.string.paused) else getFormattedDuration();

                pausedTextIndicator?.setTextColor(ContextCompat.getColor(this, if (isPaused) R.color.red else R.color.blue));

                if(!isPaused)
                    Handler(Looper.getMainLooper()).postDelayed(durationRunnable, 1000);
            }
            else {
                isRecording = true;

                //pausedTextIndicator?.text = resources.getString(R.string.recording);
                pausedTextIndicator?.text = getFormattedDuration();
                pausedTextIndicator?.setTextColor(ContextCompat.getColor(this, R.color.blue));

                Handler(Looper.getMainLooper()).postDelayed(durationRunnable, 1000);
            }

            pausedViewIndicator?.visibility = if (isPaused || !isRecording) View.VISIBLE else View.INVISIBLE;

            setRecordingButton(!isPaused);
        }*/
    };

    fun setPageIndicator(position: Int, enabled: Boolean) {
        var view: ImageView? = null;

        if(position == 0)
            view = findViewById<ImageView>(R.id.menuPageIndicator);
        else if(position == 1)
            view = findViewById<ImageView>(R.id.mapPageIndicator);
        else if(position == 2)
            view = findViewById<ImageView>(R.id.statsPageIndicator);

        if(view == null)
            return;

        if(enabled) {
            view.background.setTint(ContextCompat.getColor(this, R.color.color));
        }
        else {
            view.background.setTint(Color.parseColor("#808080"));
        }
    }

    fun setFinishView() {
        setContentView(R.layout.finish_page);

        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView);

        if(descriptionTextView != null) {
            if(recorder.lastDistanceEvent != null && recorder.lastSpeedStatsEvent != null)
                descriptionTextView.text = "You reached " + recorder.lastDistanceEvent!!.formattedDistance.text + " " + recorder.lastDistanceEvent!!.formattedDistance.unit + " with an average speed of " + recorder.lastSpeedStatsEvent!!.average.toInt().toString() + " km/h!";
            else
                descriptionTextView.text = "We couldn't get the latest stats."
        }

        findViewById<LinearLayout>(R.id.finishButton)?.setOnClickListener {
            setUploadingView();
        }

        findViewById<LinearLayout>(R.id.discardButton)?.setOnClickListener {
            setDiscardView();
        }
    }

    fun setDiscardView() {
        setContentView(R.layout.discard_page);

        findViewById<LinearLayout>(R.id.deleteButton)?.setOnClickListener {
            setLocationView();
        }

        findViewById<LinearLayout>(R.id.cancelButton)?.setOnClickListener {
            setFinishView();
        }
    }

    fun setUploadingView() {
        setContentView(R.layout.uploading_page);

        isLoading = true;
        Handler(Looper.getMainLooper()).postDelayed(progressIndicatorRunnable, 100);
    }

    override fun onLocationUpdate(event: RecorderLocationEvent) {
    }

    override fun onDurationEvent(event: RecorderDurationEvent) {
        if(!recorder.started || recorder.lastStateInfoEvent?.stateInfo?.state != ExerciseState.ACTIVE)
            return;

        val pausedTextIndicator = findViewById<TextView>(R.id.pausedTextIndicator)?: return;

        pausedTextIndicator.text = event.formattedDuration;
        pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.green));
    }

    override fun onSpeedEvent(event: RecorderSpeedEvent) {
    }

    override fun onDistanceEvent(event: RecorderDistanceEvent) {

    }

    override fun onElevationEvent(event: RecorderElevationEvent) {
    }

    override fun onSessionEndEvent(event: RecorderSessionEndEvent) {
    }

    override fun onStateInfoEvent(event: RecorderStateInfoEvent) {
        println("onStateInfoEvent");
        val pausedTextIndicator = findViewById<TextView>(R.id.pausedTextIndicator)?: return;

        println("view exists");

        if(!event.started) {
            pausedTextIndicator.text = "Not recording";
            pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.color));

            return;
        }

        when(event.stateInfo?.state) {
            ExerciseState.AUTO_PAUSED -> {
                pausedTextIndicator.text = "Auto-paused";
                pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.blue));
            }

            ExerciseState.USER_PAUSED -> {
                pausedTextIndicator.text = "Paused";
                pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.red));
            }

            ExerciseState.ACTIVE -> {
                if(recorder.lastDurationEvent != null) {
                    pausedTextIndicator.text = recorder.lastDurationEvent!!.formattedDuration;
                    pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.green));
                }
            }

            null -> {
                pausedTextIndicator.text = "Starting";
                pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.blue));
            }
        }
    }
}

@Composable
fun WearApp(greetingName: String) {
    RideTrackerTheme {
        /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
         * version of LazyColumn for wear devices with some added features. For more information,
         * see d.android.com/wear/compose.
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center
        ) {
            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}