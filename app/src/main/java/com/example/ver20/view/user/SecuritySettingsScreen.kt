package com.example.ver20.view.user

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.dao.ApiKeyService
import com.example.ver20.dao.ApiKeyData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val apiKeyService = remember { ApiKeyService(context) }

    // 상태 변수들
    var apiKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var isTestnet by remember { mutableStateOf(true) }
    var showApiKey by remember { mutableStateOf(false) }
    var showSecretKey by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var hasExistingKeys by remember { mutableStateOf(false) }
    var currentKeyData by remember { mutableStateOf<ApiKeyData?>(null) }

    // 기존 API 키 정보 로드
    LaunchedEffect(Unit) {
        val existingKeys = apiKeyService.getApiKeys()
        hasExistingKeys = existingKeys != null
        currentKeyData = existingKeys

        if (existingKeys != null) {
            isTestnet = existingKeys.isTestnet
            // 보안상 실제 키는 표시하지 않고 마스킹 처리
            apiKey = "*".repeat(20)
            secretKey = "*".repeat(20)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "API 키 설정",
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
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 안내 카드
            item {
                InstructionCard()
            }

            // 네트워크 모드 선택
            item {
                NetworkModeCard(
                    isTestnet = isTestnet,
                    onModeChange = {
                        isTestnet = it
                        if (hasExistingKeys) {
                            // 기존 키가 있으면 네트워크 모드만 변경
                            if (apiKeyService.switchNetwork(it)) {
                                Toast.makeText(
                                    context,
                                    "네트워크 모드가 변경되었습니다: ${if (it) "테스트넷" else "메인넷"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }

            // API 키 입력 섹션
            item {
                ApiKeyInputSection(
                    apiKey = if (hasExistingKeys) "" else apiKey,
                    secretKey = if (hasExistingKeys) "" else secretKey,
                    showApiKey = showApiKey,
                    showSecretKey = showSecretKey,
                    hasExistingKeys = hasExistingKeys,
                    onApiKeyChange = { apiKey = it },
                    onSecretKeyChange = { secretKey = it },
                    onShowApiKeyToggle = { showApiKey = !showApiKey },
                    onShowSecretKeyToggle = { showSecretKey = !showSecretKey }
                )
            }

            // 보안 안내 카드
            item {
                SecurityNoticeCard()
            }

            // 버튼 섹션
            item {
                ActionButtonsSection(
                    hasExistingKeys = hasExistingKeys,
                    isLoading = isLoading,
                    apiKey = apiKey,
                    secretKey = secretKey,
                    isTestnet = isTestnet,
                    onSave = {
                        if (apiKey.isBlank() || secretKey.isBlank()) {
                            Toast.makeText(context, "API 키와 Secret 키를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                            return@ActionButtonsSection
                        }

                        if (!apiKeyService.validateApiKeys(apiKey, secretKey)) {
                            Toast.makeText(context, "올바른 API 키 형식이 아닙니다", Toast.LENGTH_SHORT).show()
                            return@ActionButtonsSection
                        }

                        isLoading = true

                        if (apiKeyService.saveApiKeys(apiKey, secretKey, isTestnet)) {
                            Toast.makeText(context, "API 키가 저장되었습니다", Toast.LENGTH_SHORT).show()
                            hasExistingKeys = true
                            currentKeyData = ApiKeyData(apiKey, secretKey, isTestnet)
                            // 보안상 입력 필드 초기화
                            apiKey = "*".repeat(20)
                            secretKey = "*".repeat(20)
                        } else {
                            Toast.makeText(context, "API 키 저장에 실패했습니다", Toast.LENGTH_SHORT).show()
                        }

                        isLoading = false
                    },
                    onDelete = {
                        if (apiKeyService.clearApiKeys()) {
                            Toast.makeText(context, "API 키가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                            hasExistingKeys = false
                            currentKeyData = null
                            apiKey = ""
                            secretKey = ""
                        } else {
                            Toast.makeText(context, "API 키 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onTest = {
                        Toast.makeText(context, "API 키 테스트 기능은 개발 중입니다", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 현재 설정 정보
            if (hasExistingKeys) {
                item {
                    CurrentSettingsCard(currentKeyData)
                }
            }

            // API 키 획득 가이드
            item {
                ApiKeyGuideCard()
            }

            // 하단 여백
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun InstructionCard() {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "바이낸스 API 키 설정",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "실제 계좌 정보를 조회하고 거래하려면 바이낸스 API 키가 필요합니다.",
                fontSize = 14.sp,
                color = Color(0xFFBF360C),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun NetworkModeCard(
    isTestnet: Boolean,
    onModeChange: (Boolean) -> Unit
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
                "네트워크 모드",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 테스트넷 버튼
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTestnet) Color(0xFFFF9800) else Color.White
                    ),
                    onClick = { onModeChange(true) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🧪",
                            fontSize = 24.sp
                        )
                        Text(
                            "테스트넷",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isTestnet) Color.White else Color(0xFF666666)
                        )
                        Text(
                            "안전한 테스트",
                            fontSize = 10.sp,
                            color = if (isTestnet) Color.White else Color.Gray
                        )
                    }
                }

                // 메인넷 버튼
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isTestnet) Color(0xFFF44336) else Color.White
                    ),
                    onClick = { onModeChange(false) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🔴",
                            fontSize = 24.sp
                        )
                        Text(
                            "메인넷",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isTestnet) Color.White else Color(0xFF666666)
                        )
                        Text(
                            "실제 거래",
                            fontSize = 10.sp,
                            color = if (!isTestnet) Color.White else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyInputSection(
    apiKey: String,
    secretKey: String,
    showApiKey: Boolean,
    showSecretKey: Boolean,
    hasExistingKeys: Boolean,
    onApiKeyChange: (String) -> Unit,
    onSecretKeyChange: (String) -> Unit,
    onShowApiKeyToggle: () -> Unit,
    onShowSecretKeyToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                if (hasExistingKeys) "API 키 업데이트" else "API 키 입력",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // API Key 입력
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                placeholder = {
                    Text(
                        if (hasExistingKeys) "새 API Key 입력 (선택사항)"
                        else "바이낸스 API Key 입력"
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onShowApiKeyToggle) {
                        Icon(
                            if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showApiKey) "숨기기" else "보기"
                        )
                    }
                },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    focusedLabelColor = Color(0xFF2196F3)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Secret Key 입력
            OutlinedTextField(
                value = secretKey,
                onValueChange = onSecretKeyChange,
                label = { Text("Secret Key") },
                placeholder = {
                    Text(
                        if (hasExistingKeys) "새 Secret Key 입력 (선택사항)"
                        else "바이낸스 Secret Key 입력"
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onShowSecretKeyToggle) {
                        Icon(
                            if (showSecretKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showSecretKey) "숨기기" else "보기"
                        )
                    }
                },
                visualTransformation = if (showSecretKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    focusedLabelColor = Color(0xFF2196F3)
                )
            )
        }
    }
}

@Composable
private fun SecurityNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "보안 주의사항",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "• API 키는 안전하게 보관되며 암호화됩니다\n" +
                        "• 다른 사람과 API 키를 공유하지 마세요\n" +
                        "• 의심스러운 활동이 있으면 즉시 키를 재생성하세요\n" +
                        "• 처음 사용시에는 테스트넷을 권장합니다",
                fontSize = 12.sp,
                color = Color(0xFFD32F2F),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    hasExistingKeys: Boolean,
    isLoading: Boolean,
    apiKey: String,
    secretKey: String,
    isTestnet: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Column {
        // 저장 버튼
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (hasExistingKeys) "업데이트" else "저장",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (hasExistingKeys) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 테스트 버튼
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("테스트")
                }

                // 삭제 버튼
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("삭제")
                }
            }
        }
    }
}

@Composable
private fun CurrentSettingsCard(currentKeyData: ApiKeyData?) {
    if (currentKeyData != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E8)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "현재 설정",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "네트워크:",
                        fontSize = 14.sp,
                        color = Color(0xFF388E3C)
                    )
                    Text(
                        if (currentKeyData.isTestnet) "🧪 테스트넷" else "🔴 메인넷",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (currentKeyData.isTestnet) Color(0xFFFF9800) else Color(0xFFF44336)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "API 키:",
                        fontSize = 14.sp,
                        color = Color(0xFF388E3C)
                    )
                    Text(
                        "${currentKeyData.apiKey.take(8)}...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF388E3C)
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "API 키 발급 가이드",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B1FA2)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val steps = listOf(
                "1. 바이낸스 웹사이트 로그인",
                "2. 우측 상단 프로필 → API 관리",
                "3. '새 API 키 생성' 클릭",
                "4. API 키 라벨 입력 (예: Mobile App)",
                "5. Spot & Margin Trading 권한 체크",
                "6. API Key와 Secret Key 복사",
                "7. 위 입력창에 붙여넣기"
            )

            steps.forEach { step ->
                Text(
                    step,
                    fontSize = 12.sp,
                    color = Color(0xFF8E24AA),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "⚠️ 처음 사용시에는 테스트넷으로 연습해보세요!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7B1FA2)
            )
        }
    }
}