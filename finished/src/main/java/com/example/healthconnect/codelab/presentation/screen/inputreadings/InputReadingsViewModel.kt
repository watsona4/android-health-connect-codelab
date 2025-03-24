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
import android.widget.TextView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.WeightRecord
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.data.HealthConnectManager
import com.example.healthconnect.codelab.data.PostManager
import com.example.healthconnect.codelab.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

val PUBLISH_URL = "https://home.battenkillwoodworks.com/sync"

class InputReadingsViewModel(
  activity: MainActivity,
  private val healthConnectManager: HealthConnectManager,
  private val postManager : PostManager,
  private val output : TextView
) {

  val permissions = setOf(
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
  )

  val requestPermission =
    activity.registerForActivityResult(
      PermissionController.createRequestPermissionResultContract()
    ) {}

  var weightList: MutableState<List<WeightRecord>> = mutableStateOf(listOf())
    private set

  var bodyFatList: MutableState<List<BodyFatRecord>> = mutableStateOf(listOf())
    private set

  init {
    requestPermission.launch(permissions)
    runBlocking { run() }
  }

  suspend fun run() {
      readWeightInputs()
      readBodyFatInputs()
      publishWeightData()
      publishBodyFatData()
  }

  private fun getTime(): String {
    val formatter = DateTimeFormatter.ofPattern("M/d/uuuu h:mm a")
    val now = Instant.now()
    val zoned = now.atZone(ZoneId.of("America/New_York"))
    return zoned.format(formatter)
  }
  private suspend fun readWeightInputs() {
    output.append("${getTime()}: Reading weight values...\n")
    val then = Instant.ofEpochSecond(0)
    val now = Instant.now()
    weightList.value = healthConnectManager.readWeightInputs(then, now)
    output.append("${getTime()}:     read ${weightList.value.size} weight values\n")
  }

  private suspend fun readBodyFatInputs() {
    output.append("${getTime()}: Reading bodyfat values...\n")
    val then = Instant.ofEpochSecond(0)
    val now = Instant.now()
    bodyFatList.value = healthConnectManager.readBodyFatInputs(then, now)
    output.append("${getTime()}:     read ${bodyFatList.value.size} bodyfat values\n")
  }

  private fun publishWeightData() {
    output.append("${getTime()}: Publishing weight values...\n")
    val postData = JSONObject()
    weightList.value.forEach { postData.put(it.time.toString(), it.weight) }
    publishData(postData)
  }

  private fun publishBodyFatData() {
    output.append("${getTime()}: Publishing bodyfat values...\n")
    val postData = JSONObject()
    bodyFatList.value.forEach { postData.put(it.time.toString(), it.percentage) }
    publishData(postData)
  }

  private fun publishData(postData: JSONObject) {
    postManager.performPostRequest(PUBLISH_URL, postData,
      { success -> Log.i(TAG, success) },
      { error -> Log.i(TAG, error) }
    )
  }

  companion object {
    private val TAG: String = InputReadingsViewModel::class.java.simpleName
  }
}
