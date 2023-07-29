package com.norasoderlund.ridetrackerapp

import com.norasoderlund.ridetrackerapp.utils.FormattedDistance

data class RecorderElevationEvent(val meters: Double, val kilometers: Double, val formattedDistance: FormattedDistance);
