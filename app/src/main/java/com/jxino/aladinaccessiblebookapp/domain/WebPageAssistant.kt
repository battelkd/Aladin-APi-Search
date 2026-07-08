package com.jxino.aladinaccessiblebookapp.domain

sealed class WebPageState {
    data object Unknown : WebPageState()
    data object ProductPage : WebPageState()
    data object LoginRequired : WebPageState()
    data object PaymentMethodSelection : WebPageState()
    data object PaymentConfirmation : WebPageState()
}

interface WebPageAssistant {
    fun analyzePageState(pageUrl: String, pageTitle: String?): WebPageState
}

class RuleBasedWebPageAssistant : WebPageAssistant {
    override fun analyzePageState(pageUrl: String, pageTitle: String?): WebPageState {
        val source = "${pageUrl.lowercase()} ${(pageTitle ?: "").lowercase()}"
        return when {
            "login" in source || "로그인" in source -> WebPageState.LoginRequired
            "payment" in source || "결제수단" in source -> WebPageState.PaymentMethodSelection
            "confirm" in source || "결제확인" in source -> WebPageState.PaymentConfirmation
            "itemid" in source || "itemdetail" in source || "shop/wproduct.aspx" in source -> WebPageState.ProductPage
            else -> WebPageState.Unknown
        }
    }
}
