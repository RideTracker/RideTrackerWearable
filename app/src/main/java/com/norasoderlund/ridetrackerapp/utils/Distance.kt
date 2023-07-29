package com.norasoderlund.ridetrackerapp.utils

import kotlin.math.round

data class FormattedDistance(val text: String, val unit: String);

internal fun getFormattedDistance(distance: Double): FormattedDistance {
    val kilometers = distance / 1000;

    var text = (round(kilometers * 10.0) / 10.0).toString();
    var unit = "km";

    if(kilometers < 1.0) {
        val meters = round(distance).toInt();

        text = meters.toString();
        unit = if(meters == 1) "meter" else "meters";
    }

    return FormattedDistance(text, unit);
}