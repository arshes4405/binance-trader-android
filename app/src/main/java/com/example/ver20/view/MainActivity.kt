// MainActivity.kt - 코르타 AT 스플래시 & 메인화면 (기존 소스 업데이트)

package com.example.ver20.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.example.ver20.view.backtest.BacktestingScreen
import com.example.ver20.view.signal.user.UserInfoScreen
import com.example.ver20.view.signal.user.SecuritySettingsScreen
import com.example.ver20.view.signal.user.LoginScreen
import com.example.ver20.view.signal.user.CreateAccountScreen
import com.example.ver20.view.signal.MarketSignalScreen
import com.example.ver20.view.price.PriceScreen
import com.example.ver20.view.price.AnalysisScreen
import com.example.ver20.view.account.AccountBalanceScreen
import com.example.ver20.ui.theme.Ver20Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Ver20Theme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    var showSplash by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var showUserInfo by remember { mutableStateOf(false) }
    var showCreateAccount by remember { mutableStateOf(false) }
    var showSecuritySettings by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }

    // 스플래시 화면 타이머
    LaunchedEffect(Unit) {
        delay(2500) // 2.5초 후 메인화면으로
        showSplash = false
    }

    // 코르타 AT 브랜딩 컬러
    val cortaPrimary = Color(0xFF1A237E)
    val cortaGold = Color(0xFFFFD700)
    val cortaSilver = Color(0xFFC0C0C0)

    // 기존 탭 구조 유지 - 첫 번째만 HOME으로 변경
    val tabs = listOf(
        TabItem("HOME", Icons.Default.Home),        // 대시보드 -> HOME 변경
        TabItem("코인검색", Icons.Default.Search),   // 기존 시세조회 복원
        TabItem("시세포착", Icons.Default.Notifications), // 브랜딩 적용
        TabItem("계좌조회", Icons.Default.AccountBox), // 기존 계좌조회 복원
        TabItem("백테스팅", Icons.Default.Analytics),
        TabItem("시세분석", Icons.Default.Info)
    )

    when {
        showSplash -> {
            SplashScreen()
        }
        showLogin -> {
            LoginScreen(
                onBackClick = { showLogin = false },
                onLoginSuccess = { userData -> showLogin = false },
                onCreateAccountClick = {
                    showLogin = false
                    showCreateAccount = true
                }
            )
        }
        showCreateAccount -> {
            CreateAccountScreen(
                onBackClick = { showCreateAccount = false },
                onAccountCreated = { userData ->
                    showCreateAccount = false
                }
            )
        }
        showUserInfo -> {
            UserInfoScreen(
                onBackClick = { showUserInfo = false },
                onSecuritySettingsClick = {
                    showUserInfo = false
                    showSecuritySettings = true
                },
                onLoginClick = {
                    showUserInfo = false
                    showLogin = true
                },
                onCreateAccountClick = {
                    showUserInfo = false
                    showCreateAccount = true
                }
            )
        }
        showSecuritySettings -> {
            SecuritySettingsScreen(
                onBackClick = { showSecuritySettings = false }
            )
        }
        else -> {
            MainScreen(
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                onUserInfoClick = { showUserInfo = true },
                tabs = tabs,
                cortaGold = cortaGold,
                cortaSilver = cortaSilver,
                onShowCreateAccount = { showCreateAccount = true },
                onShowSecuritySettings = { showSecuritySettings = true }
            )
        }
    }
}

data class TabItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // 검 아이콘 회전 애니메이션
    val swordRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sword_rotation"
    )

    // 로고 크기 펄스 애니메이션
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    // 배경 그라데이션
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D1117),
            Color(0xFF1A1A2E),
            Color(0xFF16213E)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 메인 로고
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚔️",
                        fontSize = 48.sp,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = swordRotation
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 브랜드명
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "코르타",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = " AT",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF64B5F6)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "스마트 자동매매 시스템",
                fontSize = 14.sp,
                color = Color(0xFF90A4AE),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 로딩 인디케이터
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color(0xFF2196F3),
                strokeWidth = 3.dp
            )
        }

        // 하단 버전 정보
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "버전 2.0.0",
                fontSize = 12.sp,
                color = Color(0xFF546E7A)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onUserInfoClick: () -> Unit,
    tabs: List<TabItem>,
    cortaGold: Color,
    cortaSilver: Color,
    onShowCreateAccount: () -> Unit,
    onShowSecuritySettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 검 아이콘
                        Text(
                            text = "⚔️",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        // 브랜드명
                        Text(
                            text = "코르타",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        // AT
                        Text(
                            text = " AT",
                            fontSize = 18.sp,
                            color = Color(0xFF64B5F6)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onUserInfoClick) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "계정 정보",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E1E1E)
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title,
                                tint = if (selectedTab == index) cortaGold else cortaSilver
                            )
                        },
                        label = {
                            Text(
                                tab.title,
                                fontSize = 11.sp,
                                color = if (selectedTab == index) cortaGold else cortaSilver
                            )
                        },
                        selected = selectedTab == index,
                        onClick = { onTabChange(index) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cortaGold,
                            selectedTextColor = cortaGold,
                            unselectedIconColor = cortaSilver,
                            unselectedTextColor = cortaSilver,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 기존 탭 컨텐츠 로직 완전히 유지
            when (selectedTab) {
                0 -> DashboardScreen() // HOME - 새로운 대시보드
                1 -> PriceScreen(      // 코인검색 - 기존 시세조회 복원
                    onShowCreateAccount = onShowCreateAccount
                )
                2 -> MarketSignalScreen() // 시세포착
                3 -> AccountBalanceScreen( // 계좌조회 - 기존 기능 복원
                    onShowSecuritySettings = onShowSecuritySettings
                )
                4 -> BacktestingScreen() // 백테스팅
                5 -> AnalysisScreen()    // 시세분석
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    val totalProfit = remember { mutableStateOf(1250000f) }
    val todayProfit = remember { mutableStateOf(45000f) }
    val activeTrades = remember { mutableStateOf(3) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "대시보드",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 수익 현황 카드들
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfitCard(
                    title = "총 수익",
                    amount = totalProfit.value,
                    isPositive = totalProfit.value > 0,
                    modifier = Modifier.weight(1f)
                )
                ProfitCard(
                    title = "오늘 수익",
                    amount = todayProfit.value,
                    isPositive = todayProfit.value > 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 활성 거래 현황
        item {
            StatusCard(
                title = "활성 거래",
                count = activeTrades.value,
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF4CAF50)
            )
        }

        // 빠른 액션 버튼들
        item {
            Text(
                text = "빠른 액션",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    title = "코인 검색",
                    icon = Icons.Default.Search,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                ) {
                    // TODO: 코인검색 탭으로 이동
                }
                QuickActionButton(
                    title = "시세포착 설정",
                    icon = Icons.Default.Add,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                ) {
                    // TODO: 시세포착 화면으로 이동
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    title = "백테스트 실행",
                    icon = Icons.Default.Analytics,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                ) {
                    // TODO: 백테스팅 화면으로 이동
                }
                QuickActionButton(
                    title = "계좌 조회",
                    icon = Icons.Default.AccountBox,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                ) {
                    // TODO: 계좌조회 화면으로 이동
                }
            }
        }

        // 최근 알림
        item {
            Text(
                text = "최근 알림",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(3) { index ->
            NotificationItem(
                title = when(index) {
                    0 -> "BTC-USDT 매수 신호 포착"
                    1 -> "ETH-USDT 포지션 청산 완료"
                    else -> "시세포착 조건 업데이트 필요"
                },
                time = "${index + 1}시간 전",
                type = when(index) {
                    0 -> "BUY"
                    1 -> "SELL"
                    else -> "INFO"
                }
            )
        }
    }
}

@Composable
fun ProfitCard(
    title: String,
    amount: Float,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF90A4AE)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${if (isPositive) "+" else ""}${String.format("%,.0f", amount)}원",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE53E3E)
            )
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color(0xFF90A4AE)
                )
                Text(
                    text = "$count 개",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = color,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NotificationItem(
    title: String,
    time: String,
    type: String
) {
    val typeColor = when(type) {
        "BUY" -> Color(0xFF4CAF50)
        "SELL" -> Color(0xFFE53E3E)
        else -> Color(0xFF2196F3)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 타입 인디케이터
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(typeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = time,
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                )
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        val settingItems = listOf(
            "알림 설정" to Icons.Default.Notifications,
            "거래 설정" to Icons.Default.Settings,
            "보안 설정" to Icons.Default.Security,
            "백업 및 복원" to Icons.Default.Backup,
            "앱 정보" to Icons.Default.Info
        )

        items(settingItems) { (title, icon) ->
            SettingItem(title = title, icon = icon) {
                // TODO: 각 설정 화면으로 이동
            }
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF90A4AE),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF90A4AE),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}