package com.norasoderlund.ridetrackerapp.presentation

import android.os.Bundle
import androidx.wear.ambient.AmbientModeSupport.AmbientCallback

internal class MainActivityAmbientCallback : AmbientCallback() {

    override fun onEnterAmbient(ambientDetails: Bundle?) {
        // Handle entering ambient mode
        super.onEnterAmbient(ambientDetails);

        println("MainActivityAmbientCallback: onEnterAmbient");
    }

    override fun onExitAmbient() {
        // Handle exiting ambient mode
        super.onExitAmbient();

        println("MainActivityAmbientCallback: onExitAmbient");
    }

    override fun onUpdateAmbient() {
        // Update the content
        super.onUpdateAmbient();

        println("MainActivityAmbientCallback: onUpdateAmbient");
    }
}