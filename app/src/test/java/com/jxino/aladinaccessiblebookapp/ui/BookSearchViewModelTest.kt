package com.jxino.aladinaccessiblebookapp.ui

import com.jxino.aladinaccessiblebookapp.data.BookRepository
import com.jxino.aladinaccessiblebookapp.data.BookSearchError
import com.jxino.aladinaccessiblebookapp.data.BookSearchResponse
import com.jxino.aladinaccessiblebookapp.domain.BasicResultAnnouncer
import com.jxino.aladinaccessiblebookapp.domain.BookSearchCriteria
import com.jxino.aladinaccessiblebookapp.domain.BookSearchEnhancer
import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult
import com.jxino.aladinaccessiblebookapp.domain.ParsedCommand
import com.jxino.aladinaccessiblebookapp.domain.RuleBasedUserUtteranceParser
import com.jxino.aladinaccessiblebookapp.domain.SearchResultEnhancementContext
import com.jxino.aladinaccessiblebookapp.domain.SpeechCommandEnhancementContext
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
    fun `speech text searches extracted title automatically`() = runTest {
        val fakeRepository = CapturingBookRepository(BookSearchResponse.Success(listOf(sampleResult("채식주의자"))))
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("한강 작가 채식주의자 검색해줘")

        assertEquals("채식주의자", fakeRepository.lastQuery)
        val state = viewModel.uiState.value
        assertTrue(state is BookSearchUiState.Results)
        assertEquals("채식주의자", (state as BookSearchUiState.Results).query)
    }

    @Test
    fun `speech text searches title from plain search command`() = runTest {
        val fakeRepository = CapturingBookRepository(BookSearchResponse.Success(listOf(sampleResult("채식주의자"))))
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("채식주의자 검색")

        assertEquals("채식주의자", fakeRepository.lastQuery)
    }

    @Test
    fun `speech text searches title from attached search command`() = runTest {
        val fakeRepository = CapturingBookRepository(BookSearchResponse.Success(listOf(sampleResult("채식주의자"))))
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("채식주의자검색해줘")

        assertEquals("채식주의자", fakeRepository.lastQuery)
    }

    @Test
    fun `speech text searches title only when author is inferred`() = runTest {
        val fakeRepository = CapturingBookRepository(BookSearchResponse.Success(listOf(sampleResult("채식주의자"))))
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("채식주의자 한강 검색해줘")

        assertEquals("채식주의자", fakeRepository.lastQuery)
    }

    @Test
    fun `speech text searches title only when author comes before title`() = runTest {
        val fakeRepository = CapturingBookRepository(BookSearchResponse.Success(listOf(sampleResult("채식주의자"))))
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("한강 채식주의자 검색해줘")

        assertEquals("채식주의자", fakeRepository.lastQuery)
    }

    @Test
    fun `speech text searches title from natural phrase without search word`() = runTest {
        val fakeRepository = CapturingBookRepository(BookSearchResponse.Success(listOf(sampleResult("채식주의자"))))
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("채식주의자 한강")

        assertEquals("채식주의자", fakeRepository.lastQuery)
    }

    @Test
    fun `search shows all returned ebook results`() = runTest {
        val fakeRepository = CapturingBookRepository(
            BookSearchResponse.Success(
                listOf(
                    sampleResult("채식주의자"),
                    sampleResult("한강, 채식주의자 깊게 읽기"),
                    sampleResult("해방촌의 채식주의자"),
                ),
            ),
        )
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("채식주의자 검색해줘")

        val state = viewModel.uiState.value
        assertTrue(state is BookSearchUiState.Results)
        assertEquals(3, (state as BookSearchUiState.Results).results.size)
    }

    @Test
    fun `search limits visible results to five`() = runTest {
        val fakeRepository = CapturingBookRepository(
            BookSearchResponse.Success(
                (1..6).map { sampleResult("검색 결과 $it") },
            ),
        )
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("채식주의자 검색해줘")

        val state = viewModel.uiState.value
        assertTrue(state is BookSearchUiState.Results)
        val results = (state as BookSearchUiState.Results).results
        assertEquals(5, results.size)
        assertEquals("검색 결과 5", results.last().title)
    }

    @Test
    fun `internet unavailable changes ui state`() = runTest {
        val fakeRepository = CapturingBookRepository(
            BookSearchResponse.Failure(BookSearchError.InternetUnavailable),
        )
        val viewModel = createViewModel(fakeRepository)

        viewModel.onSpeechText("채식주의자 검색해줘")

        assertEquals(BookSearchUiState.InternetUnavailable, viewModel.uiState.value)
    }

    @Test
    fun `enhancer can rewrite parsed search command before repository search`() = runTest {
        val fakeRepository = CapturingBookRepository(BookSearchResponse.Success(listOf(sampleResult("소년이 온다"))))
        val viewModel = createViewModel(
            repository = fakeRepository,
            enhancer = RewritingSearchEnhancer(rewrittenTitle = "소년이 온다"),
        )

        viewModel.onSpeechText("채식주의자 검색해줘")

        assertEquals("소년이 온다", fakeRepository.lastQuery)
    }

    @Test
    fun `enhancer can transform api results before ui display`() = runTest {
        val fakeRepository = CapturingBookRepository(
            BookSearchResponse.Success(
                listOf(
                    sampleResult("채식주의자"),
                    sampleResult("채식주의자 해설"),
                ),
            ),
        )
        val viewModel = createViewModel(
            repository = fakeRepository,
            enhancer = ResultFilteringEnhancer(titleToKeep = "채식주의자 해설"),
        )

        viewModel.onSpeechText("채식주의자 검색해줘")

        val state = viewModel.uiState.value
        assertTrue(state is BookSearchUiState.Results)
        assertEquals(listOf("채식주의자 해설"), (state as BookSearchUiState.Results).results.map { it.title })
    }

    private fun createViewModel(
        repository: BookRepository,
        enhancer: BookSearchEnhancer = object : BookSearchEnhancer {
            override suspend fun enhanceParsedCommand(
                parsedCommand: ParsedCommand,
                context: SpeechCommandEnhancementContext,
            ): ParsedCommand = parsedCommand

            override suspend fun enhanceSearchResults(
                results: List<BookSearchResult>,
                context: SearchResultEnhancementContext,
            ): List<BookSearchResult> = results
        },
    ): BookSearchViewModel =
        BookSearchViewModel(
            repository = repository,
            parser = RuleBasedUserUtteranceParser(),
            announcer = BasicResultAnnouncer(),
            enhancer = enhancer,
        )

    private class CapturingBookRepository(
        private val response: BookSearchResponse,
    ) : BookRepository {
        var lastQuery: String? = null

        override suspend fun searchEbooks(query: String): BookSearchResponse {
            lastQuery = query
            return response
        }
    }

    private class RewritingSearchEnhancer(
        private val rewrittenTitle: String,
    ) : BookSearchEnhancer {
        override suspend fun enhanceParsedCommand(
            parsedCommand: ParsedCommand,
            context: SpeechCommandEnhancementContext,
        ): ParsedCommand =
            if (parsedCommand is ParsedCommand.Search) {
                ParsedCommand.Search(BookSearchCriteria(title = rewrittenTitle))
            } else {
                parsedCommand
            }

        override suspend fun enhanceSearchResults(
            results: List<BookSearchResult>,
            context: SearchResultEnhancementContext,
        ): List<BookSearchResult> = results
    }

    private class ResultFilteringEnhancer(
        private val titleToKeep: String,
    ) : BookSearchEnhancer {
        override suspend fun enhanceParsedCommand(
            parsedCommand: ParsedCommand,
            context: SpeechCommandEnhancementContext,
        ): ParsedCommand = parsedCommand

        override suspend fun enhanceSearchResults(
            results: List<BookSearchResult>,
            context: SearchResultEnhancementContext,
        ): List<BookSearchResult> =
            results.filter { it.title == titleToKeep }
    }

    private fun sampleResult(title: String) = BookSearchResult(
        title = title,
        author = "한강",
        publisher = "창비",
        priceSales = 13600,
        priceStandard = 16000,
        link = "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=385408193",
        isbn = "123",
        isbn13 = "1234567890123",
        mallType = "EBOOK",
        cover = null,
        isAdult = false,
        isFixedPrice = true,
        fileFormats = listOf("ePub"),
    )
}
