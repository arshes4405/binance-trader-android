// MainActivity.kt - 코르타 AT 브랜딩 (기존 구조 최대한 유지)
package com.example.ver20.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ver20.ui.theme.Ver20Theme
import com.example.ver20.view.account.AccountBalanceScreen
import com.example.ver20.view.backtest.BacktestingScreen
import com.example.ver20.view.price.AnalysisScreen
import com.example.ver20.view.price.PriceScreen
import com.example.ver20.view.signal.MarketSignalScreen
import com.example.ver20.view.signal.user.CreateAccountScreen
import com.example.ver20.view.signal.user.LoginScreen
import com.example.ver20.view.signal.user.SecuritySettingsScreen
import com.example.ver20.view.signal.user.UserInfoScreen

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

    // 코르타 AT 브랜딩 컬러
    val cortaPrimary = Color(0xFF1A237E)
    val cortaGold = Color(0xFFFFD700)
    val cortaSilver = Color(0xFFC0C0C0)

    // 기존 탭 구조 그대로 유지하며 브랜딩만 적용
    val tabs = listOf(
        TabItem("시세조회", Icons.Default.Search),
        TabItem("⚔️ 시세포착", Icons.Default.Notifications), // 브랜딩 적용
        TabItem("계좌조회", Icons.Default.AccountBox),
        TabItem("백테스팅", Icons.Default.Analytics),
        TabItem("시세분석", Icons.Default.Info)
    )

    // 기존 분기 처리 로직 유지
    when {
        showLogin -> {
            LoginScreen(
                onBackClick = { showLogin = false },
                onLoginSuccess = { userData ->
                    showLogin = false
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
                }
            )
        }
        showSecuritySettings -> {
            SecuritySettingsScreen(
                onBackClick = { showSecuritySettings = false }
            )
        }
        else -> {
            // 메인 화면 - 코르타 AT 브랜딩 적용
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
                                    fontWeight = FontWeight.Bold,
                                    color = cortaGold
                                )
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
                            containerColor = cortaPrimary,
                            titleContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = cortaPrimary,
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
                // 기존 탭 컨텐츠 로직 완전히 유지
                when (selectedTab) {
                    0 -> PriceScreen(
                        modifier = Modifier.padding(paddingValues),
                        onShowCreateAccount = { showCreateAccount = true }
                    )
                    1 -> MarketSignalScreen(modifier = Modifier.padding(paddingValues))
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

// 기존 TabItem 구조 완전히 유지
data class TabItem(
    val title: String,
    val icon: ImageVector
)