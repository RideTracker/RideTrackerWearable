package com.norasoderlund.ridetrackerapp

import androidx.health.services.client.data.ExerciseUpdate

data class RecorderDurationEvent(val duration: Long, val formattedDuration: String);
