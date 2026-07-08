package com.jxino.aladinaccessiblebookapp.domain

class RuleBasedUserUtteranceParser : UserUtteranceParser {
    private val commandPhrases = listOf(
        "알라딘에서 찾아줘",
        "전자책 검색해줘",
        "전자책 찾아줘",
        "이북 검색해줘",
        "이북 찾아줘",
        "책 찾아줘",
        "검색해줘",
        "찾아줘",
    )

    private val numberSelections = listOf(
        "1번 선택" to 0,
        "첫 번째 선택" to 0,
        "1번" to 0,
        "일번" to 0,
        "첫 번째" to 0,
        "2번 선택" to 1,
        "두 번째 선택" to 1,
        "2번" to 1,
        "이번" to 1,
        "두 번째" to 1,
        "3번 선택" to 2,
        "세 번째 선택" to 2,
        "3번" to 2,
        "삼번" to 2,
        "세 번째" to 2,
        "4번 선택" to 3,
        "네 번째 선택" to 3,
        "4번" to 3,
        "사번" to 3,
        "네 번째" to 3,
        "5번 선택" to 4,
        "다섯 번째 선택" to 4,
        "5번" to 4,
        "오번" to 4,
        "다섯 번째" to 4,
    )

    override fun parse(text: String, context: UtteranceContext): ParsedCommand {
        val normalized = normalize(text)
        if (normalized.isBlank()) return ParsedCommand.Unknown

        if (context.hasSearchResults) {
            parseSelection(normalized, context)?.let { return it }
        }

        val query = extractSearchQuery(normalized)
        return if (query.isBlank()) ParsedCommand.Unknown else ParsedCommand.Search(query)
    }

    private fun parseSelection(text: String, context: UtteranceContext): ParsedCommand? {
        val numericMatches = numberSelections
            .filter { (phrase, _) -> text == phrase || text.contains(phrase) }
            .map { it.second }
            .distinct()

        if (numericMatches.size == 1) {
            return ParsedCommand.SelectResult(numericMatches.first())
        }
        if (numericMatches.size > 1) {
            return ParsedCommand.Unknown
        }

        val titleMatches = context.resultTitles
            .mapIndexedNotNull { index, title ->
                val simplifiedTitle = normalize(title)
                when {
                    simplifiedTitle.isNotBlank() && text.contains(simplifiedTitle) -> index to title
                    simplifiedTitle.isNotBlank() && simplifiedTitle.contains(text) -> index to text
                    else -> null
                }
            }

        return when (titleMatches.size) {
            0 -> null
            1 -> ParsedCommand.SelectByTitle(titleMatches.first().second)
            else -> ParsedCommand.Unknown
        }
    }

    private fun extractSearchQuery(text: String): String {
        var query = text
        commandPhrases.forEach { phrase ->
            query = query.replace(phrase, " ")
        }
        return query.replace(Regex("\\s+"), " ").trim()
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[,.!?]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
