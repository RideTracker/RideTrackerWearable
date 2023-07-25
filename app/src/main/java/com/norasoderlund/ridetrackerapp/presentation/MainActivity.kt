/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.norasoderlund.ridetrackerapp.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.ImageView
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
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.norasoderlund.ridetrackerapp.R
import com.norasoderlund.ridetrackerapp.presentation.theme.RideTrackerTheme

class MainActivity : FragmentActivity() {
    var previousLocation: Int = 0;
    var isRecording: Boolean = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        println("created");

        val fineLocationPermissions = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        val coarseLocationPermissions = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (fineLocationPermissions != PackageManager.PERMISSION_GRANTED && coarseLocationPermissions != PackageManager.PERMISSION_GRANTED) {
            setContentView(R.layout.permissions);

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0);
        }
        else {
            setPagesView();
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        println("results: " + grantResults[0].toString() + "_" + grantResults[1].toString());

        if(grantResults[1] == PackageManager.PERMISSION_DENIED && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            setContentView(R.layout.permissions);
        }
        else {
            setPagesView();
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

        findViewById<ImageButton>(R.id.recordingButton)?.setOnClickListener { setRecordingButton(!isRecording); }
    };

    fun setPageIndicator(position: Int, enabled: Boolean) {
        var view: ImageView? = null;

        if(position == 0)
            view = findViewById<ImageView>(R.id.mapPageIndicator);
        else if(position == 1)
            view = findViewById<ImageView>(R.id.statsPageIndicator);

        if(view == null)
            return;

        if(enabled)
            view.background.setTint(Color.parseColor("#FFFFFF"));
        else
            view.background.setTint(Color.parseColor("#808080"));
    }

    fun setRecordingButton(recording: Boolean) {
        this.isRecording = recording;

        var view: ImageButton? = findViewById<ImageButton>(R.id.recordingButton) ?: return;

        if(recording) {
            view!!.setImageResource(R.drawable.baseline_stop_24);
            view.background.setTint(Color.parseColor("#171A23"));
        }
        else {
            view!!.setImageResource(R.drawable.baseline_play_arrow_24);
            view.background.setTint(Color.parseColor("#CDBFF7"));
        }
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