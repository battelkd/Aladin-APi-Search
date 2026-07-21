package com.jxino.aladinaccessiblebookapp.ui

import androidx.compose.runtime.Composable
import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult
import com.jxino.aladinaccessiblebookapp.domain.CartActionResult
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun AladinAccessibleBookApp(
    uiState: BookSearchUiState,
    screen: AppScreen,
    cartActionRequests: SharedFlow<Unit>,
    hasAudioPermission: Boolean,
    shouldOpenAppSettingsForAudio: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onResultClicked: (BookSearchResult) -> Unit,
    onBackToSearch: () -> Unit,
    onWebViewLoadingChanged: (Boolean) -> Unit,
    onCartActionResult: (CartActionResult) -> Unit,
) {
    AppTheme {
        VoiceSearchScreen(
            uiState = uiState,
            screen = screen,
            cartActionRequests = cartActionRequests,
            hasAudioPermission = hasAudioPermission,
            shouldOpenAppSettingsForAudio = shouldOpenAppSettingsForAudio,
            onRequestPermission = onRequestPermission,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onResultClicked = onResultClicked,
            onBackToSearch = onBackToSearch,
            onWebViewLoadingChanged = onWebViewLoadingChanged,
            onCartActionResult = onCartActionResult,
        )
    }
}
