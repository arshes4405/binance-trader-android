// MainActivity.kt - 코르타 AT 스플래시 & 메인화면 (메뉴 구조 업데이트)

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
import com.example.ver20.view.signal.SignalHistoryScreen // 새로 추가될 시그널 화면
import com.example.ver20.view.autotrade.AutoTradingScreen // 새로 추가될 자동매매 화면
import com.example.ver20.view.price.PriceScreen
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

    // 업데이트된 탭 구조 - "시세분석" 삭제, "시그널", "자동매매" 추가
    val tabs = listOf(
        TabItem("HOME", Icons.Default.Home),
        TabItem("코인검색", Icons.Default.Search),
        TabItem("계좌조회", Icons.Default.AccountBox),
        TabItem("시세포착", Icons.Default.Notifications),
        TabItem("시그널", Icons.Default.TrendingUp),
        TabItem("자동매매", Icons.Default.SmartToy), // 새로 추가
        TabItem("백테스팅", Icons.Default.Analytics)
        // "시세분석" 탭 삭제됨
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
            MainContent(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                onShowUserInfo = { showUserInfo = true },
                onShowCreateAccount = { showCreateAccount = true },
                onShowSecuritySettings = { showSecuritySettings = true }
            )
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    tabs: List<TabItem>,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onShowUserInfo: () -> Unit,
    onShowCreateAccount: () -> Unit,
    onShowSecuritySettings: () -> Unit
) {
    val cortaPrimary = Color(0xFF1A237E)
    val cortaGold = Color(0xFFFFD700)
    val cortaSilver = Color(0xFFC0C0C0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "코르타 AT",
                        fontWeight = FontWeight.Bold,
                        color = cortaGold
                    )
                },
                actions = {
                    IconButton(onClick = onShowUserInfo) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "사용자 정보",
                            tint = cortaGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cortaPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = cortaPrimary,
                contentColor = cortaGold
            ) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 10.sp
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
            // 업데이트된 탭 컨텐츠 - 인덱스 조정됨
            when (selectedTab) {
                0 -> DashboardScreen() // HOME
                1 -> PriceScreen(      // 코인검색
                    onShowCreateAccount = onShowCreateAccount
                )
                2 -> AccountBalanceScreen( // 계좌조회
                    onShowSecuritySettings = onShowSecuritySettings
                )
                3 -> MarketSignalScreen() // 시세포착
                4 -> SignalHistoryScreen() // 시그널
                5 -> AutoTradingScreen() // 자동매매 (새로 추가)
                6 -> BacktestingScreen() // 백테스팅
                // 시세분석 화면 제거됨 (기존 인덱스 5)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
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
                            text = "활성 거래",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${activeTrades.value}건",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        // 시세포착 상태
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "시세포착 현황",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusItem("활성 설정", "2", Color(0xFF2196F3))
                        StatusItem("대기 중", "1", Color(0xFFFFC107))
                        StatusItem("완료", "7", Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfitCard(
    title: String,
    amount: Float,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (amount >= 0) "+${String.format("%,.0f", amount)}" else "${String.format("%,.0f", amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336),
                textAlign = TextAlign.Center
            )
            Text(
                text = "원",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    count: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF3F51B5)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 로고 영역
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        Color(0xFFFFD700),
                        CircleShape
                    )
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "코르타 AT",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Automated Trading System",
                fontSize = 14.sp,
                color = Color(0xFFFFD700),
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }
    }
}