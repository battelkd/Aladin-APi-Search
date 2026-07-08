package com.jxino.aladinaccessiblebookapp.data

import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.URLEncoder

sealed class BookSearchError {
    data object ApiKeyMissing : BookSearchError()
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
            val response = try {
                httpsApiService.searchEbooks(ttbKey = ttbKey, query = query)
            } catch (httpsFailure: IOException) {
                httpApiService.searchEbooks(ttbKey = ttbKey, query = query)
            }
            BookSearchResponse.Success(response.item.toBookSearchResults().take(5))
        } catch (exception: Exception) {
            BookSearchResponse.Failure(BookSearchError.Network(exception.message ?: "알 수 없는 오류"))
        }
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

fun List<AladinItem>.toBookSearchResults(): List<BookSearchResult> =
    map { item ->
        BookSearchResult(
            title = item.title.orEmpty(),
            author = item.author.orEmpty(),
            publisher = item.publisher.orEmpty(),
            priceSales = item.priceSales ?: 0,
            priceStandard = item.priceStandard ?: 0,
            link = item.link,
            isbn = item.isbn,
            isbn13 = item.isbn13,
            mallType = item.mallType,
            cover = item.cover,
            isAdult = item.adult ?: false,
            isFixedPrice = item.fixedPrice ?: false,
            fileFormats = item.subInfo?.fileFormatList.orEmpty(),
        )
    }.filter { it.isEbook }

fun buildAladinSearchFallbackUrl(query: String): String {
    val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
    return "https://www.aladin.co.kr/search/wsearchresult.aspx?SearchTarget=eBook&SearchWord=$encoded"
}
