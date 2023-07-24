package com.norasoderlund.ridetrackerapp.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.widget.SwipeDismissFrameLayout
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.norasoderlund.ridetrackerapp.R

class MapPageFragment : Fragment() {
    var mapFragment: SupportMapFragment? = null;
    var googleMap: GoogleMap? = null;
    var googleMapLocationMarker: Marker? = null;

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

        val mapFrameLayout = view.findViewById<SwipeDismissFrameLayout>(R.id.mapContainer);

        mapFrameLayout.addCallback(object : SwipeDismissFrameLayout.Callback() {
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

        // Obtain the MapFragment and set the async listener to be notified when the map is ready.
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?;

        if(mapFragment != null) {
            (mapFragment as SupportMapFragment).getMapAsync(this::onMapReady);
        }
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        val context = requireContext();
        val activity = requireActivity();

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

        googleMap.isTrafficEnabled = true;

        this.googleMapLocationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(getScaledMarkerIcon(R.drawable.location, 32, 32)))
        );

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

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

    fun getScaledMarkerIcon(resource: Int, width: Int, height: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(this.resources, resource);

        return Bitmap.createScaledBitmap(bitmap, width, height, false);
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
