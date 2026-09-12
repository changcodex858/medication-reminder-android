package com.example.medicationreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.medicationreminder.ui.AppViewModel
import com.example.medicationreminder.ui.MedicationReminderRoot
import com.example.medicationreminder.ui.theme.MedicationReminderTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.factory(
            application = application,
            repository = appGraph.medicationRepository,
            preferencesRepository = appGraph.preferencesRepository,
            alarmCoordinator = appGraph.alarmCoordinator,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedicationReminderTheme {
                MedicationReminderRoot(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }
}
