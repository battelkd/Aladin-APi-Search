package com.jxino.aladinaccessiblebookapp.ui

import com.jxino.aladinaccessiblebookapp.data.BookRepository
import com.jxino.aladinaccessiblebookapp.data.BookSearchError
import com.jxino.aladinaccessiblebookapp.data.BookSearchResponse
import com.jxino.aladinaccessiblebookapp.domain.BasicResultAnnouncer
import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult
import com.jxino.aladinaccessiblebookapp.domain.RuleBasedUserUtteranceParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookSearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `api key missing changes ui state`() = runTest {
        val viewModel = createViewModel(BookSearchResponse.Failure(BookSearchError.ApiKeyMissing))

        viewModel.onSpeechText("채식주의자 찾아줘")

        assertEquals(BookSearchUiState.ApiKeyMissing, viewModel.uiState.value)
    }

    @Test
    fun `search success shows ebook results`() = runTest {
        val viewModel = createViewModel(BookSearchResponse.Success(listOf(sampleResult())))

        viewModel.onSpeechText("채식주의자 찾아줘")

        val state = viewModel.uiState.value
        assertTrue(state is BookSearchUiState.Results)
        assertEquals("채식주의자", (state as BookSearchUiState.Results).query)
        assertEquals(1, state.results.size)
    }

    @Test
    fun `select result opens webview screen`() = runTest {
        val viewModel = createViewModel(BookSearchResponse.Success(listOf(sampleResult())))
        viewModel.onSpeechText("채식주의자 찾아줘")

        viewModel.onSpeechText("1번 선택")

        assertTrue(viewModel.screen.value is AppScreen.WebView)
    }

    private fun createViewModel(response: BookSearchResponse): BookSearchViewModel =
        BookSearchViewModel(
            repository = object : BookRepository {
                override suspend fun searchEbooks(query: String): BookSearchResponse = response
            },
            parser = RuleBasedUserUtteranceParser(),
            announcer = BasicResultAnnouncer(),
        )

    private fun sampleResult() = BookSearchResult(
        title = "채식주의자",
        author = "한강",
        publisher = "창비",
        priceSales = 13500,
        priceStandard = 15000,
        link = "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=1",
        isbn = "123",
        isbn13 = "1234567890123",
        mallType = "EBOOK",
        cover = null,
        isAdult = false,
        isFixedPrice = true,
        fileFormats = listOf("ePub"),
    )
}
