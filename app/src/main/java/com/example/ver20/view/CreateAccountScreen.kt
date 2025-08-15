package com.example.ver20.view

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import com.example.ver20.dao.UserService
import com.example.ver20.dao.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    onBackClick: () -> Unit,
    onAccountCreated: (UserData) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "계정 생성",
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 헤더 아이콘
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "새 계정 만들기",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Text(
                "거래를 시작하기 위해 계정을 생성하세요",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 계정 입력
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("계정") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    focusedLabelColor = Color(0xFF2196F3),
                    focusedLeadingIconColor = Color(0xFF2196F3)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 이메일 입력
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    focusedLabelColor = Color(0xFF2196F3),
                    focusedLeadingIconColor = Color(0xFF2196F3)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호 입력
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "비밀번호 숨기기" else "비밀번호 보기"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    focusedLabelColor = Color(0xFF2196F3),
                    focusedLeadingIconColor = Color(0xFF2196F3)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호 확인
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("비밀번호 확인") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showConfirmPassword) "비밀번호 숨기기" else "비밀번호 보기"
                        )
                    }
                },
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    focusedLabelColor = Color(0xFF2196F3),
                    focusedLeadingIconColor = Color(0xFF2196F3)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 계정 생성 버튼
            Button(
                onClick = {
                    val validationResult = validateInput(name, email, password, confirmPassword)
                    if (validationResult.isValid) {
                        isLoading = true
                        errorMessage = ""

                        // API 테스트 후 계정 생성
                        val userService = UserService()

                        // 1단계: 서버 연결 및 API 액션 테스트
                        Log.d("CreateAccount", "API 테스트 시작")
                        userService.testServerConnection { isConnected, message ->
                            CoroutineScope(Dispatchers.Main).launch {
                                if (isConnected) {
                                    Log.d("CreateAccount", "서버 연결 성공, API 액션 테스트 중...")

                                    // 2단계: API 액션 테스트
                                    userService.testApiActions { results ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            Log.d("CreateAccount", "API 액션 테스트 결과:\n$results")

                                            // 3단계: 실제 계정 생성 시도
                                            userService.createAccount(name, email, password) { success, createMessage ->
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    isLoading = false
                                                    if (success) {
                                                        Toast.makeText(context, "계정이 생성되었습니다!", Toast.LENGTH_SHORT).show()

                                                        // 생성된 계정으로 자동 로그인 시도
                                                        userService.loginUser(name, password) { loginSuccess, userData, loginMessage ->
                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                if (loginSuccess && userData != null) {
                                                                    userService.saveUserToPreferences(context, userData)
                                                                    Toast.makeText(context, "로그인 완료!", Toast.LENGTH_SHORT).show()
                                                                    onAccountCreated(userData)
                                                                } else {
                                                                    Toast.makeText(context, "계정은 생성되었지만 자동 로그인에 실패했습니다.", Toast.LENGTH_LONG).show()
                                                                    onBackClick()
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        errorMessage = """
                                                            계정 생성 실패: $createMessage
                                                            
                                                            API 테스트 결과:
                                                            $results
                                                            
                                                            서버가 'createUser' 액션을 지원하지 않는 것 같습니다.
                                                        """.trimIndent()
                                                        Toast.makeText(context, "계정 생성에 실패했습니다. 로그를 확인해주세요.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    errorMessage = "서버 연결 실패: $message"
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        errorMessage = validationResult.errorMessage
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
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
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "계정 생성하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 에러 메시지 표시
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    errorMessage,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 안내 문구
            Text(
                "계정을 생성하면 서비스 이용약관 및 개인정보 처리방침에 동의한 것으로 간주됩니다.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// 입력 검증 결과 클래스
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String
)

// 입력 검증 함수
private fun validateInput(
    name: String,
    email: String,
    password: String,
    confirmPassword: String
): ValidationResult {
    return when {
        name.isBlank() -> ValidationResult(false, "계정명을 입력해주세요")
        name.length < 3 -> ValidationResult(false, "계정명은 3자 이상이어야 합니다")
        email.isBlank() -> ValidationResult(false, "이메일을 입력해주세요")
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
            ValidationResult(false, "올바른 이메일 형식을 입력해주세요")
        password.isBlank() -> ValidationResult(false, "비밀번호를 입력해주세요")
        password.length < 6 -> ValidationResult(false, "비밀번호는 6자 이상이어야 합니다")
        password != confirmPassword -> ValidationResult(false, "비밀번호가 일치하지 않습니다")
        else -> ValidationResult(true, "")
    }
}