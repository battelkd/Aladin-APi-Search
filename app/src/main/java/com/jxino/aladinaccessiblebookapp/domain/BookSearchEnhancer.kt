package com.jxino.aladinaccessiblebookapp.domain

data class SpeechCommandEnhancementContext(
    val originalSpeechText: String,
    val utteranceContext: UtteranceContext,
)

data class SearchResultEnhancementContext(
    val originalSpeechText: String,
    val requestedTitle: String,
    val resultsBeforeEnhancement: List<BookSearchResult>,
)

interface BookSearchEnhancer {
    suspend fun enhanceParsedCommand(
        parsedCommand: ParsedCommand,
        context: SpeechCommandEnhancementContext,
    ): ParsedCommand

    suspend fun enhanceSearchResults(
        results: List<BookSearchResult>,
        context: SearchResultEnhancementContext,
    ): List<BookSearchResult>
}

class PassthroughBookSearchEnhancer : BookSearchEnhancer {
    override suspend fun enhanceParsedCommand(
        parsedCommand: ParsedCommand,
        context: SpeechCommandEnhancementContext,
    ): ParsedCommand = parsedCommand

    override suspend fun enhanceSearchResults(
        results: List<BookSearchResult>,
        context: SearchResultEnhancementContext,
    ): List<BookSearchResult> = results
}
