package com.jxino.aladinaccessiblebookapp.ui

import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult

sealed class BookSearchUiState {
    data object Initial : BookSearchUiState()
    data object PermissionDenied : BookSearchUiState()
    data object Listening : BookSearchUiState()
    data class SpeechNotRecognized(val message: String = "음성을 인식하지 못했습니다. 다시 말씀해 주세요.") : BookSearchUiState()
    data object Searching : BookSearchUiState()
    data class Results(val query: String, val results: List<BookSearchResult>) : BookSearchUiState()
    data object NoResults : BookSearchUiState()
    data object ApiKeyMissing : BookSearchUiState()
    data class NetworkError(val message: String) : BookSearchUiState()
    data object AmbiguousSelection : BookSearchUiState()
    data object WebViewLoading : BookSearchUiState()
    data object WebViewLoaded : BookSearchUiState()
}

sealed class AppScreen {
    data object VoiceSearch : AppScreen()
    data class WebView(val url: String, val title: String) : AppScreen()
}
