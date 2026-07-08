package com.jxino.aladinaccessiblebookapp.ui

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jxino.aladinaccessiblebookapp.domain.RuleBasedWebPageAssistant
import com.jxino.aladinaccessiblebookapp.web.BookWebViewController

@Composable
fun WebViewScreen(
    url: String,
    title: String,
    onBackToSearch: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    val controller = remember {
        BookWebViewController(
            pageAssistant = RuleBasedWebPageAssistant(),
            onLoadingChanged = onLoadingChanged,
        )
    }

    BackHandler {
        if (controller.canGoBack(webView)) {
            controller.goBack(webView)
        } else {
            onBackToSearch()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "$title 알라딘 웹페이지" },
            factory = { context ->
                WebView(context).also {
                    webView = it
                    controller.attach(it)
                    controller.load(it, url)
                }
            },
            update = { view ->
                if (view.url != url) {
                    controller.load(view, url)
                }
            },
        )
        FloatingActionButton(
            onClick = onBackToSearch,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .semantics { contentDescription = "검색 화면으로 돌아가기" },
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
        }
    }
}
