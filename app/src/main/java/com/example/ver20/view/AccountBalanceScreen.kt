// AccountBalanceScreen.kt - 기존 파일을 이 내용으로 교체

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
import com.example.ver20.dao.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import android.util.Log
import kotlin.math.abs

@Composable
fun AccountBalanceScreen(
    modifier: Modifier = Modifier,
    onShowSecuritySettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val apiKeyService = remember { ApiKeyService(context) }
    val binanceService = remember { RealBinanceService() } // 기존 서비스 사용
    val coroutineScope = rememberCoroutineScope()

    // 상태 변수들
    var hasApiKeys by remember { mutableStateOf(false) }
    var isTestnet by remember { mutableStateOf(true) }
    var apiKeyData by remember { mutableStateOf<ApiKeyData?>(null) }

    // 데이터 상태
    var spotAccountInfo by remember { mutableStateOf<AccountInfo?>(null) }
    var futuresAccountInfo by remember { mutableStateOf<FuturesAccountInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 탭별 자산 분류
    var spotBalances by remember { mutableStateOf<List<BalanceInfo>>(emptyList()) }
    var earnBalances by remember { mutableStateOf<List<BalanceInfo>>(emptyList()) }
    var futuresBalances by remember { mutableStateOf<List<BalanceInfo>>(emptyList()) }
    var spotTotalUSD by remember { mutableStateOf(0.0) }
    var earnTotalUSD by remember { mutableStateOf(0.0) }
    var futuresTotalUSD by remember { mutableStateOf(0.0) }
    var totalBalanceUSD by remember { mutableStateOf(0.0) }

    // 통합 데이터 로드 함수
    fun loadIntegratedData() {
        if (apiKeyData == null) return

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                Log.d("AccountScreen", "🚀 통합 계좌 데이터 로드 시작")

                // 기존 RealBinanceService의 새로운 통합 함수 사용
                val (spotResponse, futuresInfo, priceMap) = binanceService.getIntegratedAccountInfo(apiKeyData!!)

                if (spotResponse.success && spotResponse.data != null) {
                    spotAccountInfo = spotResponse.data
                    futuresAccountInfo = futuresInfo

                    // 자산 분류
                    classifyIntegratedAssets(
                        spotBalances = spotResponse.data.balances,
                        futuresInfo = futuresInfo,
                        priceMap = priceMap
                    ) { spot, earn, futures, spotTotal, earnTotal, futuresTotal ->
                        spotBalances = spot
                        earnBalances = earn
                        futuresBalances = futures
                        spotTotalUSD = spotTotal
                        earnTotalUSD = earnTotal
                        futuresTotalUSD = futuresTotal
                        totalBalanceUSD = spotTotal + earnTotal + futuresTotal

                        Log.d("AccountScreen", "💰 총 자산: Spot $${spotTotal}, Earn $${earnTotal}, Futures $${futuresTotal}")
                    }

                    errorMessage = null
                } else {
                    errorMessage = spotResponse.message ?: "계좌 정보 조회 실패"
                }

            } catch (e: Exception) {
                Log.e("AccountScreen", "❌ 통합 데이터 로드 예외: ${e.message}")
                errorMessage = "데이터 로드 오류: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        val keys = apiKeyService.getApiKeys()
        hasApiKeys = keys != null
        apiKeyData = keys

        if (keys != null) {
            isTestnet = keys.isTestnet
            loadIntegratedData()
        }
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
            // 헤더
            AccountHeader(
                hasApiKeys = hasApiKeys,
                isTestnet = isTestnet,
                totalBalanceUSD = totalBalanceUSD,
                spotTotalUSD = spotTotalUSD,
                futuresTotalUSD = futuresTotalUSD,
                onRefreshClick = { loadIntegratedData() },
                onSettingsClick = onShowSecuritySettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 컨텐츠
            when {
                !hasApiKeys -> {
                    NoApiKeyContentSection(onShowSecuritySettings = onShowSecuritySettings)
                }
                isLoading -> {
                    LoadingContentSection()
                }
                errorMessage != null -> {
                    ErrorContentSection(
                        errorMessage = errorMessage!!,
                        onRetryClick = { loadIntegratedData() }
                    )
                }
                spotAccountInfo != null -> {
                    AccountContent(
                        accountInfo = spotAccountInfo!!,
                        futuresAccountInfo = futuresAccountInfo,
                        spotBalances = spotBalances,
                        earnBalances = earnBalances,
                        futuresBalances = futuresBalances,
                        spotTotalUSD = spotTotalUSD,
                        earnTotalUSD = earnTotalUSD,
                        futuresTotalUSD = futuresTotalUSD
                    )
                }
                else -> {
                    EmptyContentSection()
                }
            }
        }
    }
}

@Composable
private fun AccountHeader(
    hasApiKeys: Boolean,
    isTestnet: Boolean,
    totalBalanceUSD: Double,
    spotTotalUSD: Double,
    futuresTotalUSD: Double,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val formatter = DecimalFormat("$#,##0.00")

    Column {
        // 제목 및 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "통합 계좌 조회",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                if (hasApiKeys) {
                    Text(
                        if (isTestnet) "🧪 테스트넷" else "🔴 메인넷",
                        fontSize = 12.sp,
                        color = if (isTestnet) Color(0xFFFF9800) else Color(0xFFF44336)
                    )
                }
            }

            Row {
                if (hasApiKeys) {
                    IconButton(onClick = onRefreshClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = Color(0xFF2196F3))
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "설정", tint = Color(0xFF2196F3))
                }
            }
        }

        // 자산 요약
        if (hasApiKeys && totalBalanceUSD > 0) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem("총 자산", formatter.format(totalBalanceUSD), Color(0xFF7B1FA2))
                    SummaryItem("Spot", formatter.format(spotTotalUSD), Color(0xFF2E7D32))
                    SummaryItem("Futures", formatter.format(futuresTotalUSD), Color(0xFFC62828))
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun AccountContent(
    accountInfo: AccountInfo,
    futuresAccountInfo: FuturesAccountInfo?,
    spotBalances: List<BalanceInfo>,
    earnBalances: List<BalanceInfo>,
    futuresBalances: List<BalanceInfo>,
    spotTotalUSD: Double,
    earnTotalUSD: Double,
    futuresTotalUSD: Double
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column {
        // 탭 메뉴
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2)
            ) {
                val tabs = listOf(
                    "Spot" to spotBalances.size,
                    "Earn" to earnBalances.size,
                    "Futures" to futuresBalances.size
                )

                tabs.forEachIndexed { index, (title, count) ->
                    Tab(
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                                if (count > 0) {
                                    Text(
                                        "($count)",
                                        fontSize = 10.sp,
                                        color = if (selectedTab == index) Color(0xFF1976D2) else Color.Gray
                                    )
                                }
                            }
                        },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = Color(0xFF1976D2),
                        unselectedContentColor = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 탭 컨텐츠
        when (selectedTab) {
            0 -> BalanceContent("Spot 자산", spotBalances, spotTotalUSD, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
            1 -> BalanceContent("Earn 자산", earnBalances, earnTotalUSD, Color(0xFFE8F5E8), Color(0xFF2E7D32))
            2 -> FuturesContent(futuresBalances, futuresTotalUSD, futuresAccountInfo)
        }
    }
}

@Composable
private fun BalanceContent(
    title: String,
    balances: List<BalanceInfo>,
    totalUSD: Double,
    cardColor: Color,
    titleColor: Color
) {
    val formatter = DecimalFormat("$#,##0.00")

    if (balances.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$title (${balances.size}개)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )

                    if (totalUSD > 0) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))) {
                            Text(
                                "총 ${formatter.format(totalUSD)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(balances) { balance -> BalanceRow(balance) }
                }
            }
        }
    } else {
        EmptyCard("${title}이 없습니다", if (title.contains("Spot")) "💰" else if (title.contains("Earn")) "🌱" else "📈")
    }
}

@Composable
private fun FuturesContent(
    futuresBalances: List<BalanceInfo>,
    futuresTotalUSD: Double,
    futuresAccountInfo: FuturesAccountInfo?
) {
    Column {
        // Futures 계좌 요약
        if (futuresAccountInfo != null) {
            FuturesSummaryCard(futuresAccountInfo)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Futures 자산
        BalanceContent("Futures 자산", futuresBalances, futuresTotalUSD, Color(0xFFFFEBEE), Color(0xFFC62828))

        // 포지션 정보
        if (futuresAccountInfo?.positions?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(8.dp))
            PositionsCard(futuresAccountInfo.positions)
        }
    }
}

@Composable
private fun FuturesSummaryCard(futuresInfo: FuturesAccountInfo) {
    val formatter = DecimalFormat("$#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📈 Futures 계좌 요약",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("총 잔고", formatter.format(futuresInfo.totalWalletBalance), Color(0xFF2E7D32))
                SummaryItem(
                    "미실현 PnL",
                    formatter.format(futuresInfo.totalUnrealizedProfit),
                    if (futuresInfo.totalUnrealizedProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                SummaryItem("사용가능", formatter.format(futuresInfo.availableBalance), Color(0xFF1976D2))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 계좌 상태
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusChip("거래", futuresInfo.canTrade)
                StatusChip("입금", futuresInfo.canDeposit)
                StatusChip("출금", futuresInfo.canWithdraw)
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, enabled: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        )
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) Color(0xFF2E7D32) else Color(0xFFC62828),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PositionsCard(positions: List<FuturesPositionInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📊 활성 포지션 (${positions.size}개)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(positions) { position -> PositionRow(position) }
            }
        }
    }
}

@Composable
private fun PositionRow(position: FuturesPositionInfo) {
    val formatter = DecimalFormat("#,##0.########")
    val usdFormatter = DecimalFormat("$#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 심볼과 방향
            Column(modifier = Modifier.weight(2f)) {
                Text(
                    position.symbol,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Row {
                    Text(
                        position.positionSide,
                        fontSize = 10.sp,
                        color = if (position.positionSide == "LONG") Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${position.leverage}x",
                        fontSize = 10.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 포지션 정보
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(3f)
            ) {
                Text(
                    formatter.format(abs(position.positionAmt)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    usdFormatter.format(position.unrealizedProfit),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (position.unrealizedProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                if (position.entryPrice > 0) {
                    Text(
                        "@${formatter.format(position.entryPrice)}",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(balance: BalanceInfo) {
    val formatter = DecimalFormat("#,##0.########")
    val totalAmount = BalanceUtils.getTotalBalance(balance)
    val lockedAmount = balance.locked.toDoubleOrNull() ?: 0.0
    val freeAmount = balance.free.toDoubleOrNull() ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 코인 정보
            Column(modifier = Modifier.weight(2f)) {
                Text(
                    balance.asset,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            // 잔고 정보
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(3f)
            ) {
                Text(
                    formatter.format(totalAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                if (freeAmount > 0) {
                    Text(
                        "자유: ${formatter.format(freeAmount)}",
                        fontSize = 10.sp,
                        color = Color(0xFF388E3C)
                    )
                }

                if (lockedAmount > 0) {
                    Text(
                        "잠김: ${formatter.format(lockedAmount)}",
                        fontSize = 10.sp,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String, emoji: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(message, fontSize = 16.sp, color = Color(0xFFE65100))
            }
        }
    }
}

@Composable
private fun NoApiKeyContentSection(onShowSecuritySettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
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
                    "실제 계좌 정보를 조회하려면\n바이낸스 API 키를 설정해주세요",
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
                    Text("API 키 설정", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LoadingContentSection() {
    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(16.dp))
            Text("계좌 정보를 불러오는 중...", fontSize = 16.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun ErrorContentSection(errorMessage: String, onRetryClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "계좌 정보 로드 실패",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage,
                fontSize = 14.sp,
                color = Color(0xFFD32F2F),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("다시 시도")
            }
        }
    }
}

@Composable
private fun EmptyContentSection() {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("계좌 정보를 불러올 수 없습니다", fontSize = 16.sp, color = Color.Gray)
    }
}