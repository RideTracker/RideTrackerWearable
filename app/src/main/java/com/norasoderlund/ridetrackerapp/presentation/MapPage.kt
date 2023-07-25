package com.norasoderlund.ridetrackerapp.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.widget.SwipeDismissFrameLayout
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.norasoderlund.ridetrackerapp.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MapPageFragment : Fragment() {
    var mapFragment: SupportMapFragment? = null;
    var googleMap: GoogleMap? = null;
    var googleMapLocationMarker: Marker? = null;

    var trafficEnabled: Boolean = false;
    var ambientEnabled: Boolean = false;

    override fun onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    override fun onResume() {
        super.onResume();
    }

    override fun onPause() {
        super.onPause();
    }

    override fun onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onMainActivityLocationEventReceived(result: LocationResult) {
        println("Location result received.");

        if(googleMap != null) {
            if (result.lastLocation != null) {
                val coordinate =
                    LatLng(result.lastLocation!!.latitude, result.lastLocation!!.longitude);

                if (googleMapLocationMarker != null)
                    googleMapLocationMarker!!.position = coordinate;

                googleMap!!.animateCamera(CameraUpdateFactory.newLatLng(coordinate));
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.map_page, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState);

        val context = requireContext();
        val activity = requireActivity();

        val swipeDismissal = view.findViewById<SwipeDismissFrameLayout>(R.id.swipeDismissal);

        swipeDismissal.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onSwipeStarted(layout: SwipeDismissFrameLayout?) {
                super.onSwipeStarted(layout);

                println("Swiped start");
            }

            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                super.onSwipeStarted(layout);

                println("Swiped to back.");

                activity.onBackPressedDispatcher.onBackPressed();
            }
        })

        val controller = AmbientModeSupport.attach(activity);

        println("Is ambient enabled: " + controller.isAmbient);

        setTrafficButton(false);
        view.findViewById<ImageButton>(R.id.trafficButton)?.setOnClickListener { setTrafficButton(!trafficEnabled); }

        setAmbientButton(false);
        view.findViewById<ImageButton>(R.id.ambientButton)?.setOnClickListener { setAmbientButton(!ambientEnabled); }

        // Obtain the MapFragment and set the async listener to be notified when the map is ready.
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?;

        if(mapFragment != null) {
            (mapFragment as SupportMapFragment).getMapAsync(this::onMapReady);
        }
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        val context = requireContext();
        val activity = requireActivity() as MainActivity;

        this.googleMap = googleMap;

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.darkmap));

            if(!success)
                println("Failed to load map style.");
        }
        catch(error: Error) {
            println("Failed to load map style because of an error.");
        }

        googleMap.uiSettings.isMapToolbarEnabled = false;
        googleMap.uiSettings.isScrollGesturesEnabled = false;
        googleMap.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = false;

        googleMap.isTrafficEnabled = trafficEnabled;

        this.googleMapLocationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(getScaledMarkerIcon(R.drawable.location, 32, 32)))
        );

        if(activity.lastLocation != null) {
            val coordinate = LatLng(activity.lastLocation!!.latitude, activity.lastLocation!!.longitude);

            this.googleMapLocationMarker!!.position = coordinate;
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 15f));
        }

        /*val lastLocation = fusedLocationClient.lastLocation;

        lastLocation.addOnSuccessListener { location : Location? ->
            if(location != null) {
                val coordinate = LatLng(location.latitude, location.longitude);

                this.googleMapLocationMarker!!.position = coordinate;
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 14f));
            }
        };*/



        /*val sydney = LatLng(-33.85704, 151.21522);

        googleMap.addMarker(
            MarkerOptions().position(sydney)
                .title("Sydney Opera House")
        );

        // Move the camera to show the marker.
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 18f));*/
    }

    fun getScaledMarkerIcon(resource: Int, width: Int, height: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(this.resources, resource);

        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    fun setTrafficButton(enabled: Boolean) {
        this.trafficEnabled = enabled;

        var trafficButton: ImageButton? = view?.findViewById<ImageButton>(R.id.trafficButton) ?: return;

        var context = requireContext();

        if(enabled) {
            //trafficButton!!.background.setTint(ContextCompat.getColor(context, R.color.button));
            trafficButton!!.setColorFilter(ContextCompat.getColor(context, R.color.green));
        }
        else {
            //trafficButton!!.background.setTint(ContextCompat.getColor(context, R.color.green));
            trafficButton!!.setColorFilter(ContextCompat.getColor(context, R.color.color));
        }

        if(googleMap != null)
            googleMap!!.isTrafficEnabled = enabled;
    }

    fun setAmbientButton(enabled: Boolean) {
        this.ambientEnabled = enabled;

        var ambientButton: ImageButton? = view?.findViewById<ImageButton>(R.id.ambientButton) ?: return;

        var context = requireContext();

        if(enabled) {
            //ambientButton!!.background.setTint(ContextCompat.getColor(context, R.color.button));
            ambientButton!!.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
        }
        else {
            //ambientButton!!.background.setTint(ContextCompat.getColor(context, R.color.yellow));
            ambientButton!!.setColorFilter(ContextCompat.getColor(context, R.color.color));
        }
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
}
