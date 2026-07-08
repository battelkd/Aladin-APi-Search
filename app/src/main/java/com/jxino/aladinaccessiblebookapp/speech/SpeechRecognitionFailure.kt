package com.jxino.aladinaccessiblebookapp.speech

import android.speech.SpeechRecognizer

data class SpeechRecognitionFailure(
    val code: Int,
    val userMessage: String,
)

fun speechRecognitionFailureFor(code: Int): SpeechRecognitionFailure {
    val message = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "마이크 오디오 입력에 문제가 있습니다. 기기 마이크 설정을 확인해 주세요."
        SpeechRecognizer.ERROR_CLIENT -> "음성 인식이 너무 빨리 종료되었습니다. 버튼을 누른 채 말한 뒤 손을 떼 주세요."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 없습니다. 마이크 권한을 허용해 주세요."
        SpeechRecognizer.ERROR_NETWORK -> "음성 인식 네트워크 오류가 발생했습니다."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "음성 인식 네트워크 응답 시간이 초과되었습니다."
        SpeechRecognizer.ERROR_NO_MATCH -> "음성을 텍스트로 인식하지 못했습니다. 조금 더 크게 다시 말씀해 주세요."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기가 아직 준비되지 않았습니다. 잠시 후 다시 시도해 주세요."
        SpeechRecognizer.ERROR_SERVER -> "음성 인식 서비스 오류가 발생했습니다. Google 음성 인식 서비스를 확인해 주세요."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말소리를 감지하지 못했습니다. 버튼을 누른 채 말해 주세요."
        else -> "음성 인식 오류가 발생했습니다. 오류 코드 $code."
    }
    return SpeechRecognitionFailure(code = code, userMessage = message)
}
