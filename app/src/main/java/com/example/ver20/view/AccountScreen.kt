package com.example.ver20.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.dao.ApiKeyService
import com.example.ver20.dao.BalanceInfo
import kotlinx.coroutines.launch
import java.text.DecimalFormat

// 전체 계좌 총액 계산 함수
@Composable
fun getTotalBalance(): String {
    val formatter = DecimalFormat("$#,##0.00")

    // Spot + Future + Earn 총액 계산
    val spotTotal = 1 * 45000 + 10000 + 10 * 3000 + 100 * 300 + 5000 * 0.5 + 500 * 25 + 50 * 100 + 200 * 35 + 150 * 15 + 300 * 7
    val futureTotal = 1000 + 0.01 * 45000 + 0.5 * 3000 + 10 * 300 + 500 * 0.5 + 5 * 100
    val earnTotal = 5000 + 0.5 * 45000 + 5 * 3000 + 50 * 300 + 3000 + 2000 * 0.5 + 100 * 25 + 200 * 10 + 1000 * 1.5 + 80 * 35 + 150 * 2 + 300 * 5

    val totalUSD = spotTotal + futureTotal + earnTotal

    return "${formatter.format(totalUSD)}"
}

@Composable
fun AccountBalanceScreen(
    modifier: Modifier = Modifier,
    onShowSecuritySettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val apiKeyService = remember { ApiKeyService(context) }

    // 탭 상태 (0: Spot, 1: Future, 2: Earn)
    var selectedTab by remember { mutableIntStateOf(0) }

    // API 키 상태
    var hasApiKeys by remember { mutableStateOf(false) }
    var isTestnet by remember { mutableStateOf(true) }

    // API 키 상태 확인
    LaunchedEffect(Unit) {
        val apiKeys = apiKeyService.getApiKeys()
        hasApiKeys = apiKeys != null
        isTestnet = apiKeys?.isTestnet ?: true
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "계좌 조회",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    if (hasApiKeys) {
                        Text(
                            if (isTestnet) "🧪 테스트넷 모드" else "🔴 메인넷 모드",
                            fontSize = 12.sp,
                            color = if (isTestnet) Color(0xFFFF9800) else Color(0xFFF44336)
                        )
                    }
                }

                // 전체 계좌 총액 및 설정 버튼
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasApiKeys) {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "전체 자산",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                getTotalBalance(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // 설정 버튼
                    IconButton(onClick = onShowSecuritySettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "설정",
                            tint = Color(0xFF2196F3)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasApiKeys) {
                // API 키가 없는 경우
                NoApiKeyContent(onShowSecuritySettings = onShowSecuritySettings)
            } else {
                // 탭과 내용 표시
                Column {
                    // 탭 헤더
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFFBBDEFB),
                        contentColor = Color(0xFF1976D2)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTab == 0) Color(0xFF1976D2) else Color(0xFF757575)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Spot",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTab == 1) Color(0xFF1976D2) else Color(0xFF757575)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Future",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Savings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTab == 2) Color(0xFF1976D2) else Color(0xFF757575)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Earn",
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 계좌 총액 표시
                    AccountSummaryCard(selectedTab = selectedTab, isTestnet = isTestnet)

                    Spacer(modifier = Modifier.height(12.dp))

                    // 탭 내용
                    when (selectedTab) {
                        0 -> SpotTabContent(isTestnet = isTestnet)
                        1 -> FutureTabContent(isTestnet = isTestnet)
                        2 -> EarnTabContent(isTestnet = isTestnet)
                    }
                }
            }
        }
    }
}

@Composable
fun NoApiKeyContent(onShowSecuritySettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "API 키가 필요합니다",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "계좌 정보를 조회하려면\n바이낸스 API 키를 설정해주세요",
                    fontSize = 15.sp,
                    color = Color(0xFFBF360C),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onShowSecuritySettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "API 키 설정",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSummaryCard(selectedTab: Int, isTestnet: Boolean) {
    val formatter = DecimalFormat("#,##0.00")

    // 탭별 총액 계산 (샘플 데이터)
    val (totalUSD, accountType, cardColor) = when (selectedTab) {
        0 -> { // Spot
            val spotTotal = 1 * 45000 + 10000 + 10 * 3000 + 100 * 300 + 5000 * 0.5 + 500 * 25 + 50 * 100 + 200 * 35 + 150 * 15 + 300 * 7
            Triple(spotTotal.toDouble(), "Spot 자산", Color(0xFFE8F5E8))
        }
        1 -> { // Future
            val futureTotal = 1000 + 0.01 * 45000 + 0.5 * 3000 + 10 * 300 + 500 * 0.5 + 5 * 100
            Triple(futureTotal.toDouble(), "Future 자산", Color(0xFFF3E5F5))
        }
        2 -> { // Earn
            val earnTotal = 5000 + 0.5 * 45000 + 5 * 3000 + 50 * 300 + 3000 + 2000 * 0.5 + 100 * 25 + 200 * 10 + 1000 * 1.5 + 80 * 35 + 150 * 2 + 300 * 5
            Triple(earnTotal.toDouble(), "Earn 자산", Color(0xFFFFF3E0))
        }
        else -> Triple(0.0, "자산", Color.White)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    "$accountType 총액",
                    fontSize = 16.sp,
                    color = when (selectedTab) {
                        0 -> Color(0xFF2E7D32) // Spot - 초록
                        1 -> Color(0xFF7B1FA2) // Future - 보라
                        2 -> Color(0xFFE65100) // Earn - 주황
                        else -> Color(0xFF1976D2)
                    },
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${formatter.format(totalUSD)} USD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (selectedTab) {
                        0 -> Color(0xFF2E7D32) // Spot - 초록
                        1 -> Color(0xFF7B1FA2) // Future - 보라
                        2 -> Color(0xFFE65100) // Earn - 주황
                        else -> Color(0xFF1976D2)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 24시간 변동률 (예시)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val changePercent = when (selectedTab) {
                    0 -> 2.45 // Spot +2.45%
                    1 -> -1.23 // Future -1.23%
                    2 -> 0.89 // Earn +0.89%
                    else -> 0.0
                }

                Icon(
                    if (changePercent >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (changePercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    "${if (changePercent >= 0) "+" else ""}${String.format("%.2f", changePercent)}% (24h)",
                    fontSize = 12.sp,
                    color = if (changePercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isTestnet) {
                    Text(
                        "🧪 테스트넷",
                        fontSize = 10.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SpotTabContent(isTestnet: Boolean) {
    val sampleBalances = listOf(
        BalanceInfo("BTC", "1.00000000", "0.00000000"),
        BalanceInfo("USDT", "10000.00000000", "0.00000000"),
        BalanceInfo("ETH", "10.00000000", "0.00000000"),
        BalanceInfo("BNB", "100.00000000", "0.00000000"),
        BalanceInfo("ADA", "5000.00000000", "0.00000000"),
        BalanceInfo("DOT", "500.00000000", "0.00000000"),
        BalanceInfo("SOL", "50.00000000", "0.00000000"),
        BalanceInfo("AVAX", "200.00000000", "0.00000000"),
        BalanceInfo("LINK", "150.00000000", "0.00000000"),
        BalanceInfo("UNI", "300.00000000", "0.00000000")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(sampleBalances) { balance ->
            SimpleBalanceRow(
                asset = balance.asset,
                amount = balance.free,
                backgroundColor = Color(0xFFE8F5E8)
            )
        }
    }
}

@Composable
fun FutureTabContent(isTestnet: Boolean) {
    val sampleBalances = listOf(
        BalanceInfo("USDT", "1000.00000000", "0.00000000"),
        BalanceInfo("BTC", "0.01000000", "0.005000000"),
        BalanceInfo("ETH", "0.50000000", "0.10000000"),
        BalanceInfo("BNB", "10.00000000", "2.00000000"),
        BalanceInfo("ADA", "500.00000000", "100.00000000"),
        BalanceInfo("SOL", "5.00000000", "1.00000000")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(sampleBalances) { balance ->
            SimpleBalanceRow(
                asset = balance.asset,
                amount = balance.free,
                backgroundColor = Color(0xFFF3E5F5),
                locked = balance.locked
            )
        }
    }
}

@Composable
fun EarnTabContent(isTestnet: Boolean) {
    val earnBalances = listOf(
        BalanceInfo("USDT", "5000.00000000", "0.00000000"),
        BalanceInfo("BTC", "0.50000000", "0.00000000"),
        BalanceInfo("ETH", "5.00000000", "0.00000000"),
        BalanceInfo("BNB", "50.00000000", "0.00000000"),
        BalanceInfo("BUSD", "3000.00000000", "0.00000000"),
        BalanceInfo("ADA", "2000.00000000", "0.00000000"),
        BalanceInfo("DOT", "100.00000000", "0.00000000"),
        BalanceInfo("ATOM", "200.00000000", "0.00000000"),
        BalanceInfo("MATIC", "1000.00000000", "0.00000000"),
        BalanceInfo("AVAX", "80.00000000", "0.00000000"),
        BalanceInfo("LUNA", "150.00000000", "0.00000000"),
        BalanceInfo("NEAR", "300.00000000", "0.00000000")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(earnBalances) { balance ->
            SimpleBalanceRow(
                asset = balance.asset,
                amount = balance.free,
                backgroundColor = Color(0xFFFFF3E0),
                isEarn = true
            )
        }
    }
}

@Composable
fun SimpleBalanceRow(
    asset: String,
    amount: String,
    backgroundColor: Color,
    locked: String = "0.00000000",
    isEarn: Boolean = false
) {
    val formatter = DecimalFormat("#,##0.########")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 코인 이름
            Text(
                asset,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isEarn -> Color(0xFFE65100)
                    backgroundColor == Color(0xFFF3E5F5) -> Color(0xFF7B1FA2)
                    else -> Color(0xFF2E7D32)
                }
            )

            // 잔고 정보
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    formatter.format(amount.toDoubleOrNull() ?: 0.0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isEarn -> Color(0xFFFF9800)
                        backgroundColor == Color(0xFFF3E5F5) -> Color(0xFF9C27B0)
                        else -> Color(0xFF4CAF50)
                    }
                )

                // Future에서 포지션 정보 표시
                if (locked.toDoubleOrNull()?.let { it > 0.0 } == true) {
                    Text(
                        "포지션: ${formatter.format(locked.toDouble())}",
                        fontSize = 10.sp,
                        color = Color(0xFFFF9800)
                    )
                }

                // Earn에서 수익률 표시 (예시)
                if (isEarn) {
                    Text(
                        "APY: ${(1..15).random()}%",
                        fontSize = 10.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

// 3개 값을 담는 데이터 클래스는 Kotlin 기본 제공
// Triple<A, B, C> 사용