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
package com.example.healthconnect.codelab.presentation.screen.inputreadings

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.healthconnect.codelab.data.HealthConnectManager
import com.example.healthconnect.codelab.data.PostManager
import com.example.healthconnect.codelab.presentation.formatter
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val PUBLISH_URL = "https://home.battenkillwoodworks.com/sync"

class InputReadingsViewModel(
  permissionLauncher: ActivityResultLauncher<Set<String>>,
  private val healthConnectManager: HealthConnectManager,
  private val postManager: PostManager,
  private var output: (String) -> Unit
): ViewModel() {

  val permissions = setOf(
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
  )

  var weightList: MutableState<List<WeightRecord>> = mutableStateOf(listOf())
    private set

  var bodyFatList: MutableState<List<BodyFatRecord>> = mutableStateOf(listOf())
    private set

  init {
    permissionLauncher.launch(permissions)
  }

  suspend fun run(date: ZonedDateTime) {
    readWeightInputs(date)
    readBodyFatInputs(date)
    publishWeightData()
    publishBodyFatData()
  }

  private fun getTime(): String {
    return Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter
      .ofPattern("M/d/uuuu h:mm a"))
  }

  private fun getDate(date: ZonedDateTime): String {
    return formatter.format(date)
  }

  private suspend fun readWeightInputs(date: ZonedDateTime) {
    val then = date.toInstant()
    output("${getTime()}: Reading weight values since ${getDate(date)}...")
    val now = Instant.now()
    weightList.value = healthConnectManager.readWeightInputs(then, now)
    output("${getTime()}:     read ${weightList.value.size/2} weight values")
  }

  private suspend fun readBodyFatInputs(date: ZonedDateTime) {
    val then = date.toInstant()
    output("${getTime()}: Reading bodyfat values since ${getDate(date)}...")
    val now = Instant.now()
    bodyFatList.value = healthConnectManager.readBodyFatInputs(then, now)
    output("${getTime()}:     read ${bodyFatList.value.size/2} bodyfat values")
  }

  private fun publishWeightData() {
    output("${getTime()}: Publishing weight values...")
    val postData = JSONObject()
    weightList.value.forEach { postData.put(it.time.toString(), it.weight) }
    publishData(postData)
  }

  private fun publishBodyFatData() {
    output("${getTime()}: Publishing bodyfat values...")
    val postData = JSONObject()
    bodyFatList.value.forEach { postData.put(it.time.toString(), it.percentage) }
    publishData(postData)
  }

  private fun publishData(postData: JSONObject) {
    postManager.performPostRequest(PUBLISH_URL, postData,
      { success ->
        output("${getTime()}: $success")
        Log.i(TAG, success)
      },
      { error ->
        output("${getTime()}: Error: $error")
        Log.e(TAG, error)
      }
    )
  }

  companion object {
    private val TAG: String = InputReadingsViewModel::class.java.simpleName
  }
}

class InputReadingsViewModelFactory(
  private val permissionLauncher: ActivityResultLauncher<Set<String>>,
  private val healthConnectManager: HealthConnectManager,
  private val postManager: PostManager,
  private val output: (String) -> Unit
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(InputReadingsViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return InputReadingsViewModel(
        permissionLauncher,
        healthConnectManager,
        postManager,
        output) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
