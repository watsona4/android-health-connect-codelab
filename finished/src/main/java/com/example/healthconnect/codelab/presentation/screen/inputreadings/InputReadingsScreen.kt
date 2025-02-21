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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.WeightRecord
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.data.dateTimeWithOffsetOrDefault
import com.example.healthconnect.codelab.presentation.component.FormattedChange
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@Composable
fun InputReadingsScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    weightList: List<WeightRecord>,
    bodyFatList: List<BodyFatRecord>,
    onGetChanges: () -> Unit,
    changes: List<Change>,
    changesToken: String?,
    uiState: InputReadingsViewModel.UiState,
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
) {

  val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

  LaunchedEffect(uiState) {
    if (uiState is InputReadingsViewModel.UiState.Uninitialized) {
      onPermissionsResult()
    }
    if (uiState is InputReadingsViewModel.UiState.Error && errorId.value != uiState.uuid) {
      onError(uiState.exception)
      errorId.value = uiState.uuid
    }
  }

  if (uiState != InputReadingsViewModel.UiState.Uninitialized) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Top,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (!permissionsGranted) {
        item {
          Button(
            onClick = { onPermissionsLaunch(permissions) }
          ) {
            Text(text = stringResource(R.string.permissions_button_label))
          }
        }
      } else {
        item {
          val token = changesToken ?: stringResource(id = R.string.not_available_abbrev)
          Text(stringResource(id = R.string.differential_changes_current_token, token))
        }
        item {
          Button(
            modifier = Modifier.padding(8.dp),
            enabled = changesToken != null,
            onClick = onGetChanges
          ) {
            Text(stringResource(R.string.differential_changes_button_text))
          }
        }
        items(changes) { changeItem ->
          FormattedChange(changeItem)
        }
        if (changes.isEmpty()) {
          item {
            Text(stringResource(R.string.differential_changes_empty))
          }
        }
        item {
          Text(
            text = stringResource(id = R.string.previous_readings),
            fontSize = 24.sp,
            color = MaterialTheme.colors.primary
          )
        }
        items(weightList) { reading ->
          Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // show local date and time
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            val zonedDateTime =
              dateTimeWithOffsetOrDefault(reading.time, reading.zoneOffset)
            Text(
              text = "${reading.weight}" + " ",
            )
            Text(text = formatter.format(zonedDateTime))
          }
        }
        items(bodyFatList) { reading ->
          Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // show local date and time
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            val zonedDateTime =
              dateTimeWithOffsetOrDefault(reading.time, reading.zoneOffset)
            Text(
              text = "${reading.percentage}" + " ",
            )
            Text(text = formatter.format(zonedDateTime))
          }
        }
      }
    }
  }
}
