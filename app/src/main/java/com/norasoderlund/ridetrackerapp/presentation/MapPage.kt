package com.norasoderlund.ridetrackerapp.presentation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.health.services.client.data.ExerciseState
import androidx.wear.widget.SwipeDismissFrameLayout
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
import com.norasoderlund.ridetrackerapp.RecorderCallbacks
import com.norasoderlund.ridetrackerapp.RecorderDistanceEvent
import com.norasoderlund.ridetrackerapp.RecorderDurationEvent
import com.norasoderlund.ridetrackerapp.RecorderElevationEvent
import com.norasoderlund.ridetrackerapp.RecorderLocationEvent
import com.norasoderlund.ridetrackerapp.RecorderSessionEndEvent
import com.norasoderlund.ridetrackerapp.RecorderSpeedEvent
import com.norasoderlund.ridetrackerapp.RecorderStateInfoEvent
import com.norasoderlund.ridetrackerapp.entities.Session
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.max
import kotlin.math.min

class MapPageFragment : Fragment(), RecorderCallbacks {
    private lateinit var activity: MainActivity;

    private var map: GoogleMap? = null;
    private var mapFragment: SupportMapFragment? = null;

    private var locationMarker: Marker? = null;

    private var trafficEnabled: Boolean = false;
    private var ambientEnabled: Boolean = false;

    private val sessionPolylines: MutableMap<String, Polyline> = mutableMapOf();

    private var cameraOverview: Boolean = false;

    override fun onStart() {
        super.onStart();

        activity.recorder.addCallback(this);
    }

    override fun onResume() {
        super.onResume();
    }

    override fun onPause() {
        super.onPause();
    }

    override fun onStop() {
        super.onStop();

        activity.recorder.removeCallback(this);
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

        this.locationMarker = createMarker(activity.initialLocation, R.drawable.location, 1000f);

        sessionPolylines.clear();

        if(activity.recorder.started) {
            var sessionLocations = activity.recorder.restoreSessionLocations();

            sessionLocations.forEach { (session, locations) ->
                createPolyline(session.id);

                if(locations.isNotEmpty()) {
                    val points = sessionPolylines[session.id]!!.points;
                    locations.forEach { location -> points.add(LatLng(location.latitude, location.longitude)) }
                    sessionPolylines[session.id]!!.points = points;

                    createMarker(sessionPolylines[session.id]!!.points.first(), if(session.index == 0) R.drawable.start else R.drawable.intermediate);

                    if(session.id != activity.recorder.currentSession?.id)
                        createMarker(sessionPolylines[session.id]!!.points.last(), R.drawable.intermediate);
                }
            }
        }
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
        var minLat: Double? = null;
        var maxLat: Double? = null;

        var minLng: Double? = null;
        var maxLng: Double? = null;

        sessionPolylines.flatMap { (session, polyline) -> polyline.points }!!.forEach { point ->
            if (minLat == null) {
                minLat = point.latitude
                maxLat = point.latitude

                minLng = point.longitude
                maxLng = point.longitude
            } else {
                minLat = min(point.latitude, minLat!!)
                maxLat = max(point.latitude, maxLat!!)

                minLng = min(point.longitude, minLng!!)
                maxLng = max(point.longitude, maxLng!!)
            }
        }

        val builder = LatLngBounds.builder();
        builder.include(LatLng(minLat!!, minLng!!));
        builder.include(LatLng(maxLat!!, maxLng!!));

        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

    private fun createPolyline(sessionId: String) {
        sessionPolylines[sessionId] = map!!.addPolyline(PolylineOptions().color(ContextCompat.getColor(requireContext(), R.color.brand)).width(10f).zIndex(10f));
    }

    private fun createMarker(location: LatLng, resource: Int, zIndex: Float = 100f): Marker? {
        return map!!.addMarker(
            MarkerOptions().position(location).anchor(0.5f, 0.5f).zIndex(zIndex).icon(
                BitmapDescriptorFactory.fromBitmap(
                    getScaledMarkerIcon(
                        resource,
                        32,
                        32
                    )
                )
            )
        );
    }

    private fun addPolylinePoint(sessionId: String, point: LatLng) {
        val points = sessionPolylines[sessionId]!!.points;
        points.add(point);
        sessionPolylines[sessionId]!!.points = points;
    }

    override fun onLocationUpdate(event: RecorderLocationEvent) {
        if(map != null) {
            val coordinate = LatLng(event.latitude, event.longitude);

            println("Received location on map page");

            if(!cameraOverview)
                map!!.moveCamera(CameraUpdateFactory.newLatLng(coordinate));
            else if(activity.recorder.lastStateInfoEvent?.stateInfo?.state == ExerciseState.ACTIVE)
                setMapCameraToBounds();

            locationMarker?.position = coordinate;

            if(activity.recorder.started && activity.recorder.lastStateInfoEvent?.stateInfo?.state == ExerciseState.ACTIVE) {
                println("Adding polyline updates");

                val sessionId = activity.recorder.currentSession!!.id;

                if (sessionPolylines.contains(sessionId)) {
                    addPolylinePoint(sessionId, coordinate);
                } else {
                    createMarker(coordinate, if (activity.recorder.currentSessionIndex == 0) R.drawable.start else R.drawable.intermediate);

                    createPolyline(sessionId);
                    addPolylinePoint(sessionId, coordinate);
                }
            }
        }
    }

    override fun onStateInfoEvent(event: RecorderStateInfoEvent) {
        if(event.previousStateInfo?.state == ExerciseState.ACTIVE && event.stateInfo?.state != ExerciseState.ACTIVE) {
            cameraOverview = true;

            setMapCameraToBounds();
        }
        else if(event.previousStateInfo?.state != ExerciseState.ACTIVE && event.stateInfo?.state == ExerciseState.ACTIVE) {
            cameraOverview = false;

            val lastLocationEvent = activity.recorder.lastLocationEvent;

            if(lastLocationEvent != null)
                map!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lastLocationEvent!!.latitude, lastLocationEvent!!.longitude), 15f));
        }

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

    override fun onDistanceEvent(event: RecorderDistanceEvent) {

    }

    override fun onElevationEvent(event: RecorderElevationEvent) {
    }

    override fun onSessionEndEvent(event: RecorderSessionEndEvent) {
        if(map == null)
            return;

        if(sessionPolylines.contains(event.sessionId)) {
            val sessionPolyline = sessionPolylines[event.sessionId];

            createMarker(sessionPolyline!!.points.last(), R.drawable.intermediate);
        }
    }

    override fun onDurationEvent(event: RecorderDurationEvent) {
    }
}
