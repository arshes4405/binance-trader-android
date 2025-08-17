// MainActivity.kt - 시세포착 탭 추가 (기존 파일 업데이트)

package com.example.ver20.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ver20.ui.theme.Ver20Theme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Ver20Theme {
                BinanceTraderApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinanceTraderApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showUserInfo by remember { mutableStateOf(false) }
    var showCreateAccount by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var showSecuritySettings by remember { mutableStateOf(false) }

    val tabs = listOf(
        TabItem("시세조회", Icons.Default.Search),
        TabItem("시세포착", Icons.Default.Notifications), // 새로 추가
        TabItem("계좌조회", Icons.Default.AccountBox),
        TabItem("백테스팅", Icons.Default.Analytics),
        TabItem("시세분석", Icons.Default.Info)
    )

    when {
        showLogin -> {
            LoginScreen(
                onBackClick = { showLogin = false },
                onLoginSuccess = { userData ->
                    showLogin = false
                    // 로그인 성공 시 메인 화면으로
                },
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
                    // 계정 생성 성공 시 메인 화면으로
                }
            )
        }
        showUserInfo -> {
            UserInfoScreen(
                onBackClick = { showUserInfo = false },
                onLoginClick = {
                    showUserInfo = false
                    showLogin = true
                },
                onCreateAccountClick = {
                    showUserInfo = false
                    showCreateAccount = true
                },
                onSecuritySettingsClick = {
                    showUserInfo = false
                    showSecuritySettings = true
                }
            )
        }
        showSecuritySettings -> {
            SecuritySettingsScreen(
                onBackClick = { showSecuritySettings = false }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Box(modifier = Modifier.offset(x = 8.dp)) {
                                    Text(
                                        "바이낸스 트레이더",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { showUserInfo = true }
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "유저 정보",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF2196F3),
                            titleContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF2196F3),
                        modifier = Modifier.height(110.dp)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        tab.title,
                                        fontSize = 11.sp
                                    )
                                },
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color(0xFFBBDEFB),
                                    unselectedTextColor = Color(0xFFBBDEFB),
                                    indicatorColor = Color(0xFF1976D2)
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                when (selectedTab) {
                    0 -> PriceScreen(
                        modifier = Modifier.padding(paddingValues),
                        onShowCreateAccount = { showCreateAccount = true }
                    )
                    1 -> MarketSignalScreen(modifier = Modifier.padding(paddingValues)) // 새로 추가
                    2 -> AccountBalanceScreen(
                        modifier = Modifier.padding(paddingValues),
                        onShowSecuritySettings = { showSecuritySettings = true }
                    )
                    3 -> BacktestingScreen(modifier = Modifier.padding(paddingValues))
                    4 -> AnalysisScreen(modifier = Modifier.padding(paddingValues))
                }
            }
        }
    }
}

data class TabItem(
    val title: String,
    val icon: ImageVector
)