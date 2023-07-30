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
import android.view.View
import android.widget.EditText
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
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.NO_EXERCISE_IN_PROGRESS
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.OTHER_APP_IN_PROGRESS
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.OWNED_EXERCISE_IN_PROGRESS
import androidx.health.services.client.endExercise
import androidx.viewpager2.widget.ViewPager2
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.ambient.AmbientModeSupport.AmbientController
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientModeSupport.AmbientCallbackProvider
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.norasoderlund.ridetrackerapp.ApiClient
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
import com.norasoderlund.ridetrackerapp.RecorderUploader
import com.norasoderlund.ridetrackerapp.TokenStore
import com.norasoderlund.ridetrackerapp.presentation.theme.RideTrackerTheme
import com.norasoderlund.ridetrackerapp.utils.getDeviceName
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar


class MainActivity : AppCompatActivity(), AmbientCallbackProvider, RecorderCallbacks {
    private var previousPage: Int = 1;
    private var isLoading: Boolean = true;

    internal lateinit var recorder: Recorder;

    private lateinit var ambientController: AmbientController;
    private lateinit var healthClient: HealthServicesClient;
    internal lateinit var tokenStore: TokenStore;
    private lateinit var apiClient: ApiClient;

    internal var deviceName: String? = null;

    internal var initialLocation: LatLng = LatLng(0.0, 0.0);

    /**
     * Flow:
     * Empty loading >
     *                  if permissions: location view
     *                  else permisisons view
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar);

        super.onCreate(savedInstanceState);

        setLoadingView({});

        val activity = this;

        lifecycleScope.launch {
            deviceName = getDeviceName(activity)?: "Unknown";

            tokenStore = TokenStore(activity);

            ambientController = AmbientModeSupport.attach(activity);
            ambientController.setAmbientOffloadEnabled(false);
            println("Is ambient enabled: " + ambientController.isAmbient);

            healthClient = HealthServices.getClient(activity);
            recorder = Recorder(activity, healthClient);
            apiClient = ApiClient(activity);

            println("Checking if there's a key in the token store...");

            if(tokenStore.readKey() == null) {
                println("No token found, instructing to open login view.");

                setLoadingViewRunnable {
                    run {
                        setLoginView();
                    }
                }
            }
            else {
                println("Token found, instructing to check for permissions.");

                requestPermissionsPage();
            }

            isLoading = false;
        }.start();
    }

    fun requestPermissionsPage() {
        val permissions = mapOf<String, Int>(
            Manifest.permission.ACCESS_FINE_LOCATION to ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION),
            Manifest.permission.ACCESS_COARSE_LOCATION to ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION to ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            Manifest.permission.BODY_SENSORS to ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS),
            Manifest.permission.ACTIVITY_RECOGNITION to ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION),
            Manifest.permission.WAKE_LOCK to ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK)
        );

        setLoadingViewRunnable {
            run {
                if (permissions.any { permission -> permission.value == PackageManager.PERMISSION_DENIED }) {
                    setContentView(R.layout.permissions);

                    ActivityCompat.requestPermissions(this, permissions.filter { permission -> permission.value == PackageManager.PERMISSION_DENIED }.keys.toTypedArray(), 1);
                }
                else {
                    //setPagesView();
                    setLocationView();
                }
            }
        };
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

    private lateinit var onLoadingComplete: Runnable;

    private fun setLoadingView(runnable: Runnable, iconResource: Int? = null, text: String? = null) {
        onLoadingComplete = runnable;

        setContentView(R.layout.starting_page);

        if(iconResource != null)
            findViewById<ImageView>(R.id.iconImage)?.setImageResource(iconResource);

        if(text != null)
            findViewById<TextView>(R.id.textView)?.text = text;

        Handler(Looper.getMainLooper()).postDelayed(progressIndicatorRunnable, 100);
    }

    internal fun setLoginView() {
        setContentView(R.layout.login_page);

        findViewById<LinearLayout>(R.id.appButton)?.setOnClickListener {
            setLoginAppView();
        }

        findViewById<LinearLayout>(R.id.passwordButton)?.setOnClickListener {
            setLoginAppEmailView();
        }
    }

    private fun setLoginAppView() {
        setContentView(R.layout.login_app_page);

        findViewById<LinearLayout>(R.id.continueButton)?.setOnClickListener {
            setLoginAppCodeView();
        }

        findViewById<LinearLayout>(R.id.cancelButton)?.setOnClickListener {
            setLoginView();
        }
    }

    private fun setLoginAppCodeView() {
        setContentView(R.layout.login_app_code_page);

        val codeInput = findViewById<EditText>(R.id.codeInput);

        findViewById<LinearLayout>(R.id.continueButton)?.setOnClickListener {
            val code = codeInput.text.toString();

            setLoadingView({}, R.drawable.baseline_directions_bike_24, "Verifying login...");

            lifecycleScope.launch {
                apiClient.verifyLoginCode(deviceName!!, code) { response ->
                    if(!response.success) {
                        setLoadingViewRunnable {
                            run {
                                setErrorView(response.message!!) {
                                    setLoginAppCodeView();
                                }
                            }
                        }

                        isLoading = false;
                    }
                    else {
                        tokenStore.putKey(response.token!!);

                        requestPermissionsPage();

                        isLoading = false;
                    }
                }
            }.start();
        }

        findViewById<LinearLayout>(R.id.cancelButton)?.setOnClickListener {
            setLoginView();
        }
    }

    private fun setLoginAppEmailView() {
        setContentView(R.layout.login_app_email_page);

        val codeInput = findViewById<EditText>(R.id.codeInput);

        findViewById<LinearLayout>(R.id.continueButton)?.setOnClickListener {
            val email = codeInput.text.toString();

            setLoginAppPasswordView(email);
        }

        findViewById<LinearLayout>(R.id.cancelButton)?.setOnClickListener {
            setLoginView();
        }
    }

    private fun setLoginAppPasswordView(email: String) {
        setContentView(R.layout.login_app_password_page);

        val codeInput = findViewById<EditText>(R.id.codeInput);

        findViewById<LinearLayout>(R.id.continueButton)?.setOnClickListener {
            val password = codeInput.text.toString();

            setLoadingView({}, R.drawable.baseline_directions_bike_24, "Verifying login...");

            lifecycleScope.launch {
                apiClient.verifyLoginPassword(deviceName!!, email, password) { response ->
                    if(!response.success) {
                        setLoadingViewRunnable {
                            run {
                                setErrorView(response.message!!) {
                                    setLoginAppPasswordView(email);
                                }
                            }
                        }

                        isLoading = false;
                    }
                    else {
                        tokenStore.putKey(response.token!!);

                        requestPermissionsPage();

                        isLoading = false;
                    }
                }
            }.start();
        }

        findViewById<LinearLayout>(R.id.cancelButton)?.setOnClickListener {
            setLoginAppEmailView();
        }
    }

    private fun setErrorView(text: String, backListener: View.OnClickListener) {
        setContentView(R.layout.error_page);

        findViewById<TextView>(R.id.textView)?.text = text;

        findViewById<LinearLayout>(R.id.backButton)?.setOnClickListener(backListener);
    }

    private fun setLoadingViewRunnable(runnable: Runnable) {
        onLoadingComplete = runnable;
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

                    setLoadingView({
                        run {
                            setPagesView();
                        }
                    }, R.drawable.baseline_my_location_24, "Calibrating your GPS...");

                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener {
                                location: Location -> initialLocation = LatLng(location.latitude, location.longitude);
                        }
                        .addOnCompleteListener {
                            lifecycleScope.launch {
                                healthClient.exerciseClient.endExercise();

                                recorder = Recorder(activity, healthClient);

                                recorder.startDatabase();
                                //recorder.clearDatabase();

                                recorder.addCallback(activity);

                                recorder.startExerciseUpdates();

                                isLoading = false;
                            }.start();
                        };
                }

                // Start a fresh workout.
                NO_EXERCISE_IN_PROGRESS -> {
                    println("no exercise in progress");

                    setLoadingView({
                        run {
                            setPagesView();
                        }
                    }, R.drawable.baseline_my_location_24, "Calibrating your GPS...");

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

        setLoadingView({}, R.drawable.baseline_cloud_upload_24, "Uploading your activity...");

        val activity = this;

        lifecycleScope.launch {
            val recording = JSONObject();
            recording.put("localId", recorder.id);
            recording.put("visibility", "PUBLIC");

            val sessions = JSONArray();

            recorder.dao!!.getSessions().forEach { session ->
                val locationsArray = JSONArray();

                recorder.dao!!.getSessionLocations(session.id).forEach { location ->
                    val coordsObject = JSONObject();

                    coordsObject.put("latitude", location.latitude);
                    coordsObject.put("longitude", location.longitude);
                    coordsObject.put("altitude", location.altitude);

                    //TODO: add session properties
                    coordsObject.put("accuracy", 0);
                    coordsObject.put("altitudeAccuracy", 0);
                    coordsObject.put("speed", 0);
                    coordsObject.put("heading", 0);

                    val locationObject = JSONObject();
                    locationObject.put("coords", coordsObject);
                    locationObject.put("timestamp", location.timestamp);

                    locationsArray.put(locationObject);
                }

                val sessionObject = JSONObject();
                sessionObject.put("id", session.id);
                sessionObject.put("locations", locationsArray);
                sessionObject.put("timestamp", session.timestamp);

                sessions.put(sessionObject);
            }

            recording.put("sessions", sessions);

            println(recording.toString());

            val workRequestBuilder = OneTimeWorkRequestBuilder<RecorderUploader>();

            val data = Data.Builder();
            data.putString("recording", recording.toString());
            data.putString("token", tokenStore.readKey());
            workRequestBuilder.setInputData(data.build());

            WorkManager.getInstance(activity).enqueue(workRequestBuilder.build()).await();

            setLoadingViewRunnable {
                run {
                    setUploadedView();
                }
            };

            isLoading = false;
        }
    }

    fun setUploadedView() {
        setContentView(R.layout.uploaded_page);
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