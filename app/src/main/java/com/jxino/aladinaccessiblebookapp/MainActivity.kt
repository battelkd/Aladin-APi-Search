package com.jxino.aladinaccessiblebookapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jxino.aladinaccessiblebookapp.data.AladinRepository
import com.jxino.aladinaccessiblebookapp.domain.BasicResultAnnouncer
import com.jxino.aladinaccessiblebookapp.domain.RuleBasedUserUtteranceParser
import com.jxino.aladinaccessiblebookapp.speech.SpeechRecognizerManager
import com.jxino.aladinaccessiblebookapp.speech.TtsManager
import com.jxino.aladinaccessiblebookapp.ui.AladinAccessibleBookApp
import com.jxino.aladinaccessiblebookapp.ui.AppScreen
import com.jxino.aladinaccessiblebookapp.ui.BookSearchViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<com.jxino.aladinaccessiblebookapp.ui.BookSearchViewModel> {
        BookSearchViewModelFactory(
            repository = AladinRepository(BuildConfig.ALADIN_TTB_KEY),
            parser = RuleBasedUserUtteranceParser(),
            announcer = BasicResultAnnouncer(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val screen by viewModel.screen.collectAsStateWithLifecycle()
            var hasAudioPermission by remember {
                mutableStateOf(checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            }
            var hasRequestedAudioPermission by rememberSaveable { mutableStateOf(false) }
            var shouldOpenAppSettingsForAudio by rememberSaveable { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                hasAudioPermission = granted
                shouldOpenAppSettingsForAudio = hasRequestedAudioPermission &&
                    !granted &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
                if (!granted) viewModel.onPermissionDenied()
            }
            val ttsManager = remember { TtsManager(this) }
            val speechManager = remember {
                SpeechRecognizerManager(
                    context = this,
                    onResult = viewModel::onSpeechText,
                    onError = { failure -> viewModel.onSpeechError(failure.userMessage) },
                    onReady = viewModel::onListeningStarted,
                )
            }

            LaunchedEffect(Unit) {
                viewModel.ttsEvents.collect { ttsManager.speak(it) }
            }

            androidx.compose.runtime.DisposableEffect(Unit) {
                onDispose {
                    speechManager.destroy()
                    ttsManager.shutdown()
                }
            }

            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasAudioPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasAudioPermission) {
                            shouldOpenAppSettingsForAudio = false
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            AladinAccessibleBookApp(
                uiState = uiState,
                screen = screen,
                cartActionRequests = viewModel.cartActionRequests,
                hasAudioPermission = hasAudioPermission,
                shouldOpenAppSettingsForAudio = shouldOpenAppSettingsForAudio,
                onRequestPermission = {
                    if (hasAudioPermission) return@AladinAccessibleBookApp
                    if (shouldOpenAppSettingsForAudio) {
                        openAppSettings()
                    } else {
                        hasRequestedAudioPermission = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStartListening = {
                    if (hasAudioPermission) {
                        ttsManager.stop()
                        viewModel.onListeningStarted()
                        speechManager.startListening()
                    } else {
                        hasRequestedAudioPermission = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopListening = speechManager::stopListening,
                onResultClicked = viewModel::onResultClicked,
                onBackToSearch = viewModel::onBackToSearch,
                onWebViewLoadingChanged = viewModel::onWebViewLoadingChanged,
                onCartActionResult = viewModel::onCartActionResult,
            )
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
