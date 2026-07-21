package com.jxino.aladinaccessiblebookapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jxino.aladinaccessiblebookapp.data.BookRepository
import com.jxino.aladinaccessiblebookapp.data.BookSearchError
import com.jxino.aladinaccessiblebookapp.data.BookSearchResponse
import com.jxino.aladinaccessiblebookapp.data.buildAladinSearchFallbackUrl
import com.jxino.aladinaccessiblebookapp.domain.BookSearchEnhancer
import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult
import com.jxino.aladinaccessiblebookapp.domain.PassthroughBookSearchEnhancer
import com.jxino.aladinaccessiblebookapp.domain.ParsedCommand
import com.jxino.aladinaccessiblebookapp.domain.ResultAnnouncer
import com.jxino.aladinaccessiblebookapp.domain.SearchResultEnhancementContext
import com.jxino.aladinaccessiblebookapp.domain.SpeechCommandEnhancementContext
import com.jxino.aladinaccessiblebookapp.domain.UserUtteranceParser
import com.jxino.aladinaccessiblebookapp.domain.UtteranceContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookSearchViewModel(
    private val repository: BookRepository,
    private val parser: UserUtteranceParser,
    private val announcer: ResultAnnouncer,
    private val enhancer: BookSearchEnhancer = PassthroughBookSearchEnhancer(),
) : ViewModel() {
    private val _uiState = MutableStateFlow<BookSearchUiState>(BookSearchUiState.Initial)
    val uiState: StateFlow<BookSearchUiState> = _uiState.asStateFlow()

    private val _screen = MutableStateFlow<AppScreen>(AppScreen.VoiceSearch)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _ttsEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val ttsEvents: SharedFlow<String> = _ttsEvents.asSharedFlow()

    private var currentResults: List<BookSearchResult> = emptyList()
    private var lastQuery: String = ""

    fun onPermissionDenied() {
        _uiState.value = BookSearchUiState.PermissionDenied
        speak("마이크 권한이 필요합니다.")
    }

    fun onListeningStarted() {
        _uiState.value = BookSearchUiState.Listening
    }

    fun onSpeechText(text: String) {
        viewModelScope.launch {
            val utteranceContext = UtteranceContext(
                hasSearchResults = currentResults.isNotEmpty(),
                resultTitles = currentResults.map { it.title },
            )
            val parsedCommand = parser.parse(text = text, context = utteranceContext)
            val command = enhanceParsedCommandSafely(
                parsedCommand = parsedCommand,
                context = SpeechCommandEnhancementContext(
                    originalSpeechText = text,
                    utteranceContext = utteranceContext,
                ),
            )
            handleCommand(command, originalSpeechText = text)
        }
    }

    fun onSpeechError(message: String = "음성을 인식하지 못했습니다. 다시 말씀해 주세요.") {
        _uiState.value = BookSearchUiState.SpeechNotRecognized(message)
        speak(message)
    }

    fun onResultClicked(result: BookSearchResult) {
        openResult(result)
    }

    fun onBackToSearch() {
        _screen.value = AppScreen.VoiceSearch
        _uiState.value = if (currentResults.isEmpty()) BookSearchUiState.Initial else BookSearchUiState.Results(lastQuery, currentResults)
    }

    fun onWebViewLoadingChanged(isLoading: Boolean) {
        if (_screen.value !is AppScreen.WebView) return
        _uiState.value = if (isLoading) BookSearchUiState.WebViewLoading else BookSearchUiState.WebViewLoaded
        if (isLoading) speak("알라딘 페이지를 불러오는 중입니다.")
    }

    private suspend fun handleCommand(command: ParsedCommand, originalSpeechText: String) {
        when (command) {
            is ParsedCommand.Search -> search(command.criteria.title, originalSpeechText)
            is ParsedCommand.SelectResult -> selectByIndex(command.index)
            is ParsedCommand.SelectByTitle -> selectByTitle(command.titleKeyword)
            ParsedCommand.Unknown -> {
                _uiState.value = BookSearchUiState.SpeechNotRecognized()
                speak("명령을 이해하지 못했습니다. 검색어 또는 결과 번호를 다시 말씀해 주세요.")
            }
        }
    }

    private suspend fun search(query: String, originalSpeechText: String) {
        val title = query.trim()
        if (title.isBlank()) {
            _uiState.value = BookSearchUiState.SpeechNotRecognized("검색할 책 제목을 찾지 못했습니다. 다시 말씀해 주세요.")
            speakErrorIfAvailable()
            return
        }
        lastQuery = title
        _uiState.value = BookSearchUiState.Searching
        when (val response = repository.searchEbooks(title)) {
            is BookSearchResponse.Success -> {
                currentResults = enhanceSearchResultsSafely(
                    results = response.results,
                    context = SearchResultEnhancementContext(
                        originalSpeechText = originalSpeechText,
                        requestedTitle = title,
                        resultsBeforeEnhancement = response.results,
                    ),
                ).take(MAX_VISIBLE_RESULTS)
                _uiState.value = if (currentResults.isEmpty()) {
                    BookSearchUiState.NoResults
                } else {
                    BookSearchUiState.Results(title, currentResults)
                }
                if (currentResults.isEmpty()) speakErrorIfAvailable() else speak(announcer.buildSearchResultsAnnouncement(currentResults))
            }
            is BookSearchResponse.Failure -> {
                _uiState.value = when (val error = response.error) {
                    BookSearchError.ApiKeyMissing -> BookSearchUiState.ApiKeyMissing
                    BookSearchError.InternetUnavailable -> BookSearchUiState.InternetUnavailable
                    is BookSearchError.AladinApiUnavailable -> BookSearchUiState.AladinApiUnavailable(error.message)
                    is BookSearchError.InvalidApiResponse -> BookSearchUiState.InvalidApiResponse(error.message)
                    is BookSearchError.Network -> BookSearchUiState.NetworkError(error.message)
                }
                speakErrorIfAvailable()
            }
        }
    }

    private suspend fun enhanceParsedCommandSafely(
        parsedCommand: ParsedCommand,
        context: SpeechCommandEnhancementContext,
    ): ParsedCommand =
        runCatching {
            enhancer.enhanceParsedCommand(parsedCommand, context)
        }.getOrElse {
            parsedCommand
        }

    private suspend fun enhanceSearchResultsSafely(
        results: List<BookSearchResult>,
        context: SearchResultEnhancementContext,
    ): List<BookSearchResult> =
        runCatching {
            enhancer.enhanceSearchResults(results, context)
        }.getOrElse {
            results
        }

    private fun selectByIndex(index: Int) {
        val result = currentResults.getOrNull(index)
        if (result == null) {
            _uiState.value = BookSearchUiState.SpeechNotRecognized("해당 번호의 검색 결과가 없습니다.")
            speak("해당 번호의 검색 결과가 없습니다.")
        } else {
            openResult(result)
        }
    }

    private fun selectByTitle(titleKeyword: String) {
        val matches = currentResults.filter { it.title.contains(titleKeyword, ignoreCase = true) || titleKeyword.contains(it.title, ignoreCase = true) }
        when (matches.size) {
            1 -> openResult(matches.first())
            0 -> {
                _uiState.value = BookSearchUiState.SpeechNotRecognized("해당 제목의 검색 결과를 찾지 못했습니다.")
                speak("해당 제목의 검색 결과를 찾지 못했습니다.")
            }
            else -> {
                _uiState.value = BookSearchUiState.AmbiguousSelection
                speakErrorIfAvailable()
            }
        }
    }

    private fun openResult(result: BookSearchResult) {
        val url = result.link?.takeIf { it.isNotBlank() } ?: buildAladinSearchFallbackUrl(result.title.ifBlank { lastQuery })
        _screen.value = AppScreen.WebView(url = url, title = result.title)
        _uiState.value = BookSearchUiState.WebViewLoading
        speak("${result.title} 알라딘 페이지를 엽니다.")
    }

    private fun speakErrorIfAvailable() {
        announcer.buildErrorAnnouncement(_uiState.value)?.let(::speak)
    }

    private fun speak(message: String) {
        _ttsEvents.tryEmit(message)
    }

    private companion object {
        const val MAX_VISIBLE_RESULTS = 5
    }
}
