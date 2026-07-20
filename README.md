# Aladin Accessible Book App

Voice-first accessible ebook search app using Aladin Open API and Android WebView.

이 프로젝트는 시각 장애인이 음성으로 전자책을 검색하고, 알라딘 Open API 결과를 음성 안내로 확인한 뒤, 선택한 전자책의 알라딘 상품 페이지를 앱 내부 WebView에서 열 수 있도록 돕는 Android 전용 MVP입니다.

## 주요 기능

- Android Native, Kotlin, Jetpack Compose 기반 UI
- Android `SpeechRecognizer` 기반 push-to-talk 음성 검색
- Android `TextToSpeech` 기반 검색 결과 및 오류 안내
- 알라딘 Open API `ItemSearch.aspx` 전자책 검색
- `mallType == EBOOK` 응답 검증 필터링
- 검색 결과 터치 선택 및 음성 번호/제목 선택
- 앱 내부 WebView로 알라딘 상품 페이지 열기
- 향후 AI 구현체 교체를 위한 도메인 interface 구조

## 현재 구현 완료 범위

- Android 전용 앱 기본 구조 구현
- Jetpack Compose 기반 UI 구현
- 마이크 권한 요청 구조 구현
- 화면 하단 중앙 부근의 대형 원형 push-to-talk 음성 버튼 구현
- Android SpeechRecognizer 기반 음성 인식 구현
- AI 없는 규칙 기반 사용자 발화 파싱 구현
- `UserUtteranceParser`, `ParsedCommand`, `UtteranceContext` 기반 확장 구조 구현
- `ResultAnnouncer` 기반 검색 결과 안내 확장 구조 구현
- `WebPageAssistant` 기반 WebView 상태 분석 확장 구조 구현
- 알라딘 Open API `ItemSearch.aspx` 전자책 검색 연동 구조 구현
- `SearchTarget=eBook`, `Output=JS`, `Version=20131101` 기반 요청 구현
- `mallType == EBOOK` 응답 필터링 구현
- API 키 미설정 시 “알라딘 API 키가 설정되지 않았습니다” 안내 처리 구현
- 전자책 검색 결과 상위 5개 표시
- TTS 검색 결과 안내
- 검색 결과 터치/음성 선택 구조 구현
- 앱 내부 WebView에서 알라딘 전자책 상품 페이지 열기
- 결제 자동화는 구현하지 않음

## 아직 구현하지 않은 범위

- 생성형 AI/LLM 기반 자연어 의도 분석 구현체
- AI 기반 검색 결과 자동 추천/비교 설명
- AI 기반 WebView 화면 상태 요약
- 복잡한 조건 검색
- 삼성페이 선택 자동화
- 결제하기 버튼 자동 클릭
- 실제 결제 완료 검증
- 사용자 계정/주문 내역 관리
- 판매자/PG 직접 연동
- iOS 지원

## 알라딘 API 키 설정

실제 API 키는 코드에 하드코딩하지 않습니다. 루트 디렉터리에 `local.properties` 파일을 만들고 다음 값을 설정합니다.

```properties
ALADIN_TTB_KEY=your_aladin_ttb_key_here
```

`local.properties`는 `.gitignore`에 포함되어 Git에 올라가지 않습니다. 예시는 `local.properties.example`을 참고하세요. 키가 비어 있거나 누락되면 앱은 mock 결과를 사용하지 않고 API 호출도 시도하지 않으며, 사용자에게 “알라딘 API 키가 설정되지 않았습니다”라고 안내합니다.

## 알라딘 Open API 사용 파라미터

상품 검색 API를 사용합니다.

- 기본 엔드포인트: `https://www.aladin.co.kr/ttb/api/ItemSearch.aspx`
- 문서 호환 엔드포인트: `http://www.aladin.co.kr/ttb/api/ItemSearch.aspx`
- `ttbkey=BuildConfig.ALADIN_TTB_KEY`
- `Query=정제된 검색어`
- `QueryType=Keyword`
- `SearchTarget=eBook`
- `Start=1`
- `MaxResults=5`
- `Sort=Accuracy`
- `Cover=MidBig`
- `Output=JS`
- `Version=20131101`
- `outofStockfilter=1`
- `OptResult=fileFormatList`

응답에서는 `item[].title`, `author`, `publisher`, `priceSales`, `priceStandard`, `link`, `isbn`, `isbn13`, `mallType`, `cover`, `adult`, `fixedPrice`, `subInfo.fileFormatList`를 매핑합니다. `SearchTarget=eBook`으로 요청하더라도 앱 내부에서 `mallType == EBOOK` 필터를 한 번 더 적용합니다.

## 실행 방법

1. Android Studio에서 프로젝트를 엽니다.
2. 루트에 `local.properties`를 만들고 `ALADIN_TTB_KEY`를 설정합니다.
3. Android 기기 또는 에뮬레이터에서 실행합니다.
4. 마이크 권한을 허용합니다.
5. 하단 중앙의 큰 원형 버튼을 누르고 있는 동안 “채식주의자 찾아줘”처럼 말합니다.
6. 검색 결과가 나오면 “1번 선택”이라고 말하거나 결과 항목을 터치합니다.

## 실기기 테스트 방법

에뮬레이터에서는 PC 마이크 입력이나 Google 음성 인식 서비스가 불안정할 수 있습니다. 음성 인식 동작을 정확히 확인하려면 실제 Android 기기에서 테스트하는 것을 권장합니다.

### Android Studio로 실기기 실행

1. Android 기기에서 개발자 옵션을 켭니다.
2. `USB 디버깅`을 켭니다.
3. USB 케이블로 기기와 개발 PC를 연결합니다.
4. 기기 화면에 USB 디버깅 허용 팝업이 뜨면 허용합니다.
5. Android Studio 상단 실행 대상에서 연결된 실제 기기를 선택합니다.
6. 실행 구성은 `app`으로 둔 뒤 초록색 실행 버튼을 누릅니다.
7. 앱이 설치되면 우측 상단의 마이크 권한 버튼을 눌러 마이크 권한을 허용합니다.
8. 하단 중앙의 큰 `말하기` 버튼을 누르고 있는 동안 “채식주의자 찾아줘”처럼 말한 뒤 손을 뗍니다.

실제 알라딘 검색까지 확인하려면 `local.properties`의 `ALADIN_TTB_KEY`가 설정되어 있어야 합니다. 음성 인식 자체만 확인하는 경우에는 API 키가 없어도 됩니다.

### APK로 일반 테스터에게 전달하는 경우

일반 테스터는 Android Studio 없이 APK 파일만 설치해서 앱 화면과 음성 인식을 확인할 수 있습니다.

GitHub Releases에 APK가 올라와 있는 경우:

1. Android 기기에서 이 저장소의 Releases 페이지를 엽니다.
   - `https://github.com/Jxino/aladin-accessible-book-app/releases`
2. 최신 릴리스를 선택합니다.
3. `Assets` 영역을 펼칩니다.
4. `.apk`로 끝나는 파일을 누릅니다.
5. 브라우저가 다운로드 경고를 표시하면 APK 파일 다운로드를 계속합니다.
6. 다운로드가 끝나면 알림 또는 파일 관리자에서 APK 파일을 엽니다.
7. Android가 “알 수 없는 앱 설치” 허용을 요구하면, 다운로드에 사용한 브라우저 또는 파일 관리자에 대해 한 번만 허용합니다.
8. 설치 화면에서 `설치`를 누릅니다.
9. 설치가 끝나면 `열기`를 누르거나 앱 목록에서 `Aladin Accessible Book`을 실행합니다.
10. 앱에서 마이크 권한을 허용합니다.
11. 하단 중앙의 큰 `말하기` 버튼을 누르고 있는 동안 검색어를 말합니다.

GitHub Releases에 APK가 없는 경우:

1. 일반 테스터는 GitHub 화면만으로 앱을 바로 설치할 수 없습니다.
2. 개발자가 Android Studio에서 APK를 빌드한 뒤 파일을 직접 전달하거나, GitHub Releases에 APK를 업로드해야 합니다.
3. APK 파일을 받은 뒤에는 위의 6번부터 진행합니다.

개발자가 APK를 직접 빌드하는 명령:

```bash
./gradlew assembleDebug
```

빌드된 APK 위치:

```text
app/build/outputs/apk/debug/app-debug.apk
```

현재 MVP는 API 키를 `local.properties`에서 빌드 시점에 읽어 `BuildConfig.ALADIN_TTB_KEY`로 포함합니다. 따라서 APK로 실제 알라딘 검색까지 테스트하려면 API 키가 포함된 빌드가 필요합니다. API 키가 포함되지 않은 APK에서도 음성 인식 성공 여부는 확인할 수 있습니다.

### API 키 없이 음성 인식만 확인하는 방법

API 키가 없어도 음성 인식이 성공했는지 구분할 수 있습니다.

1. 앱을 실행합니다.
2. 마이크 권한을 허용합니다.
3. 하단 중앙의 큰 `말하기` 버튼을 누른 채 “1984 찾아줘” 또는 “채식주의자 찾아줘”라고 말합니다.
4. 말을 끝낸 뒤 버튼에서 손을 뗍니다.

결과 해석:

- “알라딘 API 키가 설정되지 않았습니다”가 표시되거나 음성으로 안내되면 음성 인식과 명령 파싱은 성공한 것입니다.
- “음성을 인식하지 못했습니다”가 표시되면 마이크 입력, Android 음성 인식 서비스, 발화 시간, 또는 기기 권한 상태를 확인해야 합니다.
- 아무 반응이 없으면 마이크 권한과 버튼을 누르고 있는 방식(push-to-talk)을 먼저 확인해야 합니다.

## 테스트

```bash
./gradlew test
```

현재 테스트는 규칙 기반 발화 파싱, 번호/제목 선택, `1984 찾아줘` 검색어 처리, API 키 없음 상태 처리, `mallType == EBOOK` 필터링, 검색 결과 안내 문구, ViewModel 상태 전환을 포함합니다.

## 접근성 고려사항

- 주요 버튼과 검색 결과에 `contentDescription`을 설정했습니다.
- 음성 검색 버튼은 우측 하단의 작은 FAB가 아니라 화면 하단 중앙 부근의 180dp 원형 버튼입니다.
- 검색 결과 텍스트는 큰 글씨와 충분한 줄 높이로 표시합니다.
- TalkBack이 결과 번호, 제목, 저자, 출판사, 가격을 자연스럽게 읽을 수 있도록 결과 항목 설명을 구성했습니다.
- TTS로 검색 결과, 오류, WebView 로딩 상태를 안내합니다.

## AI/LLM을 현재 사용하지 않는 이유

이번 MVP는 외부 LLM, 생성형 AI API, AI 의도 분석 모듈을 사용하지 않습니다. 검색과 선택 동작은 예측 가능해야 하며, 알라딘 API 결과에 없는 책을 생성하거나 임의로 추천하지 않아야 하기 때문입니다. 현재 구현은 규칙 기반이며, 향후 AI 구현체를 추가하더라도 앱 도메인 타입을 반환하도록 제한합니다.

## 향후 AI 확장을 위한 interface 설계

- `UserUtteranceParser`: 음성 인식 텍스트와 `UtteranceContext`를 받아 `ParsedCommand`를 반환합니다.
- `ResultAnnouncer`: 알라딘 API에서 받은 `BookSearchResult` 목록을 기반으로 안내 문구를 생성합니다.
- `WebPageAssistant`: WebView URL과 제목을 받아 `WebPageState`를 반환합니다.

향후 `AiUserUtteranceParser`, `AiResultAnnouncer`, AI 기반 `WebPageAssistant`를 추가할 수 있지만, 결과 생성과 선택은 반드시 실제 API 결과를 기반으로 해야 합니다.

## 결제 자동화 제외 이유

이 버전은 선택한 전자책의 알라딘 상품 페이지 또는 검색 페이지까지만 이동합니다. 결제 실행, 결제하기 버튼 자동 클릭, 삼성페이 자동 선택, 좌표 기반 클릭, 화면 이미지 인식 방식은 구현하지 않았습니다. 결제 단계는 민감도가 높고 사용자 확인이 필수이므로 MVP 명령 모델에 포함하지 않았습니다.

## 향후 확장 계획

- AI 기반 자연어 의도 분석 구현체 추가
- 검색 결과 비교 설명 및 요약 안내
- WebView 화면 상태 요약 안내
- 상세 조건 검색
- 사용자 설정 가능한 음성 안내 속도와 안내 범위
- 접근성 사용자 테스트 기반 UI 조정
