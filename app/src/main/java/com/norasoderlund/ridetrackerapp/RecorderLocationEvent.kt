package com.norasoderlund.ridetrackerapp

import com.google.android.gms.location.LocationResult
import com.norasoderlund.ridetrackerapp.entities.SessionLocation

data class RecorderLocationEvent(val location: SessionLocation);
