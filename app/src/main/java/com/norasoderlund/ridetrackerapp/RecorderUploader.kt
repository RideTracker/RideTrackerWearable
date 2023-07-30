package com.norasoderlund.ridetrackerapp

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class RecorderUploader(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val recording = inputData.getString("recording");
        val token = inputData.getString("token");

        val apiClient = ApiClient(this.applicationContext);

        apiClient.uploadRecording(token!!, recording!!) {};

        return Result.success();
    }
}