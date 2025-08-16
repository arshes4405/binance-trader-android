package com.example.ver20.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.dao.UserService
import com.example.ver20.dao.UserData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {},
    onSecuritySettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userService = UserService()
    var currentUser by remember { mutableStateOf<UserData?>(null) }

    // 현재 로그인된 사용자 확인
    LaunchedEffect(Unit) {
        currentUser = userService.getUserFromPreferences(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "유저 정보",
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
                .verticalScroll(rememberScrollState()), // 🔥 스크롤 추가!
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // 여백 줄임

            // 프로필 이미지 - 크기 축소
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2196F3)
                ),
                modifier = Modifier.size(80.dp) // 120dp → 80dp로 축소
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "프로필",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp) // 60dp → 40dp로 축소
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // 여백 줄임

            // 로그인 상태에 따른 표시
            if (currentUser != null) {
                // 로그인된 상태
                Text(
                    currentUser!!.username,
                    fontSize = 22.sp, // 24sp → 22sp로 축소
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                Text(
                    currentUser!!.email,
                    fontSize = 15.sp, // 16sp → 15sp로 축소
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp)) // 여백 줄임

                // 정보 카드들
                UserInfoCard(
                    icon = Icons.Default.AccountBalance,
                    title = "계좌 정보",
                    subtitle = "잔고 및 자산 조회",
                    onClick = { /* TODO: 계좌 정보 화면으로 이동 */ }
                )

                Spacer(modifier = Modifier.height(12.dp))

                UserInfoCard(
                    icon = Icons.Default.Security,
                    title = "보안 설정",
                    subtitle = "API 키 및 보안 관리",
                    onClick = onSecuritySettingsClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                UserInfoCard(
                    icon = Icons.Default.Notifications,
                    title = "알림 설정",
                    subtitle = "가격 알림 및 거래 알림",
                    onClick = { /* TODO: 알림 설정 화면으로 이동 */ }
                )

                Spacer(modifier = Modifier.height(12.dp))

                UserInfoCard(
                    icon = Icons.Default.Settings,
                    title = "앱 설정",
                    subtitle = "테마 및 기본 설정",
                    onClick = { /* TODO: 앱 설정 화면으로 이동 */ }
                )

                Spacer(modifier = Modifier.height(12.dp))

                UserInfoCard(
                    icon = Icons.Default.History,
                    title = "거래 내역",
                    subtitle = "과거 거래 기록 조회",
                    onClick = { /* TODO: 거래 내역 화면으로 이동 */ }
                )

                Spacer(modifier = Modifier.height(12.dp))

                UserInfoCard(
                    icon = Icons.Default.TrendingUp,
                    title = "자동매매 설정",
                    subtitle = "매매 전략 및 설정 관리",
                    onClick = { /* TODO: 자동매매 설정 화면으로 이동 */ }
                )

                Spacer(modifier = Modifier.height(12.dp))

                UserInfoCard(
                    icon = Icons.Default.Info,
                    title = "앱 정보",
                    subtitle = "버전 정보 및 도움말",
                    onClick = { /* TODO: 앱 정보 화면으로 이동 */ }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 로그아웃 버튼
                Button(
                    onClick = {
                        userService.logout(context)
                        currentUser = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "로그아웃",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 하단 여백 추가 (스크롤 여유 공간)
                Spacer(modifier = Modifier.height(32.dp))

            } else {
                // 로그인되지 않은 상태
                Text(
                    "로그인이 필요합니다",
                    fontSize = 22.sp, // 24sp → 22sp로 축소
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                Text(
                    "계정에 로그인하여 모든 기능을 이용하세요",
                    fontSize = 15.sp, // 16sp → 15sp로 축소
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 로그인 버튼
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "로그인",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 계정 생성 버튼
                OutlinedButton(
                    onClick = onCreateAccountClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "계정 만들기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 로그인 없이도 보안 설정 접근 가능하도록
                Text(
                    "또는",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onSecuritySettingsClick
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "API 키 설정하기",
                        fontSize = 16.sp,
                        color = Color(0xFF2196F3)
                    )
                }

                // 하단 여백 추가 (스크롤 여유 공간)
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun UserInfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        onClick = onClick  // 🔥 클릭 이벤트 연결!
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
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "더보기",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}