package com.norasoderlund.ridetrackerapp

import androidx.health.services.client.data.ExerciseStateInfo

data class RecorderStateInfoEvent(val started: Boolean, val stateInfo: ExerciseStateInfo?);
