package com.norasoderlund.ridetrackerapp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class RecorderUploader(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val recording = inputData.getString("recording");

        println("Worker: " + recording);

        // Do the work here--in this case, upload the images.

        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }
}