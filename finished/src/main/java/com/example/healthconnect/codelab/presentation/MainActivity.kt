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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.presentation.screen.inputreadings.InputReadingsViewModel
import com.example.healthconnect.codelab.presentation.screen.inputreadings.InputReadingsViewModelFactory
import com.example.healthconnect.codelab.presentation.theme.HealthConnectTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

class MainActivity : ComponentActivity() {

  val requestPermission =
    registerForActivityResult(
      PermissionController.createRequestPermissionResultContract()
    ) {}

  private val prefs = Preferences(this)

  private fun saveData(value: Long) {
    lifecycleScope.launch {
      prefs.writeToDataStore(Preferences.START_DATE, value)
    }
  }

  private fun readData(): LiveData<Long> {
    val liveData = MutableLiveData<Long>()
    lifecycleScope.launch {
      liveData.value = prefs.readFromDataStore(Preferences.START_DATE)
    }
    return liveData
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val healthConnectManager = (application as BaseApplication).healthConnectManager
    val postManager = (application as BaseApplication).postManager

    val textState = mutableStateOf("")

    setContent {
      var showDatePicker by remember { mutableStateOf(false) }
      val datePickerState = rememberDatePickerState()
      val millisToLocalDate = datePickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC)
          .toLocalDate().atStartOfDay(ZoneId.systemDefault())
      }
      val dateToString = millisToLocalDate?.let {
        formatter.format(it)
      } ?: "Select Start Date"
      val builder = StringBuilder()
      val dateObserver = Observer<Long> {
        datePickerState.selectedDateMillis = it
      }
      readData().observe(this, dateObserver)
      val viewModel: InputReadingsViewModel = viewModel(
        factory = InputReadingsViewModelFactory(
          permissionLauncher = requestPermission,
          healthConnectManager = healthConnectManager,
          postManager = postManager,
          output = { str ->
            builder.apply {
              appendLine(str)
            }
            textState.value = builder.toString()
          }
        )
      )

      HealthConnectTheme {
        Scaffold(
          topBar = {
            CenterAlignedTopAppBar(
              title= {
                Text("Health Connect Sync")
              },
              navigationIcon = {
                Icon(painterResource(R.drawable.ic_health_connect_logo),
                  "Health Connect Logo")
              }
            )
          },
          bottomBar = {
            BottomAppBar(
              actions = {
                IconButton(onClick = {
                  lifecycleScope.launch {
                    if (millisToLocalDate != null) {
                      viewModel.run(millisToLocalDate)
                    }
                  }
                }) {
                  Icon(Icons.Filled.Sync, contentDescription = "Localized description")
                }
                IconButton(onClick = {
                  finish()
                }) {
                  Icon(Icons.Filled.Close, contentDescription = "Localized description")
                }
              }
            )
          }
        ) { innerPadding ->
          Column(
            modifier = Modifier
              .padding(innerPadding)
              .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row (horizontalArrangement = Arrangement.Center){
              Text(text = "Start Date: ")
              Text(
                modifier = Modifier
                  .clickable(onClick = { showDatePicker = true }),
                text = dateToString,
                textAlign = TextAlign.Center,
              )
              if (showDatePicker) {
                DatePickerDialog(
                  onDismissRequest = { showDatePicker = false },
                  confirmButton = {
                    Button(
                      onClick = {
                        datePickerState.selectedDateMillis?.let { saveData(it) }
                        showDatePicker = false
                      }
                    ) {
                      Text(text = "OK")
                    }
                  },
                  dismissButton = {
                    Button(
                      onClick = { showDatePicker = false }
                    ) {
                      Text(text = "Cancel")
                    }
                  }
                ) {
                  DatePicker(
                    state = datePickerState,
                    showModeToggle = true
                  )
                }
              }
            }
            Text(text = textState.value)
          }
        }
      }
    }
  }
}
