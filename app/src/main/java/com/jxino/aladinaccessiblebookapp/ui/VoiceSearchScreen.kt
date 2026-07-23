package com.jxino.aladinaccessiblebookapp.ui

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult
import com.jxino.aladinaccessiblebookapp.domain.CartActionResult
import com.jxino.aladinaccessiblebookapp.domain.RuleBasedWebPageAssistant
import com.jxino.aladinaccessiblebookapp.web.BookWebViewController
import kotlinx.coroutines.flow.SharedFlow
import java.text.NumberFormat
import java.util.Locale

private const val ALADIN_HOME_URL = "https://www.aladin.co.kr/home/welcome.aspx"
private const val ALADIN_HOME_TITLE = "알라딘"

@Composable
fun VoiceSearchScreen(
    uiState: BookSearchUiState,
    screen: AppScreen,
    cartActionRequests: SharedFlow<Unit>,
    hasAudioPermission: Boolean,
    shouldOpenAppSettingsForAudio: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onResultClicked: (BookSearchResult) -> Unit,
    onBackToSearch: () -> Unit,
    onWebViewLoadingChanged: (Boolean) -> Unit,
    onCartActionResult: (CartActionResult) -> Unit,
) {
    val webScreen = screen as? AppScreen.WebView
    val currentUrl = webScreen?.url ?: ALADIN_HOME_URL
    val currentTitle = webScreen?.title ?: ALADIN_HOME_TITLE
    var webView by remember { mutableStateOf<WebView?>(null) }
    var requestedUrl by remember { mutableStateOf<String?>(null) }
    val controller = remember {
        BookWebViewController(
            pageAssistant = RuleBasedWebPageAssistant(),
            onLoadingChanged = onWebViewLoadingChanged,
        )
    }

    LaunchedEffect(cartActionRequests, controller) {
        cartActionRequests.collect {
            controller.clickCartButton(webView, onCartActionResult)
        }
    }

    BackHandler(enabled = webScreen != null) {
        if (controller.canGoBack(webView)) {
            controller.goBack(webView)
        } else {
            onBackToSearch()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White),
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "$currentTitle 알라딘 웹 페이지" },
                factory = { context ->
                    WebView(context).also { view ->
                        webView = view
                        controller.attach(view)
                        requestedUrl = currentUrl
                        controller.load(view, currentUrl)
                    }
                },
                update = { view ->
                    if (requestedUrl != currentUrl) {
                        requestedUrl = currentUrl
                        controller.load(view, currentUrl)
                    }
                },
            )

            if (!hasAudioPermission) {
                PermissionAccessButton(
                    hasAudioPermission = hasAudioPermission,
                    shouldOpenAppSettings = shouldOpenAppSettingsForAudio,
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )
            }

            if (uiState !is BookSearchUiState.Results && uiState != BookSearchUiState.WebViewLoaded) {
                StatusBanner(
                    uiState = uiState,
                    hasAudioPermission = hasAudioPermission,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = 12.dp,
                            top = 12.dp,
                            end = if (hasAudioPermission) 12.dp else 144.dp,
                        )
                        .widthIn(max = 360.dp),
                )
            }

            if (uiState is BookSearchUiState.Results) {
                SearchResultsOverlay(
                    query = uiState.query,
                    results = uiState.results,
                    onResultClicked = onResultClicked,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                )
            }
        }

        PushToTalkButton(
            isListening = uiState == BookSearchUiState.Listening,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp),
        )
    }
}

@Composable
private fun PermissionAccessButton(
    hasAudioPermission: Boolean,
    shouldOpenAppSettings: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusText = if (hasAudioPermission) "마이크 허용됨" else "마이크 권한 필요"
    val actionText = when {
        hasAudioPermission -> "완료"
        shouldOpenAppSettings -> "설정 열기"
        else -> "권한 허용"
    }
    val description = when {
        hasAudioPermission -> "마이크 권한이 허용되어 있습니다."
        shouldOpenAppSettings -> "마이크 권한이 거부되어 있습니다. Android 설정에서 권한을 열려면 누르세요."
        else -> "마이크 권한 요청 버튼. 눌러서 권한 요청을 시작하세요."
    }
    val actionColor = if (hasAudioPermission) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xF7FFFFFF),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = statusText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = actionText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = actionColor,
            )
        }
    }
}

@Composable
private fun StatusBanner(
    uiState: BookSearchUiState,
    hasAudioPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    val message = statusMessage(uiState, hasAudioPermission)
    val isError = uiState is BookSearchUiState.SpeechNotRecognized ||
        uiState is BookSearchUiState.AladinApiUnavailable ||
        uiState is BookSearchUiState.InvalidApiResponse ||
        uiState is BookSearchUiState.NetworkError ||
        uiState == BookSearchUiState.ApiKeyMissing ||
        uiState == BookSearchUiState.InternetUnavailable ||
        uiState == BookSearchUiState.PermissionDenied ||
        uiState == BookSearchUiState.NoResults ||
        uiState == BookSearchUiState.AmbiguousSelection ||
        (uiState is BookSearchUiState.CartActionMessage && uiState.isError)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isError) Color(0xFFFFF1F3) else Color(0xF7FFFFFF),
        tonalElevation = 4.dp,
        shadowElevation = 3.dp,
        modifier = modifier.semantics { contentDescription = message },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (uiState == BookSearchUiState.Searching) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
            }
            Text(
                text = message,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun statusMessage(uiState: BookSearchUiState, hasAudioPermission: Boolean): String =
    when (uiState) {
        BookSearchUiState.Initial -> if (hasAudioPermission) "전자책 이름을 말씀하세요." else "마이크 권한을 허용해 주세요."
        BookSearchUiState.PermissionDenied -> "마이크 권한이 필요합니다."
        BookSearchUiState.Listening -> "듣고 있습니다."
        is BookSearchUiState.SpeechNotRecognized -> uiState.message
        BookSearchUiState.Searching -> "알라딘에서 검색 중입니다."
        is BookSearchUiState.Results -> "\"${uiState.query}\" 전자책 검색 결과입니다."
        BookSearchUiState.NoResults -> "해당 제목의 전자책 검색 결과가 없습니다."
        BookSearchUiState.ApiKeyMissing -> "알라딘 API 키가 설정되지 않았습니다."
        BookSearchUiState.InternetUnavailable -> "인터넷 연결을 확인해 주세요."
        is BookSearchUiState.AladinApiUnavailable -> "알라딘 API에 연결할 수 없습니다. ${uiState.message}"
        is BookSearchUiState.InvalidApiResponse -> "알라딘 API 응답을 처리하지 못했습니다. ${uiState.message}"
        is BookSearchUiState.NetworkError -> "네트워크 오류: ${uiState.message}"
        BookSearchUiState.AmbiguousSelection -> "몇 번을 선택할지 다시 말씀해 주세요."
        BookSearchUiState.WebViewLoading -> "알라딘 페이지를 불러오는 중입니다."
        BookSearchUiState.WebViewLoaded -> "알라딘 페이지 로딩이 완료되었습니다."
        is BookSearchUiState.CartActionMessage -> uiState.message
    }

@Composable
private fun SearchResultsOverlay(
    query: String,
    results: List<BookSearchResult>,
    onResultClicked: (BookSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFAFFFFFF),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = "\"$query\" 검색 결과 ${results.size}개",
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 2.dp),
            ) {
                itemsIndexed(results) { index, result ->
                    SearchResultItem(index, result, onResultClicked)
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    index: Int,
    result: BookSearchResult,
    onResultClicked: (BookSearchResult) -> Unit,
) {
    val price = NumberFormat.getNumberInstance(Locale.KOREA).format(result.priceSales)
    val link = result.link.orEmpty()
    val description = "${index + 1}번. 제목 ${result.title}. 저자 ${result.author}. 출판사 ${result.publisher}. 가격 ${price}원. 알라딘 링크 $link."
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable { onResultClicked(result) },
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "${index + 1}번. ${result.title}",
                fontSize = 19.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "저자 ${result.author.ifBlank { "정보 없음" }}",
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "출판사 ${result.publisher.ifBlank { "정보 없음" }}",
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "가격 ${price}원",
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "링크 ${link.ifBlank { "정보 없음" }}",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PushToTalkButton(
    isListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonColor = if (isListening) MaterialTheme.colorScheme.error else Color(0xFF16212D)
    val ringColor = if (isListening) Color(0xFFFFCDD2) else Color(0xFFB85CFF)
    Button(
        onClick = {
            if (isListening) {
                onStopListening()
            } else {
                onStartListening()
            }
        },
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.semantics {
            contentDescription = if (isListening) {
                "음성 인식 종료 버튼"
            } else {
                "음성 검색 버튼"
            }
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(3.dp, ringColor),
                modifier = Modifier.size(104.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(54.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (isListening) "끝내기" else "말하기",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
