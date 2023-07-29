package com.norasoderlund.ridetrackerapp

import com.norasoderlund.ridetrackerapp.utils.FormattedDistance

data class RecorderDistanceEvent(val meters: Double, val kilometers: Double, val formattedDistance: FormattedDistance);
