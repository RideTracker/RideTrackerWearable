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
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.norasoderlund.ridetrackerapp.R
import com.norasoderlund.ridetrackerapp.RecorderLocationEvent
import com.norasoderlund.ridetrackerapp.RecorderStateEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.max
import kotlin.math.min


class MapPageFragment : Fragment() {
    lateinit var activity: MainActivity;
    var mapFragment: SupportMapFragment? = null;
    var googleMap: GoogleMap? = null;
    var googleMapLocationMarker: Marker? = null;
    var currentPolyline: Polyline? = null;

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
        activity = requireActivity() as MainActivity;

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

        view.findViewById<ImageButton>(R.id.recordingButton)?.setOnClickListener {
            activity.recorder.toggle();
        }

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

        googleMap.setMaxZoomPreference(16f);
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

        val coordinate = LatLng(0.0, 0.0);

        this.googleMapLocationMarker!!.position = coordinate;

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 15f));

        /*if(activity.lastLocation != null) {
            val coordinate = LatLng(activity.lastLocation!!.latitude, activity.lastLocation!!.longitude);

            this.googleMapLocationMarker!!.position = coordinate;
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 15f));
        }*/

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onRecorderStateEvent(event: RecorderStateEvent) {
        val recordingButton = view?.findViewById<ImageButton>(R.id.recordingButton)?: return;
        val pausedViewIndicator = view?.findViewById<View>(R.id.pausedViewIndicator);

        val context = requireContext();

        if(event.started && !event.paused) {
            recordingButton.setImageResource(R.drawable.baseline_stop_24);
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.button));
            recordingButton.setColorFilter(ContextCompat.getColor(context, R.color.color));

            pausedViewIndicator!!.visibility = View.INVISIBLE;

            if(activity.recorder.lastLocation != null)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(activity.recorder.lastLocation!!.latitude, activity.recorder.lastLocation!!.longitude), 15f));
        }
        else {
            recordingButton.setImageResource(R.drawable.baseline_play_arrow_24);
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.brand));
            recordingButton.setColorFilter(ContextCompat.getColor(context, R.color.color));

            pausedViewIndicator!!.visibility = View.VISIBLE;

            currentPolyline = null;

            setMapCameraToBounds();
        }
    }

    private fun setMapCameraToBounds() {
        /*var minLat: Double? = null;
        var maxLat: Double? = null;

        var minLng: Double? = null;
        var maxLng: Double? = null;

        activity.recorder.sessions.flatMap { session -> session.locations }!!.forEach {location ->
            if (minLat == null) {
                minLat = location.coords.latitude
                maxLat = location.coords.latitude

                minLng = location.coords.longitude
                maxLng = location.coords.longitude
            } else {
                minLat = min(location.coords.latitude, minLat!!)
                maxLat = max(location.coords.latitude, maxLat!!)

                minLng = min(location.coords.longitude, minLng!!)
                maxLng = max(location.coords.longitude, maxLng!!)
            }
        }

        val builder = LatLngBounds.builder();
        builder.include(LatLng(minLat!!, minLng!!));
        builder.include(LatLng(maxLat!!, maxLng!!));

        googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));*/
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onRecorderLocationEvent(event: RecorderLocationEvent) {
        if(googleMap != null) {
            val coordinate = LatLng(event.location.latitude, event.location.longitude);

            if(currentPolyline == null)
                currentPolyline = googleMap?.addPolyline(PolylineOptions().color(ContextCompat.getColor(requireContext(), R.color.brand)));

            var points = currentPolyline!!.points;
            points.add(coordinate);
            currentPolyline!!.points = points;

            googleMap!!.animateCamera(CameraUpdateFactory.newLatLng(coordinate));

            if (googleMapLocationMarker != null)
                googleMapLocationMarker!!.position = coordinate;
        }
    }
}
