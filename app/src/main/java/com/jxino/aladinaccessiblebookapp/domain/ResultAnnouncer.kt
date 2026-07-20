package com.jxino.aladinaccessiblebookapp.domain

import com.jxino.aladinaccessiblebookapp.ui.BookSearchUiState
import java.text.NumberFormat
import java.util.Locale

interface ResultAnnouncer {
    fun buildSearchResultsAnnouncement(results: List<BookSearchResult>): String
    fun buildErrorAnnouncement(state: BookSearchUiState): String?
}

class BasicResultAnnouncer : ResultAnnouncer {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun buildSearchResultsAnnouncement(results: List<BookSearchResult>): String {
        if (results.isEmpty()) return "해당 제목의 전자책 검색 결과가 없습니다. 다른 책 제목으로 다시 말씀해 주세요."
        return buildString {
            append("전자책 검색 결과 ${results.size}건을 찾았습니다. ")
            results.forEachIndexed { index, result ->
                append("${index + 1}번. ")
                append("제목 ${result.title}. ")
                append("저자 ${result.author.ifBlank { "정보 없음" }}. ")
                append("출판사 ${result.publisher.ifBlank { "정보 없음" }}. ")
                append("가격 ${numberFormat.format(result.priceSales)}원. ")
            }
        }.trim()
    }

    override fun buildErrorAnnouncement(state: BookSearchUiState): String? = when (state) {
        BookSearchUiState.ApiKeyMissing -> "알라딘 API 키가 설정되지 않았습니다."
        BookSearchUiState.NoResults -> "해당 제목의 전자책 검색 결과가 없습니다. 다른 책 제목으로 다시 말씀해 주세요."
        BookSearchUiState.InternetUnavailable -> "인터넷 연결을 확인해 주세요."
        is BookSearchUiState.AladinApiUnavailable -> "알라딘 API에 연결할 수 없습니다. ${state.message}"
        is BookSearchUiState.InvalidApiResponse -> "알라딘 API 응답을 처리하지 못했습니다. ${state.message}"
        is BookSearchUiState.NetworkError -> "네트워크 오류가 발생했습니다. ${state.message}"
        is BookSearchUiState.SpeechNotRecognized -> state.message
        BookSearchUiState.AmbiguousSelection -> "몇 번을 선택할지 다시 말씀해 주세요."
        else -> null
    }
}
