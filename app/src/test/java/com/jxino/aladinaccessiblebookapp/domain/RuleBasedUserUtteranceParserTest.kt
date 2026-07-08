package com.jxino.aladinaccessiblebookapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RuleBasedUserUtteranceParserTest {
    private val parser = RuleBasedUserUtteranceParser()

    @Test
    fun `parses search query without results`() {
        val command = parser.parse("채식주의자 찾아줘", emptyContext())

        assertEquals(ParsedCommand.Search("채식주의자"), command)
    }

    @Test
    fun `treats numeric utterance as search query when there are no results`() {
        val command = parser.parse("1984 찾아줘", emptyContext())

        assertEquals(ParsedCommand.Search("1984"), command)
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

    private fun emptyContext() = UtteranceContext(hasSearchResults = false, resultTitles = emptyList())

    private fun resultsContext() = UtteranceContext(
        hasSearchResults = true,
        resultTitles = listOf("채식주의자", "소년이 온다"),
    )
}
