package com.example.ver20

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ver20.ui.theme.Ver20Theme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
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

    val tabs = listOf(
        TabItem("가격조회", Icons.Default.Search),
        TabItem("계좌조회", Icons.Default.AccountBox),
        TabItem("시세분석", Icons.Default.Info),
        TabItem("거래내역", Icons.Default.List)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Binance ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            "⚡",
                            fontSize = 20.sp,
                            color = Color(0xFFFFD700) // 골드색 번개
                        )
                        Text(
                            " Trader",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3), // 밝은 블루
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF2196F3) // 밝은 블루
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(tab.title)
                        },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFFBBDEFB),
                            unselectedTextColor = Color(0xFFBBDEFB),
                            indicatorColor = Color(0xFF1976D2) // 진한 블루
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> PriceScreen(modifier = Modifier.padding(paddingValues))
            1 -> AccountScreen(modifier = Modifier.padding(paddingValues))
            2 -> AnalysisScreen(modifier = Modifier.padding(paddingValues))
            3 -> TradeHistoryScreen(modifier = Modifier.padding(paddingValues))
        }
    }
}

data class TabItem(
    val title: String,
    val icon: ImageVector
)