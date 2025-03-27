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

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.presentation.screen.inputreadings.InputReadingsViewModel
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
val zoneId: ZoneId = ZoneId.of("America/New_York")

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout);

    val healthConnectManager = (application as BaseApplication).healthConnectManager
    val postManager = (application as BaseApplication).postManager

    val sharedPref = getPreferences(Context.MODE_PRIVATE)

    val dateView = findViewById<TextView>(R.id.dateView)
    dateView.text = sharedPref.getString("Date", "")

    findViewById<Button>(R.id.dateButton).setOnClickListener {
      val mcurrentDate: Calendar = Calendar.getInstance()
      var mYear = mcurrentDate.get(Calendar.YEAR)
      var mMonth = mcurrentDate.get(Calendar.MONTH)
      var mDay = mcurrentDate.get(Calendar.DAY_OF_MONTH)

      val mDatePicker = DatePickerDialog(
        this@MainActivity,
        { datepicker, selectedyear, selectedmonth, selectedday ->
          val myCalendar: Calendar = Calendar.getInstance()
          myCalendar.set(Calendar.YEAR, selectedyear)
          myCalendar.set(Calendar.MONTH, selectedmonth)
          myCalendar.set(Calendar.DAY_OF_MONTH, selectedday)
          dateView.text = myCalendar.time.toInstant().atZone(zoneId)
            .toLocalDate().format(formatter)
          with(sharedPref.edit()) {
            putString("Date", dateView.text as String?)
            apply()
          }

          mDay = selectedday
          mMonth = selectedmonth
          mYear = selectedyear
        },mYear, mMonth, mDay
      )
      mDatePicker.show()
    }

    val inputReadingsViewModel = InputReadingsViewModel(
      activity = this,
      healthConnectManager = healthConnectManager,
      postManager = postManager,
      output = findViewById(R.id.output),
      date = dateView
    )

    findViewById<Button>(R.id.syncButton).setOnClickListener {
      runBlocking { inputReadingsViewModel.run() }
    }

    findViewById<Button>(R.id.exitButton).setOnClickListener {
      finish()
    }
  }
}
