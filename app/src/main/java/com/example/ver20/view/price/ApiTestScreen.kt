// ApiTestScreen.kt - API 테스트 UI 화면

package com.example.ver20.view.price

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.dao.dataclass.ApiKeyService
import com.example.ver20.dao.binance.BinanceApiTester
import com.example.ver20.dao.dataclass.ApiKeyData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTestScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val apiKeyService = remember { ApiKeyService(context) }
    val binanceApiTester = remember { BinanceApiTester() }
    val coroutineScope = rememberCoroutineScope()

    // UI 상태
    var apiKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var showSecretKey by remember { mutableStateOf(false) }
    var isTestnet by remember { mutableStateOf(true) }
    var testResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var currentTest by remember { mutableStateOf("") }

    // 저장된 API 키 로드
    LaunchedEffect(Unit) {
        apiKeyService.getApiKeys()?.let { savedKeys ->
            apiKey = savedKeys.apiKey
            secretKey = savedKeys.secretKey
            isTestnet = savedKeys.isTestnet
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🔑 바이낸스 API 테스트",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // API 키 설정 카드
            ApiKeyInputCard(
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                secretKey = secretKey,
                onSecretKeyChange = { secretKey = it },
                showSecretKey = showSecretKey,
                onToggleSecretKey = { showSecretKey = !showSecretKey },
                isTestnet = isTestnet,
                onTestnetChange = { isTestnet = it },
                onSave = {
                    val success = apiKeyService.saveApiKeys(apiKey, secretKey, isTestnet)
                    testResults = testResults + if (success) {
                        "✅ API 키가 안전하게 저장되었습니다"
                    } else {
                        "❌ API 키 저장에 실패했습니다"
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 테스트 버튼들 카드
            TestButtonsCard(
                isLoading = isLoading,
                currentTest = currentTest,
                onServerTest = {
                    coroutineScope.launch {
                        isLoading = true
                        currentTest = "서버 연결"
                        testResults = testResults + "🔄 바이낸스 서버 연결 테스트 중..."

                        val (success, message) = binanceApiTester.testServerConnection()
                        testResults = testResults + if (success) {
                            "✅ 서버 연결 성공: $message"
                        } else {
                            "❌ 서버 연결 실패: $message"
                        }

                        isLoading = false
                        currentTest = ""
                    }
                },
                onPriceTest = {
                    coroutineScope.launch {
                        isLoading = true
                        currentTest = "가격 조회"
                        testResults = testResults + "🔄 BTC 가격 조회 테스트 중..."

                        val (success, message) = binanceApiTester.testPriceQuery("BTCUSDT")
                        testResults = testResults + if (success) {
                            "✅ 가격 조회 성공: $message"
                        } else {
                            "❌ 가격 조회 실패: $message"
                        }

                        isLoading = false
                        currentTest = ""
                    }
                },
                onAuthTest = {
                    if (apiKey.isNotEmpty() && secretKey.isNotEmpty()) {
                        coroutineScope.launch {
                            isLoading = true
                            currentTest = "API 인증"
                            testResults = testResults + "🔄 API 키 인증 테스트 중..."

                            val apiKeyData = ApiKeyData(apiKey, secretKey, isTestnet)
                            val (success, message) = binanceApiTester.testApiAuthentication(apiKeyData)
                            testResults = testResults + if (success) {
                                "✅ API 인증 성공: $message 🎉"
                            } else {
                                "❌ API 인증 실패: $message"
                            }

                            isLoading = false
                            currentTest = ""
                        }
                    } else {
                        testResults = testResults + "⚠️ API 키와 Secret 키를 먼저 입력해주세요"
                    }
                },
                onFullTest = {
                    if (apiKey.isNotEmpty() && secretKey.isNotEmpty()) {
                        coroutineScope.launch {
                            isLoading = true
                            currentTest = "전체 테스트"
                            testResults = testResults + "🚀 전체 테스트 시작..."

                            val apiKeyData = ApiKeyData(apiKey, secretKey, isTestnet)
                            val results = binanceApiTester.runFullTest(apiKeyData)

                            results.forEach { (testName, result) ->
                                val (success, message) = result
                                testResults = testResults + if (success) {
                                    "✅ $testName: $message"
                                } else {
                                    "❌ $testName: $message"
                                }
                            }

                            testResults = testResults + "🏁 전체 테스트 완료!"

                            isLoading = false
                            currentTest = ""
                        }
                    } else {
                        testResults = testResults + "⚠️ API 키를 먼저 입력해주세요"
                    }
                },
                hasApiKeys = apiKey.isNotEmpty() && secretKey.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 테스트 결과 카드
            if (testResults.isNotEmpty()) {
                TestResultsCard(
                    results = testResults,
                    onClear = { testResults = emptyList() }
                )
            }

            // 로딩 인디케이터
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE1F5FE)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "🔄 $currentTest 진행 중...",
                            fontSize = 14.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApiKeyInputCard(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    secretKey: String,
    onSecretKeyChange: (String) -> Unit,
    showSecretKey: Boolean,
    onToggleSecretKey: () -> Unit,
    isTestnet: Boolean,
    onTestnetChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "🔑 API 키 설정",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                placeholder = { Text("발급받은 API Key 입력") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = secretKey,
                onValueChange = onSecretKeyChange,
                label = { Text("Secret Key") },
                placeholder = { Text("발급받은 Secret Key 입력") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onToggleSecretKey) {
                        Icon(
                            if (showSecretKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (showSecretKey) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = isTestnet,
                    onCheckedChange = onTestnetChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isTestnet) "🧪 테스트넷 모드 (안전)" else "🔴 메인넷 모드 (실제)",
                    fontSize = 14.sp,
                    color = if (isTestnet) Color(0xFFFF9800) else Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey.isNotEmpty() && secretKey.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("API 키 저장")
            }
        }
    }
}

@Composable
fun TestButtonsCard(
    isLoading: Boolean,
    currentTest: String,
    onServerTest: () -> Unit,
    onPriceTest: () -> Unit,
    onAuthTest: () -> Unit,
    onFullTest: () -> Unit,
    hasApiKeys: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "🧪 API 연결 테스트",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7B1FA2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onServerTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("1️⃣ 서버 연결 테스트")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPriceTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("2️⃣ 가격 조회 테스트")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAuthTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && hasApiKeys,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("3️⃣ API 인증 테스트")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onFullTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && hasApiKeys,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚀 전체 테스트 실행")
            }
        }
    }
}

@Composable
fun TestResultsCard(
    results: List<String>,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📋 테스트 결과",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )

                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "결과 지우기",
                        tint = Color(0xFFE65100)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            results.forEach { result ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "•",
                        fontSize = 14.sp,
                        color = when {
                            result.contains("✅") -> Color(0xFF4CAF50)
                            result.contains("❌") -> Color(0xFFF44336)
                            result.contains("🔄") -> Color(0xFF2196F3)
                            result.contains("🚀") || result.contains("🏁") -> Color(0xFF9C27B0)
                            else -> Color(0xFF666666)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        result,
                        fontSize = 14.sp,
                        color = when {
                            result.contains("✅") -> Color(0xFF4CAF50)
                            result.contains("❌") -> Color(0xFFF44336)
                            result.contains("🔄") -> Color(0xFF2196F3)
                            result.contains("🚀") || result.contains("🏁") -> Color(0xFF9C27B0)
                            else -> Color(0xFF666666)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (results.any { it.contains("✅ API 인증 성공") }) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "🎉 축하합니다!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            "바이낸스 API 연동이 성공적으로 완료되었습니다.\n이제 실제 자동매매 기능을 구현할 수 있습니다!",
                            fontSize = 14.sp,
                            color = Color(0xFF388E3C)
                        )
                    }
                }
            }
        }
    }
}