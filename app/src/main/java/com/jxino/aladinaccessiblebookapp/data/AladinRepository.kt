package com.jxino.aladinaccessiblebookapp.data

import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URLEncoder

sealed class BookSearchError {
    data object ApiKeyMissing : BookSearchError()
    data object InternetUnavailable : BookSearchError()
    data class AladinApiUnavailable(val message: String) : BookSearchError()
    data class InvalidApiResponse(val message: String) : BookSearchError()
    data class Network(val message: String) : BookSearchError()
}

sealed class BookSearchResponse {
    data class Success(val results: List<BookSearchResult>) : BookSearchResponse()
    data class Failure(val error: BookSearchError) : BookSearchResponse()
}

interface BookRepository {
    suspend fun searchEbooks(query: String): BookSearchResponse
}

class AladinRepository(
    private val ttbKey: String,
    private val httpsApiService: AladinApiService = createService("https://www.aladin.co.kr/"),
    private val httpApiService: AladinApiService = createService("http://www.aladin.co.kr/"),
) : BookRepository {
    override suspend fun searchEbooks(query: String): BookSearchResponse {
        if (ttbKey.isBlank()) {
            return BookSearchResponse.Failure(BookSearchError.ApiKeyMissing)
        }

        return try {
            val candidates = query.toSearchQueryCandidates()
            candidates.forEachIndexed { index, candidate ->
                val response = searchOnce(candidate)
                response.apiErrorMessage()?.let { message ->
                    return BookSearchResponse.Failure(BookSearchError.AladinApiUnavailable(message))
                }
                val results = response.item.toBookSearchResults()
                if (results.isNotEmpty() || index == candidates.lastIndex) {
                    return BookSearchResponse.Success(results)
                }
            }
            BookSearchResponse.Success(emptyList())
        } catch (exception: Exception) {
            BookSearchResponse.Failure(exception.toBookSearchError())
        }
    }

    private suspend fun searchOnce(query: String): AladinSearchResponse =
        try {
            httpsApiService.searchEbooks(ttbKey = ttbKey, query = query)
        } catch (httpsFailure: IOException) {
            httpApiService.searchEbooks(ttbKey = ttbKey, query = query)
        }

    companion object {
        fun createService(baseUrl: String): AladinApiService {
            val client = OkHttpClient.Builder().build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AladinApiService::class.java)
        }
    }
}

private fun Exception.toBookSearchError(): BookSearchError =
    when (this) {
        is UnknownHostException,
        is ConnectException,
        is SocketTimeoutException,
        is SocketException -> BookSearchError.InternetUnavailable
        is HttpException -> BookSearchError.AladinApiUnavailable("알라딘 API 응답 오류 ${code()}번")
        is JsonParseException,
        is IllegalStateException -> BookSearchError.InvalidApiResponse("알라딘 API 응답 형식이 예상과 다릅니다.")
        is IOException -> BookSearchError.Network(message ?: "네트워크 요청 중 오류가 발생했습니다.")
        else -> BookSearchError.Network(message ?: "알 수 없는 오류")
    }

private fun AladinSearchResponse.apiErrorMessage(): String? {
    val code = errorCode?.takeIf { it.isNotBlank() }
    val message = errorMessage?.takeIf { it.isNotBlank() }
    return when {
        code != null && message != null -> "$message ($code)"
        message != null -> message
        code != null -> "알라딘 API 오류 코드 $code"
        else -> null
    }
}

private fun String.toSearchQueryCandidates(): List<String> {
    val trimmed = trim()
    val compactKorean = trimmed.compactKoreanSpacesCandidate()
    return listOfNotNull(trimmed, compactKorean)
        .filter { it.isNotBlank() }
        .distinct()
}

private fun String.compactKoreanSpacesCandidate(): String? {
    if (!contains(" ")) return null
    val tokens = split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.size !in 2..4) return null
    if (!tokens.all { token -> token.all { it.isHangulSyllable() } }) return null
    return tokens.joinToString("")
}

private fun Char.isHangulSyllable(): Boolean =
    this in '\uAC00'..'\uD7A3'

fun List<AladinItem>.toBookSearchResults(): List<BookSearchResult> =
    map { item ->
        BookSearchResult(
            title = item.title.orEmpty(),
            author = item.author.orEmpty(),
            publisher = item.publisher.orEmpty(),
            priceSales = item.priceSales ?: 0,
            priceStandard = item.priceStandard ?: 0,
            link = item.link?.replace("&amp;", "&"),
            isbn = item.isbn,
            isbn13 = item.isbn13,
            mallType = item.mallType,
            cover = item.cover,
            isAdult = item.adult ?: false,
            isFixedPrice = item.fixedPrice ?: false,
            fileFormats = item.subInfo?.fileFormatList.toFileFormatNames(),
        )
    }.filter { it.isEbook }

private fun List<JsonElement>?.toFileFormatNames(): List<String> =
    orEmpty()
        .mapNotNull { element ->
            when {
                element.isJsonPrimitive -> element.asString
                element.isJsonObject -> {
                    val value = element.asJsonObject
                    value["fileFormatName"]?.takeIf { it.isJsonPrimitive }?.asString
                        ?: value["fileFormatCode"]?.takeIf { it.isJsonPrimitive }?.asString
                        ?: value["fileType"]?.takeIf { it.isJsonPrimitive }?.asString
                }
                else -> null
            }
        }
        .filter { it.isNotBlank() }

fun buildAladinSearchFallbackUrl(query: String): String {
    val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
    return "https://www.aladin.co.kr/search/wsearchresult.aspx?SearchTarget=eBook&SearchWord=$encoded"
}
