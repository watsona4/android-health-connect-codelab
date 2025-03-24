/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthconnect.codelab.presentation

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateOf
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.presentation.screen.inputreadings.InputReadingsViewModel
import java.time.Duration

/**
 * The entry point into the sample.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout);

    val healthConnectManager = (application as BaseApplication).healthConnectManager
    val postManager = (application as BaseApplication).postManager
    val inputReadingsViewModel = InputReadingsViewModel(
      activity = this,
      healthConnectManager = healthConnectManager,
      postManager = postManager,
      output = findViewById(R.id.output)
    )

    class InputWorker(context: Context, workerParameters: WorkerParameters) :
      CoroutineWorker(context, workerParameters) {
      override suspend fun doWork(): Result {
        inputReadingsViewModel.run()
        return Result.success()
      }
    }

    val workConstraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.UNMETERED)
      .build()

    val workRequest = PeriodicWorkRequestBuilder<InputWorker>(Duration.ofMinutes(15))
      .setConstraints(workConstraints)
      .build()

    val workManager = WorkManager.getInstance(this)

    workManager.enqueue(workRequest)
  }
}
