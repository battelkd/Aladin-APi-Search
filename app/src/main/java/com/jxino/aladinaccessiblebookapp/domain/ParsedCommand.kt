package com.jxino.aladinaccessiblebookapp.domain

data class UtteranceContext(
    val hasSearchResults: Boolean,
    val resultTitles: List<String>,
)

sealed class ParsedCommand {
    data class Search(val query: String) : ParsedCommand()
    data class SelectResult(val index: Int) : ParsedCommand()
    data class SelectByTitle(val titleKeyword: String) : ParsedCommand()
    data object Unknown : ParsedCommand()
}

interface UserUtteranceParser {
    fun parse(text: String, context: UtteranceContext): ParsedCommand
}
