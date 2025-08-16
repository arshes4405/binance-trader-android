package com.example.ver20.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ver20.dao.ApiKeyService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiKeyService = remember { ApiKeyService(context) }

    // API 키 상태
    var apiKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var showSecretKey by remember { mutableStateOf(false) }
    var isTestMode by remember { mutableStateOf(true) } // 테스트넷/메인넷 토글
    var isLoading by remember { mutableStateOf(false) }
    var validationStatus by remember { mutableStateOf("") }

    // 설정 상태
    var autoLockEnabled by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var notificationSecurity by remember { mutableStateOf(true) }

    // 저장된 API 키 불러오기
    LaunchedEffect(Unit) {
        val savedApiKeys = apiKeyService.getApiKeys()
        if (savedApiKeys != null) {
            apiKey = savedApiKeys.apiKey
            secretKey = savedApiKeys.secretKey
            isTestMode = savedApiKeys.isTestnet

            if (savedApiKeys.isValid) {
                validationStatus = "✅ 검증됨 (${formatTime(savedApiKeys.lastValidated)})"
            } else {
                validationStatus = "⚠️ 미검증"
            }
        } else {
            // 저장된 키가 없고 테스트넷 모드일 때 기본값 설정
            if (isTestMode) {
                apiKey = "aVr5Y9fwI8bSGMAgL49vg4nFBBJDIwk8ZNDDhwJMNTgtAe3KsOAMJV11nlIMQ6lN"
                secretKey = "MouGQp2UmZ8Jw4C9uDq6QshweTCiSmLNiQEGK9zKdHkjRm66Izcippxtsm7Ptmvt"
                validationStatus = "🧪 테스트넷 기본 키 로드됨"
            }
        }
    }

    // 테스트넷/메인넷 토글 변경 시 처리
    LaunchedEffect(isTestMode) {
        if (isTestMode && apiKey.isEmpty() && secretKey.isEmpty()) {
            // 테스트넷으로 변경 시 기본 키 설정
            apiKey = "aVr5Y9fwI8bSGMAgL49vg4nFBBJDIwk8ZNDDhwJMNTgtAe3KsOAMJV11nlIMQ6lN"
            secretKey = "MouGQp2UmZ8Jw4C9uDq6QshweTCiSmLNiQEGK9zKdHkjRm66Izcippxtsm7Ptmvt"
            validationStatus = "🧪 테스트넷 기본 키 로드됨"
        } else if (!isTestMode && apiKey == "aVr5Y9fwI8bSGMAgL49vg4nFBBJDIwk8ZNDDhwJMNTgtAe3KsOAMJV11nlIMQ6lN") {
            // 메인넷으로 변경 시 테스트넷 키라면 초기화
            apiKey = ""
            secretKey = ""
            validationStatus = "⚠️ 메인넷 키를 입력해주세요"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "보안 설정",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // API 키 설정 섹션
            SecuritySection(
                title = "Binance API 설정",
                icon = Icons.Default.Key,
                description = "거래를 위한 API 키를 설정하세요"
            ) {

                // API 키 상태 표시
                if (validationStatus.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (validationStatus.contains("✅")) Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "상태: $validationStatus",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (validationStatus.contains("✅")) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )

                            if (apiKeyService.hasApiKeys()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "(키: ${apiKeyService.getMaskedApiKey()})",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 테스트넷/메인넷 토글
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTestMode) Color(0xFFFFF3E0) else Color(0xFFE8F5E8)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isTestMode) "테스트넷 모드" else "메인넷 모드",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTestMode) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                            Text(
                                text = if (isTestMode) "안전한 테스트 환경" else "실제 거래 환경",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = !isTestMode,
                            onCheckedChange = { isTestMode = !it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                uncheckedThumbColor = Color(0xFFFF9800)
                            )
                        )
                    }
                }

                // 테스트넷 전용 빠른 설정 버튼
                if (isTestMode) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            apiKey = "aVr5Y9fwI8bSGMAgL49vg4nFBBJDIwk8ZNDDhwJMNTgtAe3KsOAMJV11nlIMQ6lN"
                            secretKey = "MouGQp2UmZ8Jw4C9uDq6QshweTCiSmLNiQEGK9zKdHkjRm66Izcippxtsm7Ptmvt"
                            validationStatus = "🧪 테스트넷 기본 키 로드됨"
                            Toast.makeText(context, "테스트넷 기본 키가 설정되었습니다", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF9800)
                        )
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🧪 테스트넷 기본 키 사용", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key 입력
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    leadingIcon = {
                        Icon(Icons.Default.VpnKey, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showApiKey) "숨기기" else "보기"
                            )
                        }
                    },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        focusedLabelColor = Color(0xFF2196F3)
                    ),
                    placeholder = { Text("Binance API Key를 입력하세요") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Secret Key 입력
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = { Text("Secret Key") },
                    leadingIcon = {
                        Icon(Icons.Default.Security, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showSecretKey = !showSecretKey }) {
                            Icon(
                                if (showSecretKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showSecretKey) "숨기기" else "보기"
                            )
                        }
                    },
                    visualTransformation = if (showSecretKey) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        focusedLabelColor = Color(0xFF2196F3)
                    ),
                    placeholder = { Text("Binance Secret Key를 입력하세요") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // API 키 테스트 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (apiKey.isNotBlank() && secretKey.isNotBlank()) {
                                isLoading = true
                                validationStatus = "🔄 검증 중..."

                                scope.launch {
                                    // 먼저 저장
                                    try {
                                        apiKeyService.saveApiKeys(apiKey, secretKey, isTestMode)

                                        // 그 다음 검증
                                        val (isValid, message) = apiKeyService.validateApiKeys()

                                        validationStatus = if (isValid) {
                                            "✅ $message"
                                        } else {
                                            "❌ $message"
                                        }

                                        Toast.makeText(
                                            context,
                                            if (isValid) "API 키 검증 성공!" else "검증 실패: $message",
                                            Toast.LENGTH_LONG
                                        ).show()

                                    } catch (e: Exception) {
                                        validationStatus = "❌ 오류: ${e.message}"
                                        Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "API 키와 Secret 키를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF2196F3)
                            )
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("연결 테스트", fontSize = 14.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (apiKey.isNotBlank() && secretKey.isNotBlank()) {
                                try {
                                    apiKeyService.saveApiKeys(apiKey, secretKey, isTestMode)
                                    validationStatus = "💾 저장됨 (미검증)"
                                    Toast.makeText(context, "API 키가 안전하게 저장되었습니다", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("저장", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 앱 보안 설정 섹션
            SecuritySection(
                title = "앱 보안",
                icon = Icons.Default.Shield,
                description = "앱 사용 시 보안 설정을 관리하세요"
            ) {

                // 자동 잠금
                SecurityToggleItem(
                    icon = Icons.Default.Lock,
                    title = "자동 잠금",
                    subtitle = "일정 시간 후 자동으로 잠금",
                    checked = autoLockEnabled,
                    onCheckedChange = { autoLockEnabled = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 생체 인증
                SecurityToggleItem(
                    icon = Icons.Default.Fingerprint,
                    title = "생체 인증",
                    subtitle = "지문 또는 얼굴 인식으로 잠금 해제",
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 알림 보안
                SecurityToggleItem(
                    icon = Icons.Default.NotificationsOff,
                    title = "알림 보안",
                    subtitle = "잠금 화면에서 민감한 정보 숨김",
                    checked = notificationSecurity,
                    onCheckedChange = { notificationSecurity = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 위험 구역
            SecuritySection(
                title = "위험 구역",
                icon = Icons.Default.Warning,
                description = "신중하게 사용하세요",
                isWarning = true
            ) {

                // API 키 삭제 버튼
                OutlinedButton(
                    onClick = {
                        if (apiKeyService.hasApiKeys()) {
                            apiKeyService.deleteApiKeys()
                            apiKey = ""
                            secretKey = ""
                            validationStatus = ""
                            Toast.makeText(context, "저장된 API 키가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "삭제할 API 키가 없습니다", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("저장된 API 키 삭제", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 시간 포맷팅 함수
private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "없음"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "방금 전"
        diff < 3600_000 -> "${diff / 60_000}분 전"
        diff < 86400_000 -> "${diff / 3600_000}시간 전"
        else -> "${diff / 86400_000}일 전"
    }
}

@Composable
fun SecuritySection(
    title: String,
    icon: ImageVector,
    description: String,
    isWarning: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) Color(0xFFFFF0F0) else Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 섹션 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isWarning) Color(0xFFF44336) else Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWarning) Color(0xFFF44336) else Color(0xFF1976D2)
                    )
                    Text(
                        description,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 섹션 내용
            content()
        }
    }
}

@Composable
fun SecurityToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50)
                )
            )
        }
    }
}