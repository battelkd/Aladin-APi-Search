package com.jxino.aladinaccessiblebookapp.domain

data class UtteranceContext(
    val hasSearchResults: Boolean,
    val resultTitles: List<String>,
)

data class BookSearchCriteria(
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
) {
    val hasAnyValue: Boolean
        get() = title.isNotBlank() || author.isNotBlank() || publisher.isNotBlank()

    fun toSearchQuery(): String =
        listOf(title, author, publisher)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

sealed class ParsedCommand {
    data class Search(val criteria: BookSearchCriteria) : ParsedCommand()
    data class SelectResult(val index: Int) : ParsedCommand()
    data class SelectByTitle(val titleKeyword: String) : ParsedCommand()
    data object Unknown : ParsedCommand()
}

interface UserUtteranceParser {
    fun parse(text: String, context: UtteranceContext): ParsedCommand
}
