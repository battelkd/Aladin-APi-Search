package com.jxino.aladinaccessiblebookapp.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VoiceSearchScreen(
    uiState: BookSearchUiState,
    hasAudioPermission: Boolean,
    shouldOpenAppSettingsForAudio: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onResultClicked: (BookSearchResult) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
    ) {
        PermissionAccessButton(
            hasAudioPermission = hasAudioPermission,
            shouldOpenAppSettings = shouldOpenAppSettingsForAudio,
            onClick = onRequestPermission,
            modifier = Modifier
                .align(Alignment.TopEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 76.dp, bottom = 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusText(uiState, hasAudioPermission)
            Spacer(Modifier.height(20.dp))
            if (uiState is BookSearchUiState.Results) {
                SearchResultsList(uiState.results, onResultClicked)
            }
        }

        PushToTalkButton(
            isListening = uiState == BookSearchUiState.Listening,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .semantics { contentDescription = description },
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
private fun StatusText(uiState: BookSearchUiState, hasAudioPermission: Boolean) {
    val message = when (uiState) {
        BookSearchUiState.Initial -> if (hasAudioPermission) "음성 버튼을 한 번 누르고 전자책 이름을 말씀하세요." else "마이크 권한을 허용해 주세요."
        BookSearchUiState.PermissionDenied -> "마이크 권한이 필요합니다."
        BookSearchUiState.Listening -> "듣고 있습니다. 다 말했으면 버튼을 한 번 더 누르거나 잠시 기다리세요."
        is BookSearchUiState.SpeechNotRecognized -> uiState.message
        BookSearchUiState.Searching -> "알라딘에서 전자책을 검색 중입니다."
        is BookSearchUiState.Results -> "\"${uiState.query}\" 전자책 검색 결과입니다."
        BookSearchUiState.NoResults -> "해당 제목의 전자책 검색 결과가 없습니다. 다른 책 제목으로 다시 말씀해 주세요."
        BookSearchUiState.ApiKeyMissing -> "알라딘 API 키가 설정되지 않았습니다."
        BookSearchUiState.InternetUnavailable -> "인터넷 연결을 확인해 주세요."
        is BookSearchUiState.AladinApiUnavailable -> "알라딘 API에 연결할 수 없습니다. ${uiState.message}"
        is BookSearchUiState.InvalidApiResponse -> "알라딘 API 응답을 처리하지 못했습니다. ${uiState.message}"
        is BookSearchUiState.NetworkError -> "네트워크 오류: ${uiState.message}"
        BookSearchUiState.AmbiguousSelection -> "몇 번을 선택할지 다시 말씀해 주세요."
        BookSearchUiState.WebViewLoading -> "알라딘 페이지를 불러오는 중입니다."
        BookSearchUiState.WebViewLoaded -> "알라딘 페이지 로딩이 완료되었습니다."
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState == BookSearchUiState.Searching) {
            CircularProgressIndicator(modifier = Modifier.size(34.dp).padding(end = 8.dp))
        }
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = message },
        )
    }
}

@Composable
private fun SearchResultsList(
    results: List<BookSearchResult>,
    onResultClicked: (BookSearchResult) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(results) { index, result ->
            SearchResultItem(index, result, onResultClicked)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable { onResultClicked(result) },
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("${index + 1}번. ${result.title}", fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("저자 ${result.author.ifBlank { "정보 없음" }}", fontSize = 21.sp, lineHeight = 28.sp)
            Text("출판사 ${result.publisher.ifBlank { "정보 없음" }}", fontSize = 21.sp, lineHeight = 28.sp)
            Text("가격 ${price}원", fontSize = 21.sp, lineHeight = 28.sp)
            Text("알라딘 링크 ${link.ifBlank { "정보 없음" }}", fontSize = 18.sp, lineHeight = 24.sp)
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
    Button(
        onClick = {
            if (isListening) {
                onStopListening()
            } else {
                onStartListening()
            }
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .size(180.dp)
            .semantics {
                contentDescription = if (isListening) {
                    "음성 인식 종료 버튼. 다 말했으면 누르세요."
                } else {
                    "음성 검색 버튼. 누르면 음성 인식을 시작합니다."
                }
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(62.dp))
            Text(if (isListening) "끝내기" else "말하기", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}
