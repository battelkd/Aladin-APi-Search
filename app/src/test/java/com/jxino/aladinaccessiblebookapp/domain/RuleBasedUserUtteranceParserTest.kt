package com.jxino.aladinaccessiblebookapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RuleBasedUserUtteranceParserTest {
    private val parser = RuleBasedUserUtteranceParser()

    @Test
    fun `parses search query without results`() {
        val command = parser.parse("채식주의자 찾아줘", emptyContext())

        assertEquals(ParsedCommand.Search(BookSearchCriteria(title = "채식주의자")), command)
    }

    @Test
    fun `extracts title from common search command variants`() {
        val utterances = listOf(
            "채식주의자 검색해줘",
            "채식주의자 검색",
            "채식주의자 검색 해 줘",
            "채식주의자검색",
            "채식주의자검색해줘",
            "채식주의자를 검색",
            "채식주의자 검색 부탁해",
            "검색 채식주의자",
            "\"채식주의자\" 검색해줘",
            "채식주의자 전자책 검색",
        )

        utterances.forEach { utterance ->
            assertEquals(
                utterance,
                ParsedCommand.Search(BookSearchCriteria(title = "채식주의자")),
                parser.parse(utterance, emptyContext()),
            )
        }
    }

    @Test
    fun `treats numeric utterance as search query when there are no results`() {
        val command = parser.parse("1984 찾아줘", emptyContext())

        assertEquals(ParsedCommand.Search(BookSearchCriteria(title = "1984")), command)
    }

    @Test
    fun `extracts author marker before title`() {
        val command = parser.parse("한강 작가 채식주의자 검색해줘", emptyContext())

        assertEquals(
            ParsedCommand.Search(BookSearchCriteria(title = "채식주의자", author = "한강")),
            command,
        )
    }

    @Test
    fun `infers trailing short person name as author`() {
        val command = parser.parse("채식주의자 한강 검색해줘", emptyContext())

        assertEquals(
            ParsedCommand.Search(BookSearchCriteria(title = "채식주의자", author = "한강")),
            command,
        )
    }

    @Test
    fun `infers leading short person name as author`() {
        val command = parser.parse("한강 채식주의자 검색해줘", emptyContext())

        assertEquals(
            ParsedCommand.Search(BookSearchCriteria(title = "채식주의자", author = "한강")),
            command,
        )
    }

    @Test
    fun `treats title and author phrase as new search when results exist`() {
        val command = parser.parse("채식주의자 한강", resultsContext())

        assertEquals(
            ParsedCommand.Search(BookSearchCriteria(title = "채식주의자", author = "한강")),
            command,
        )
    }

    @Test
    fun `cleans natural language title suffix`() {
        val command = parser.parse("채식주의자라는 책 찾아줘", emptyContext())

        assertEquals(ParsedCommand.Search(BookSearchCriteria(title = "채식주의자")), command)
    }

    @Test
    fun `infers leading multi word author before title`() {
        val command = parser.parse("조지 오웰 1984 검색해줘", emptyContext())

        assertEquals(
            ParsedCommand.Search(BookSearchCriteria(title = "1984", author = "조지 오웰")),
            command,
        )
    }

    @Test
    fun `infers trailing multi word author after title`() {
        val command = parser.parse("데미안 헤르만 헤세 찾아줘", emptyContext())

        assertEquals(
            ParsedCommand.Search(BookSearchCriteria(title = "데미안", author = "헤르만 헤세")),
            command,
        )
    }

    @Test
    fun `extracts publisher marker before title`() {
        val command = parser.parse("출판사 창비 채식주의자 검색해줘", emptyContext())

        assertEquals(
            ParsedCommand.Search(BookSearchCriteria(title = "채식주의자", publisher = "창비")),
            command,
        )
    }

    @Test
    fun `extracts author connector before title`() {
        val command = parser.parse("한강이 쓴 채식주의자 검색해줘", emptyContext())

        assertEquals(
            ParsedCommand.Search(BookSearchCriteria(title = "채식주의자", author = "한강")),
            command,
        )
    }

    @Test
    fun `extracts multi word author after marker`() {
        val command = parser.parse("1984 작가 조지 오웰 출판사 민음사 검색해줘", emptyContext())

        assertEquals(
            ParsedCommand.Search(
                BookSearchCriteria(
                    title = "1984",
                    author = "조지 오웰",
                    publisher = "민음사",
                ),
            ),
            command,
        )
    }

    @Test
    fun `parses numeric selection when results exist`() {
        val command = parser.parse("1번 선택", resultsContext())

        assertEquals(ParsedCommand.SelectResult(0), command)
    }

    @Test
    fun `parses korean ordinal selection when results exist`() {
        val command = parser.parse("첫 번째 선택", resultsContext())

        assertEquals(ParsedCommand.SelectResult(0), command)
    }

    @Test
    fun `parses title keyword selection when one title matches`() {
        val command = parser.parse("채식주의자", resultsContext())

        assertEquals(ParsedCommand.SelectByTitle("채식주의자"), command)
    }

    @Test
    fun `search command stays search even when previous results exist`() {
        val command = parser.parse("채식주의자 검색해줘", resultsContext())

        assertEquals(ParsedCommand.Search(BookSearchCriteria(title = "채식주의자")), command)
    }

    private fun emptyContext() = UtteranceContext(hasSearchResults = false, resultTitles = emptyList())

    private fun resultsContext() = UtteranceContext(
        hasSearchResults = true,
        resultTitles = listOf("채식주의자", "소년이 온다"),
    )
}
