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
import androidx.viewpager2.widget.ViewPager2
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.norasoderlund.ridetrackerapp.R
import com.norasoderlund.ridetrackerapp.Recorder
import com.norasoderlund.ridetrackerapp.RecorderElapsedSecondsEvent
import com.norasoderlund.ridetrackerapp.RecorderStateEvent
import com.norasoderlund.ridetrackerapp.presentation.theme.RideTrackerTheme
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    var previousLocation: Int = 0;
    var isRecording: Boolean = false;
    var isLoading: Boolean = true;

    internal lateinit var recorder: Recorder;
    internal var lastLocation: Location? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar);

        super.onCreate(savedInstanceState);


        val fineLocationPermissions = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        val coarseLocationPermissions = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        val backgroundLocationPermissions = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

        if (fineLocationPermissions != PackageManager.PERMISSION_GRANTED || coarseLocationPermissions != PackageManager.PERMISSION_GRANTED || backgroundLocationPermissions != PackageManager.PERMISSION_GRANTED) {
            setContentView(R.layout.permissions);

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1);
        }
        else {
            //setPagesView();
            setLocationView();
        }
    }

    override fun onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    };

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
            setPagesView();
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
        }
    }

    @SuppressLint("MissingPermission")
    fun setLocationView() {
        recorder = Recorder(this);

        setContentView(R.layout.location);

        Handler(Looper.getMainLooper()).postDelayed(progressIndicatorRunnable, 100);

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener {
            location: Location ->
                lastLocation = location;

                isLoading = false;
        };
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
        //DataBindingUtil.setContentView<MainActivityBinding>(this, R.layout.map);
        setContentView(R.layout.map);

        val viewPager2 = findViewById<ViewPager2>(R.id.pager);
        viewPager2.adapter = PageAdapter(this);

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setPageIndicator(previousLocation, false);
                setPageIndicator(position, true);

                previousLocation = position;
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onRecorderElapsedSecondsEvent(event: RecorderElapsedSecondsEvent) {
        val pausedTextIndicator = findViewById<TextView>(R.id.pausedTextIndicator);

        pausedTextIndicator?.text = event.formattedElapsedSeconds;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onRecorderStateEvent(event: RecorderStateEvent) {
        val pausedTextIndicator = findViewById<TextView>(R.id.pausedTextIndicator)?: return;

        if(event.started) {
            if(event.paused) {
                pausedTextIndicator.text = "Paused";
                pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.red));
            }
            else {
                pausedTextIndicator.text = recorder.getFormattedElapsedTime();
                pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.blue));
            }
        }
        else {
            pausedTextIndicator.text = "Not recording";
            pausedTextIndicator.setTextColor(ContextCompat.getColor(this, R.color.color));
        }
    }


    fun setPageIndicator(position: Int, enabled: Boolean) {
        var view: ImageView? = null;

        if(position == 0)
            view = findViewById<ImageView>(R.id.mapPageIndicator);
        else if(position == 1)
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

    fun setRecordingButton(recording: Boolean) {
        /*var recordingButton: ImageButton? = findViewById<ImageButton>(R.id.recordingButton) ?: return;

        if(recording) {
            recordingButton!!.setImageResource(R.drawable.baseline_stop_24);
            recordingButton.background.setTint(ContextCompat.getColor(this, R.color.button));
            recordingButton.setColorFilter(ContextCompat.getColor(this, R.color.color));

            //findViewById<ImageButton>(R.id.trafficButton)?.visibility = View.INVISIBLE;
            //findViewById<ImageButton>(R.id.ambientButton)?.visibility = View.INVISIBLE;
        }
        else {
            recordingButton!!.setImageResource(R.drawable.baseline_play_arrow_24);
            recordingButton.background.setTint(ContextCompat.getColor(this, R.color.brand));
            recordingButton.setColorFilter(ContextCompat.getColor(this, R.color.color));

            //findViewById<ImageButton>(R.id.trafficButton)?.visibility = View.VISIBLE;
            //findViewById<ImageButton>(R.id.ambientButton)?.visibility = View.VISIBLE;
        }*/
    }
}

/*class MainActivityOld : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    var mapFragment: SupportMapFragment? = null;
    var googleMap: GoogleMap? = null;
    var googleMapLocationMarker: Marker? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.map);

        val fineLocationPermissions = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        val coarseLocationPermissions = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (fineLocationPermissions != PackageManager.PERMISSION_GRANTED && coarseLocationPermissions != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        else
            createMap();

        /*setContent {
            WearApp("Android")
        }*/
    }

    fun createMap() {
        //val controller = AmbientModeSupport.attach(this);

        // Retrieve the containers for the root of the layout and the map. Margins will need to be
        // set on them to account for the system window insets.
        val mapFrameLayout = findViewById<SwipeDismissFrameLayout>(R.id.mapContainer)
        mapFrameLayout.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                onBackPressedDispatcher.onBackPressed();
            }
        })

        val controller = AmbientModeSupport.attach(this);
        println("Is ambient enabled: " + controller.isAmbient);

        // Obtain the MapFragment and set the async listener to be notified when the map is ready.
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment;

        if(mapFragment != null) {
                (mapFragment as SupportMapFragment).getMapAsync(this::onMapReady);
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        println("results: " + grantResults[0].toString() + "_" + grantResults[1].toString());

        if(grantResults.any { it == PackageManager.PERMISSION_DENIED })
            setContentView(R.layout.permissions);
        else
            createMap();
    }

    fun getScaledMarkerIcon(resource: Int, width: Int, height: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(this.resources, resource);

        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap;

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.darkmap));

            if(!success)
                println("Failed to load map style.");
        }
        catch(error: Error) {
            println("Failed to load map style because of an error.");
        }

        googleMap.isTrafficEnabled = true;

        this.googleMapLocationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(getScaledMarkerIcon(R.drawable.location, 32, 32)))
        );

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        val lastLocation = fusedLocationClient.lastLocation;

        lastLocation.addOnSuccessListener { location : Location? ->
            if(location != null) {
                val coordinate = LatLng(location.latitude, location.longitude);

                this.googleMapLocationMarker!!.position = coordinate;
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 14f));
            }
        };
        
        fusedLocationClient.requestLocationUpdates(LocationRequest.Builder(10 * 1000).build(),
            locationCallback,
            Looper.getMainLooper());


        /*val sydney = LatLng(-33.85704, 151.21522);

        googleMap.addMarker(
            MarkerOptions().position(sydney)
                .title("Sydney Opera House")
        );

        // Move the camera to show the marker.
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 18f));*/
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            println("Location result received.");
            
            if(googleMap != null) {
                if(result.lastLocation != null) {
                    val coordinate = LatLng(result.lastLocation!!.latitude, result.lastLocation!!.longitude);

                    if(googleMapLocationMarker != null)
                        googleMapLocationMarker!!.position = coordinate;

                    googleMap!!.animateCamera(CameraUpdateFactory.newLatLng(coordinate));
                }

                for (location in result.locations) {
                    println(String.format("Location update received at latitude %f longitude %f", location.latitude, location.longitude));

                    //moveToLocation(location)
                }
            }
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle) {
                super.onEnterAmbient(ambientDetails)

                mapFragment?.onEnterAmbient(ambientDetails);
            }

            override fun onExitAmbient() {
                super.onExitAmbient()

                mapFragment?.onExitAmbient()
            }
        }
    }

    fun getGreetingText() {
        /*var greetingTextView = findViewById<TextView>(R.id.greetingTextView);

        val rightNow = Calendar.getInstance();
        val currentHour = rightNow.get(Calendar.HOUR_OF_DAY);

        if(currentHour < 12)
            greetingTextView.setText("Good morning");
        else if(currentHour < 17)
            greetingTextView.setText("Good afternoon");
        else
            greetingTextView.setText("Good evening");*/

        //helloTextView.setText(String.format("Good evening\n%s!", "Nora"));
    }
}
*/


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