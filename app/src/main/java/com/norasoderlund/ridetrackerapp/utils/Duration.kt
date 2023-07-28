package com.norasoderlund.ridetrackerapp.utils

import androidx.health.services.client.data.ExerciseUpdate
import kotlin.math.floor

internal fun getFormattedDuration(duration: Long): String {
    var secondsRemaining = duration / 1000;

    var hours = floor(secondsRemaining / (60.0 * 60.0)).toLong();
    secondsRemaining -= hours * 60 * 60;

    var minutes = floor(secondsRemaining / 60.0).toLong();
    secondsRemaining -= minutes * 60;

    return String.format("%d:%02d:%02d", hours.toInt(), minutes.toInt(), secondsRemaining.toInt());
}
