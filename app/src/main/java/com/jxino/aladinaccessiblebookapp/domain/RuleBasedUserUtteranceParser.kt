package com.jxino.aladinaccessiblebookapp.domain

class RuleBasedUserUtteranceParser : UserUtteranceParser {
    private enum class SearchField {
        Title,
        Author,
        Publisher,
    }

    private val commandPhrases = listOf(
        "알라딘에서 찾아줘",
        "알라딘에서 검색해줘",
        "전자책 검색해줘",
        "전자책 검색해 주세요",
        "전자책 검색 해 줘",
        "전자책 찾아줘",
        "전자책 찾아 주세요",
        "전자책 찾아 줘",
        "이북 검색해줘",
        "이북 검색 해 줘",
        "이북 찾아줘",
        "검색해 주세요",
        "검색 해 주세요",
        "검색해 줘",
        "검색 해줘",
        "검색 해 줘",
        "찾아 주세요",
        "찾아 줘",
        "검색해줘요",
        "찾아줘요",
        "검색하고 싶어",
        "찾고 싶어",
        "책 찾아줘",
        "검색해줘",
        "찾아줘",
    )

    private val fillerTokens = setOf(
        "검색",
        "검색해",
        "검색해줘",
        "검색해줘요",
        "해",
        "줘",
        "줘요",
        "찾아",
        "찾아줘",
        "찾아줘요",
        "찾기",
        "해줘",
        "해주세요",
        "해줄래",
        "줄래",
        "주세요",
        "알려줘",
        "알려줘요",
        "찾고",
        "검색하고",
        "싶어",
        "좀",
        "부탁",
        "부탁해",
        "부탁해요",
        "책",
        "도서",
        "전자책",
        "이북",
    )

    private val searchCommandAffixes = listOf(
        "전자책 검색 해 주세요",
        "전자책 검색 해 줘",
        "전자책 검색해주세요",
        "전자책 검색해 주세요",
        "전자책 검색해줘요",
        "전자책 검색해줘",
        "전자책 검색",
        "전자책 찾아 주세요",
        "전자책 찾아 줘",
        "전자책 찾아줘요",
        "전자책 찾아줘",
        "전자책 찾아",
        "이북 검색 해 줘",
        "이북 검색해줘",
        "이북 검색",
        "이북 찾아줘",
        "이북 찾아",
        "검색 해 주세요",
        "검색 해 줘",
        "검색해주세요",
        "검색해 주세요",
        "검색해줘요",
        "검색해줘",
        "검색해 줘",
        "검색 해줘",
        "검색해",
        "검색",
        "찾아 주세요",
        "찾아 줘",
        "찾아줘요",
        "찾아줘",
        "찾아",
        "찾기",
        "알려 주세요",
        "알려 줘",
        "알려줘요",
        "알려줘",
        "보여 주세요",
        "보여 줘",
        "보여줘",
        "부탁해요",
        "부탁해",
        "부탁",
    )

    private val titleMarkers = setOf("제목", "책제목", "도서명", "책이름")
    private val authorMarkers = setOf("작가", "저자", "지은이")
    private val publisherMarkers = setOf("출판사", "출판", "출판처")
    private val authorConnectors = setOf("쓴", "지은", "집필한", "작성한")
    private val publisherConnectors = setOf("나온", "출간한", "출판한", "펴낸")
    private val selectionIntentTokens = setOf(
        "선택",
        "선택해",
        "선택해줘",
        "열어",
        "열어줘",
        "보여줘",
        "들어가",
        "들어가줘",
        "봐줘",
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

        val query = extractSearchQuery(normalized)
        val isSearchUtterance = query != normalized
        if (isSearchUtterance) {
            val criteria = extractSearchCriteria(query)
            if (criteria.hasAnyValue) return ParsedCommand.Search(criteria)
        }

        if (context.hasSearchResults) {
            parseSelection(normalized, context)?.let { return it }
        }

        val criteria = extractSearchCriteria(query)
        return if (criteria.hasAnyValue) ParsedCommand.Search(criteria) else ParsedCommand.Unknown
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
                    simplifiedTitle.isNotBlank() && text == simplifiedTitle -> index to title
                    simplifiedTitle.isNotBlank() && text.contains(simplifiedTitle) && text.hasSelectionIntent() -> index to title
                    simplifiedTitle.isNotBlank() && simplifiedTitle.contains(text) && text.hasSelectionIntent() -> index to text
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
        return query.stripSearchCommandAffixes()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.stripSearchCommandAffixes(): String {
        var result = replace(Regex("\\s+"), " ").trim()
        var changed: Boolean
        do {
            changed = false
            searchCommandAffixes.forEach { affix ->
                val stripped = result
                    .removeCommandPrefix(affix)
                    .removeCommandSuffix(affix)
                if (stripped != result) {
                    result = stripped.replace(Regex("\\s+"), " ").trim()
                    changed = true
                }
            }
        } while (changed)
        return result
    }

    private fun String.removeCommandPrefix(command: String): String =
        when {
            this == command -> ""
            startsWith("$command ") -> removePrefix(command).trim()
            else -> this
        }

    private fun String.removeCommandSuffix(command: String): String =
        when {
            this == command -> ""
            endsWith(" $command") -> dropLast(command.length).trim()
            endsWith(command) && length > command.length + 1 -> dropLast(command.length).trim()
            else -> this
        }

    private fun extractSearchCriteria(text: String): BookSearchCriteria {
        val tokens = tokenizeSearchText(text)
        if (tokens.isEmpty()) return BookSearchCriteria()

        val used = BooleanArray(tokens.size)
        var title = ""
        var author = ""
        var publisher = ""

        tokens.forEachIndexed { index, token ->
            val field = markerField(token) ?: return@forEachIndexed
            used[index] = true
            when (field) {
                SearchField.Title -> {
                    val titleIndexes = collectAfterUntilMarker(index, tokens, used)
                    title = title.ifBlank {
                        cleanTitleValue(titleIndexes.joinToString(" ") { tokens[it] })
                            .ifBlank { cleanTitleValue(collectBefore(index, tokens, used).orEmpty()) }
                    }
                    titleIndexes.forEach { used[it] = true }
                }
                SearchField.Author -> {
                    val value = collectAuthorValue(index, tokens, used)
                    author = author.ifBlank { cleanFieldValue(value.orEmpty()) }
                }
                SearchField.Publisher -> {
                    val value = collectPublisherValue(index, tokens, used)
                    publisher = publisher.ifBlank { cleanFieldValue(value.orEmpty()) }
                }
            }
        }

        tokens.forEachIndexed { index, token ->
            val field = connectorField(token) ?: return@forEachIndexed
            used[index] = true
            when (field) {
                SearchField.Author -> {
                    val value = collectAuthorValue(index, tokens, used)
                    author = author.ifBlank { cleanFieldValue(value.orEmpty()) }
                }
                SearchField.Publisher -> {
                    val value = collectPublisherValue(index, tokens, used)
                    publisher = publisher.ifBlank { cleanFieldValue(value.orEmpty()) }
                }
                SearchField.Title -> Unit
            }
        }

        val remainingTokens = tokens
            .filterIndexed { index, _ -> !used[index] }
            .map(::cleanTitleValue)
            .filter { it.isNotBlank() }

        if (title.isBlank() && remainingTokens.isNotEmpty()) {
            val inferred = inferTitleAndAuthor(remainingTokens, author)
            title = inferred.first
            author = author.ifBlank { inferred.second }
        }

        return BookSearchCriteria(
            title = cleanTitleValue(title),
            author = cleanFieldValue(author),
            publisher = cleanFieldValue(publisher),
        )
    }

    private fun tokenizeSearchText(text: String): List<String> =
        normalize(text)
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() && it !in fillerTokens }

    private fun markerField(token: String): SearchField? {
        val marker = cleanMarker(token)
        return when (marker) {
            in titleMarkers -> SearchField.Title
            in authorMarkers -> SearchField.Author
            in publisherMarkers -> SearchField.Publisher
            else -> null
        }
    }

    private fun connectorField(token: String): SearchField? {
        val marker = cleanMarker(token)
        return when (marker) {
            in authorConnectors -> SearchField.Author
            in publisherConnectors -> SearchField.Publisher
            else -> null
        }
    }

    private fun cleanMarker(token: String): String {
        val trimmed = token.trim().removeSuffix("님")
        if (isKnownMarkerOrConnector(trimmed)) return trimmed

        val particles = listOf("이라는", "라고", "에서", "으로", "로", "인", "의", "은", "는", "이", "가")
        for (particle in particles) {
            if (trimmed.length > particle.length + 1 && trimmed.endsWith(particle)) {
                val candidate = trimmed.dropLast(particle.length)
                if (isKnownMarkerOrConnector(candidate)) return candidate
            }
        }
        return trimmed
    }

    private fun isKnownMarkerOrConnector(value: String): Boolean =
        value in titleMarkers ||
            value in authorMarkers ||
            value in publisherMarkers ||
            value in authorConnectors ||
            value in publisherConnectors

    private fun collectBefore(index: Int, tokens: List<String>, used: BooleanArray): String? {
        val targetIndex = (index - 1 downTo 0)
            .firstOrNull { !used[it] && markerField(tokens[it]) == null && connectorField(tokens[it]) == null }
            ?: return null
        used[targetIndex] = true
        return tokens[targetIndex]
    }

    private fun collectAfter(index: Int, tokens: List<String>, used: BooleanArray): String? {
        val targetIndex = (index + 1 until tokens.size)
            .firstOrNull { !used[it] && markerField(tokens[it]) == null && connectorField(tokens[it]) == null }
            ?: return null
        used[targetIndex] = true
        return tokens[targetIndex]
    }

    private fun collectAuthorValue(index: Int, tokens: List<String>, used: BooleanArray): String? {
        val before = collectPersonIndexesBefore(index, tokens, used)
        val after = collectPersonIndexesAfter(index, tokens, used)
        val selected = when {
            before.isNotEmpty() && after.isEmpty() -> before
            after.isNotEmpty() -> after
            else -> emptyList()
        }
        selected.forEach { used[it] = true }
        return selected.joinToString(" ") { tokens[it] }.ifBlank { null }
    }

    private fun collectPublisherValue(index: Int, tokens: List<String>, used: BooleanArray): String? {
        val before = previousAvailableIndex(index, tokens, used)
        val after = nextAvailableIndex(index, tokens, used)
        val selected = when {
            after != null && before != null && looksLikeTitleToken(tokens[before]) -> after
            before != null -> before
            after != null -> after
            else -> null
        } ?: return null
        used[selected] = true
        return tokens[selected]
    }

    private fun collectPersonIndexesBefore(index: Int, tokens: List<String>, used: BooleanArray): List<Int> {
        val result = mutableListOf<Int>()
        for (candidate in index - 1 downTo 0) {
            if (!canUseFieldToken(candidate, tokens, used)) break
            if (!looksLikePersonName(tokens[candidate])) break
            result += candidate
            if (result.size == 3) break
        }
        return result.asReversed()
    }

    private fun collectPersonIndexesAfter(index: Int, tokens: List<String>, used: BooleanArray): List<Int> {
        val result = mutableListOf<Int>()
        for (candidate in index + 1 until tokens.size) {
            if (!canUseFieldToken(candidate, tokens, used)) break
            if (!looksLikePersonName(tokens[candidate])) break
            result += candidate
            if (result.size == 3) break
        }
        return result
    }

    private fun previousAvailableIndex(index: Int, tokens: List<String>, used: BooleanArray): Int? =
        (index - 1 downTo 0).firstOrNull { canUseFieldToken(it, tokens, used) }

    private fun nextAvailableIndex(index: Int, tokens: List<String>, used: BooleanArray): Int? =
        (index + 1 until tokens.size).firstOrNull { canUseFieldToken(it, tokens, used) }

    private fun canUseFieldToken(index: Int, tokens: List<String>, used: BooleanArray): Boolean =
        !used[index] && markerField(tokens[index]) == null && connectorField(tokens[index]) == null

    private fun collectAfterUntilMarker(index: Int, tokens: List<String>, used: BooleanArray): List<Int> {
        val result = mutableListOf<Int>()
        for (candidate in index + 1 until tokens.size) {
            if (used[candidate] || markerField(tokens[candidate]) != null || connectorField(tokens[candidate]) != null) break
            result += candidate
        }
        return result
    }

    private fun inferTitleAndAuthor(tokens: List<String>, currentAuthor: String): Pair<String, String> {
        if (tokens.size < 2 || currentAuthor.isNotBlank()) {
            return cleanTitleValue(tokens.joinToString(" ")) to ""
        }

        val first = tokens.first()
        val last = tokens.last()
        val maxMultiTokenAuthorSize = minOf(3, tokens.size - 1)
        for (authorTokenSize in maxMultiTokenAuthorSize downTo 2) {
            val trailingAuthorTokens = tokens.takeLast(authorTokenSize)
            val trailingTitleTokens = tokens.dropLast(authorTokenSize)
            if (trailingAuthorTokens.all(::looksLikePersonName)) {
                val title = trailingTitleTokens.joinToString(" ")
                if (title.isLikelyBookTitle()) {
                    return cleanTitleValue(title) to cleanFieldValue(trailingAuthorTokens.joinToString(" "))
                }
            }

            val leadingAuthorTokens = tokens.take(authorTokenSize)
            val leadingTitleTokens = tokens.drop(authorTokenSize)
            if (leadingAuthorTokens.all(::looksLikePersonName)) {
                val title = leadingTitleTokens.joinToString(" ")
                if (title.isLikelyBookTitle()) {
                    return cleanTitleValue(title) to cleanFieldValue(leadingAuthorTokens.joinToString(" "))
                }
            }
        }

        val withoutFirst = tokens.drop(1).joinToString(" ")
        val withoutLast = tokens.dropLast(1).joinToString(" ")

        if (looksLikePersonName(last) && withoutLast.isLikelyTitleBeforeInferredAuthor()) {
            return cleanTitleValue(withoutLast) to cleanFieldValue(last)
        }
        if (looksLikePersonName(first) && withoutFirst.isLikelyTitleBeforeInferredAuthor()) {
            return cleanTitleValue(withoutFirst) to cleanFieldValue(first)
        }
        return cleanTitleValue(tokens.joinToString(" ")) to ""
    }

    private fun looksLikePersonName(value: String): Boolean {
        val compact = value.replace(Regex("[^가-힣a-zA-Z]"), "")
        return compact.length in 2..4 && compact.isNotBlank()
    }

    private fun looksLikeTitleToken(value: String): Boolean {
        val compact = value.replace(" ", "")
        return compact.any(Char::isDigit) || compact.length > 4
    }

    private fun String.isLikelyBookTitle(): Boolean {
        val compact = replace(" ", "")
        return compact.length >= 3
    }

    private fun String.isLikelyTitleBeforeInferredAuthor(): Boolean {
        val compact = replace(" ", "")
        return compact.any(Char::isDigit) || compact.length >= 4
    }

    private fun cleanTitleValue(value: String): String =
        cleanTrailingParticles(value)

    private fun cleanFieldValue(value: String): String =
        cleanTrailingParticles(value)
            .removeSuffix("님")
            .trim()

    private fun cleanTrailingParticles(value: String): String {
        var result = value.replace(Regex("\\s+"), " ").trim()
        listOf("이라는책", "이라는", "이라고", "라는", "라고", "이란", "란", "인", "에서", "으로", "로", "을", "를", "은", "는", "이", "가", "의").forEach { particle ->
            if (result.length > particle.length + 1 && result.endsWith(particle)) {
                result = result.dropLast(particle.length).trim()
            }
        }
        return result
    }

    private fun String.hasSelectionIntent(): Boolean =
        split(" ").any { token -> selectionIntentTokens.any { intent -> token == intent || token.endsWith(intent) } }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[,.!?\"'“”‘’]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
