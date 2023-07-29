package com.norasoderlund.ridetrackerapp.presentation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.health.services.client.data.ExerciseState
import androidx.wear.widget.SwipeDismissFrameLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.norasoderlund.ridetrackerapp.R
import com.norasoderlund.ridetrackerapp.RecorderCallbacks
import com.norasoderlund.ridetrackerapp.RecorderDurationEvent
import com.norasoderlund.ridetrackerapp.RecorderLocationEvent
import com.norasoderlund.ridetrackerapp.RecorderSpeedEvent
import com.norasoderlund.ridetrackerapp.RecorderStateInfoEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MapPageFragment : Fragment(), RecorderCallbacks {
    private lateinit var activity: MainActivity;

    private var map: GoogleMap? = null;
    private var mapFragment: SupportMapFragment? = null;

    private var locationMarker: Marker? = null;

    private var trafficEnabled: Boolean = false;
    private var ambientEnabled: Boolean = false;

    override fun onStart() {
        super.onStart();

        activity.recorder.callbacks.add(this);
    }

    override fun onResume() {
        super.onResume();
    }

    override fun onPause() {
        super.onPause();
    }

    override fun onStop() {
        super.onStop();

        activity.recorder.callbacks.remove(this);
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
        //mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?;

        //if(mapFragment != null) {
            //(mapFragment as SupportMapFragment).getMapAsync(this::onMapReady);
        //}
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(map: GoogleMap) {
        val context = requireContext();
        val activity = requireActivity() as MainActivity;

        this.map = map;

        try {
            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.darkmap));

            if(!success)
                println("Failed to load map style.");
        }
        catch(error: Error) {
            println("Failed to load map style because of an error.");
        }

        map.setMaxZoomPreference(16f);
        map.uiSettings.isMapToolbarEnabled = false;
        map.uiSettings.isScrollGesturesEnabled = false;
        map.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = false;

        map.isTrafficEnabled = trafficEnabled;

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(activity.initialLocation, 15f));

        this.locationMarker = map.addMarker(MarkerOptions().position(activity.initialLocation).anchor(0.5f, 0.5f).icon(BitmapDescriptorFactory.fromBitmap(getScaledMarkerIcon(R.drawable.location, 32, 32))));
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

        map?.isTrafficEnabled = enabled;
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


    override fun onLocationUpdate(event: RecorderLocationEvent) {
        if(map != null) {
            val coordinate = LatLng(event.latitude, event.longitude);

            println("Received location on map page");

            map!!.moveCamera(CameraUpdateFactory.newLatLng(coordinate));

            locationMarker?.position = coordinate;
        }
    }

    override fun onStateInfoEvent(event: RecorderStateInfoEvent) {
        val recordingButton = view?.findViewById<ImageButton>(R.id.recordingButton)?: return;
        val pausedViewIndicator = view?.findViewById<View>(R.id.pausedViewIndicator);

        val context = requireContext();

        if(event.started && event.stateInfo?.state != ExerciseState.USER_PAUSED) {
            recordingButton.setImageResource(R.drawable.baseline_stop_24);
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.button));
            recordingButton.setColorFilter(ContextCompat.getColor(context, R.color.color));

            pausedViewIndicator!!.visibility = View.INVISIBLE;
        }
        else {
            recordingButton.setImageResource(R.drawable.baseline_play_arrow_24);
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.brand));
            recordingButton.setColorFilter(ContextCompat.getColor(context, R.color.color));

            pausedViewIndicator!!.visibility = View.VISIBLE;
        }
    }

    override fun onSpeedEvent(event: RecorderSpeedEvent) {
    }

    override fun onDurationEvent(event: RecorderDurationEvent) {
    }
}
