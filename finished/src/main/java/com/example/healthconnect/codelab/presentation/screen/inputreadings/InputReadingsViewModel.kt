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

import android.content.ContentValues
import android.os.RemoteException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Logger
import com.example.healthconnect.codelab.data.HealthConnectManager
import com.example.healthconnect.codelab.data.PostManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.UUID
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.health.connect.client.changes.Change

val PUBLISH_URL = "http://192.168.1.5:8568"

class InputReadingsViewModel(
  private val healthConnectManager: HealthConnectManager,
  private val postManager : PostManager
) :
  ViewModel() {
  val permissions = setOf(
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
  )
  var permissionsGranted = mutableStateOf(false)
    private set

  var weightList: MutableState<List<WeightRecord>> = mutableStateOf(listOf())
    private set

  var bodyFatList: MutableState<List<BodyFatRecord>> = mutableStateOf(listOf())
    private set

  var changesToken: MutableState<String?> = mutableStateOf(null)
    private set

  var changes = mutableStateListOf<Change>()
    private set

  var uiState: UiState by mutableStateOf(UiState.Uninitialized)
    private set

  val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

  fun initialLoad() {
    viewModelScope.launch {
      tryWithPermissionsCheck {
        readWeightInputs()
        readBodyFatInputs()
        publishWeightData()
        publishBodyFatData()
      }
    }
  }

  private suspend fun readWeightInputs() {
    val then = Instant.ofEpochSecond(0)
    val now = Instant.now()
    weightList.value = healthConnectManager.readWeightInputs(then, now)
  }

  private suspend fun readBodyFatInputs() {
    val then = Instant.ofEpochSecond(0)
    val now = Instant.now()
    bodyFatList.value = healthConnectManager.readBodyFatInputs(then, now)
  }

  private fun publishWeightData() {
    val postData = JSONObject()
    weightList.value.forEach { postData.put(it.time.toString(), it.weight) }
    publishData(postData)
  }

  private fun publishBodyFatData() {
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

  fun enableOrDisableChanges(enable: Boolean) {
    if (enable) {
      viewModelScope.launch {
        tryWithPermissionsCheck {
          changesToken.value = healthConnectManager.getChangesToken()
          Log.i(ContentValues.TAG, "Token: ${changesToken.value}")
        }
      }
    } else {
      changesToken.value = null
    }
  }

  fun getChanges() {
    viewModelScope.launch {
      tryWithPermissionsCheck {
        changesToken.value?.let { token ->
          changes.clear()
          healthConnectManager.getChanges(token).collect { message ->
            when (message) {
              is HealthConnectManager.ChangesMessage.ChangeList -> {
                changes.addAll(message.changes)
              }
              is HealthConnectManager.ChangesMessage.NoMoreChanges -> {
                changesToken.value = message.nextChangesToken
                Log.i(ContentValues.TAG, "Updating changes token: ${changesToken.value}")
              }
            }
          }
        }
      }
    }
  }

  private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
    permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)
    uiState = try {
      if (permissionsGranted.value) {
        block()
      }
      UiState.Done
    } catch (remoteException: RemoteException) {
      UiState.Error(remoteException)
    } catch (securityException: SecurityException) {
      UiState.Error(securityException)
    } catch (ioException: IOException) {
      UiState.Error(ioException)
    } catch (illegalStateException: IllegalStateException) {
      UiState.Error(illegalStateException)
    }
  }

  sealed class UiState {
    object Uninitialized : UiState()
    object Done : UiState()

    data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
  }

  companion object {
    private val TAG: String = InputReadingsViewModel::class.java.simpleName
  }
}

class InputReadingsViewModelFactory(
  private val healthConnectManager: HealthConnectManager,
  private val postManager: PostManager,
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(InputReadingsViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return InputReadingsViewModel(
        healthConnectManager = healthConnectManager,
        postManager = postManager,
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
