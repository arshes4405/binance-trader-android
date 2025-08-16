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

@Composable
fun AccountBalanceScreen(
    modifier: Modifier = Modifier,
    onShowSecuritySettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val apiKeyService = remember { ApiKeyService(context) }
    val accountService = remember { AccountService() }
    val coroutineScope = rememberCoroutineScope()

    // 상태 변수들
    var hasApiKeys by remember { mutableStateOf(false) }
    var isTestnet by remember { mutableStateOf(true) }
    var apiKeyData by remember { mutableStateOf<ApiKeyData?>(null) }

    // 실제 데이터 상태
    var accountInfo by remember { mutableStateOf<AccountInfo?>(null) }
    var balances by remember { mutableStateOf<List<BalanceInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalBalanceUSD by remember { mutableStateOf(0.0) }

    // 탭별 자산 분류
    var spotBalances by remember { mutableStateOf<List<BalanceInfo>>(emptyList()) }
    var earnBalances by remember { mutableStateOf<List<BalanceInfo>>(emptyList()) }
    var futuresBalances by remember { mutableStateOf<List<BalanceInfo>>(emptyList()) }
    var spotTotalUSD by remember { mutableStateOf(0.0) }
    var earnTotalUSD by remember { mutableStateOf(0.0) }
    var futuresTotalUSD by remember { mutableStateOf(0.0) }

    // API 키 상태 확인 및 계좌 정보 로드
    LaunchedEffect(Unit) {
        val keys = apiKeyService.getApiKeys()
        hasApiKeys = keys != null
        apiKeyData = keys

        if (keys != null) {
            isTestnet = keys.isTestnet
            loadAccountData(accountService, keys) { account, balanceList, total, error ->
                accountInfo = account
                balances = balanceList
                totalBalanceUSD = total
                errorMessage = error
                isLoading = false

                // 탭별 자산 분류
                classifyBalancesByTab(balanceList) { spot, earn, futures, spotTotal, earnTotal, futuresTotal ->
                    spotBalances = spot
                    earnBalances = earn
                    futuresBalances = futures
                    spotTotalUSD = spotTotal
                    earnTotalUSD = earnTotal
                    futuresTotalUSD = futuresTotal
                }
            }
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
            // 상단 헤더
            AccountBalanceHeader(
                hasApiKeys = hasApiKeys,
                isTestnet = isTestnet,
                totalBalanceUSD = totalBalanceUSD,
                onRefreshClick = {
                    if (apiKeyData != null) {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            loadAccountData(accountService, apiKeyData!!) { account, balanceList, total, error ->
                                accountInfo = account
                                balances = balanceList
                                totalBalanceUSD = total
                                errorMessage = error
                                isLoading = false

                                // 탭별 자산 재분류
                                classifyBalancesByTab(balanceList) { spot, earn, futures, spotTotal, earnTotal, futuresTotal ->
                                    spotBalances = spot
                                    earnBalances = earn
                                    futuresBalances = futures
                                    spotTotalUSD = spotTotal
                                    earnTotalUSD = earnTotal
                                    futuresTotalUSD = futuresTotal
                                }
                            }
                        }
                    }
                },
                onSettingsClick = onShowSecuritySettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 컨텐츠 영역
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
                        onRetryClick = {
                            if (apiKeyData != null) {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    loadAccountData(accountService, apiKeyData!!) { account, balanceList, total, error ->
                                        accountInfo = account
                                        balances = balanceList
                                        totalBalanceUSD = total
                                        errorMessage = error
                                        isLoading = false

                                        classifyBalancesByTab(balanceList) { spot, earn, futures, spotTotal, earnTotal, futuresTotal ->
                                            spotBalances = spot
                                            earnBalances = earn
                                            futuresBalances = futures
                                            spotTotalUSD = spotTotal
                                            earnTotalUSD = earnTotal
                                            futuresTotalUSD = futuresTotal
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                accountInfo != null -> {
                    EnhancedAccountContentSection(
                        accountInfo = accountInfo!!,
                        spotBalances = spotBalances,
                        earnBalances = earnBalances,
                        futuresBalances = futuresBalances,
                        spotTotalUSD = spotTotalUSD,
                        earnTotalUSD = earnTotalUSD,
                        futuresTotalUSD = futuresTotalUSD,
                        isTestnet = isTestnet
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
private fun AccountBalanceHeader(
    hasApiKeys: Boolean,
    isTestnet: Boolean,
    totalBalanceUSD: Double,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val formatter = DecimalFormat("$#,##0.00")

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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasApiKeys && totalBalanceUSD > 0) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "전체 자산",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        formatter.format(totalBalanceUSD),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 새로고침 버튼
            if (hasApiKeys) {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = Color(0xFF2196F3)
                    )
                }
            }

            // 설정 버튼
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun EnhancedAccountContentSection(
    accountInfo: AccountInfo,
    spotBalances: List<BalanceInfo>,
    earnBalances: List<BalanceInfo>,
    futuresBalances: List<BalanceInfo>,
    spotTotalUSD: Double,
    earnTotalUSD: Double,
    futuresTotalUSD: Double,
    isTestnet: Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column {
        // 깔끔한 탭 메뉴 (제목만)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2)
            ) {
                val tabTitles = listOf("Spot", "Earn", "Futures")

                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
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

        // 탭별 컨텐츠
        when (selectedTab) {
            0 -> EnhancedSpotBalanceContent(spotBalances, spotTotalUSD)
            1 -> EnhancedEarnBalanceContent(earnBalances, earnTotalUSD)
            2 -> EnhancedFuturesBalanceContent(futuresBalances, futuresTotalUSD)
        }
    }
}

@Composable
private fun EnhancedBalanceListCard(
    title: String,
    balances: List<BalanceInfo>,
    totalUSD: Double,
    cardColor: Color = Color.White,
    titleColor: Color = Color(0xFF1976D2)
) {
    val formatter = DecimalFormat("$#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 헤더 (제목 + 총 금액)
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
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E8)
                        )
                    ) {
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

            if (balances.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(balances) { balance ->
                        EnhancedBalanceRowComponent(balance)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedSpotBalanceContent(balances: List<BalanceInfo>, totalUSD: Double) {
    if (balances.isNotEmpty()) {
        EnhancedBalanceListCard(
            title = "Spot 자산",
            balances = balances,
            totalUSD = totalUSD,
            cardColor = Color(0xFFF3E5F5),
            titleColor = Color(0xFF7B1FA2)
        )
    } else {
        EmptyBalanceCard("Spot 자산이 없습니다", "💰")
    }
}

@Composable
private fun EnhancedEarnBalanceContent(balances: List<BalanceInfo>, totalUSD: Double) {
    if (balances.isNotEmpty()) {
        EnhancedBalanceListCard(
            title = "Earn 자산",
            balances = balances,
            totalUSD = totalUSD,
            cardColor = Color(0xFFE8F5E8),
            titleColor = Color(0xFF2E7D32)
        )
    } else {
        EmptyBalanceCard("Earn 자산이 없습니다", "🌱")
    }
}

@Composable
private fun EnhancedFuturesBalanceContent(balances: List<BalanceInfo>, totalUSD: Double) {
    if (balances.isNotEmpty()) {
        EnhancedBalanceListCard(
            title = "Futures 자산",
            balances = balances,
            totalUSD = totalUSD,
            cardColor = Color(0xFFFFEBEE),
            titleColor = Color(0xFFC62828)
        )
    } else {
        EmptyBalanceCard("Futures 자산이 없습니다", "📈")
    }
}

@Composable
private fun EmptyBalanceCard(message: String, emoji: String = "💼") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    emoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    fontSize = 16.sp,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
private fun EnhancedBalanceRowComponent(balance: BalanceInfo) {
    val formatter = DecimalFormat("#,##0.########")
    val totalAmount = BalanceUtils.getTotalBalance(balance)
    val lockedAmount = balance.locked.toDoubleOrNull() ?: 0.0
    val freeAmount = balance.free.toDoubleOrNull() ?: 0.0

    // 가상의 USD 가격 (실제로는 API에서 가져와야 함)
    val usdValue = calculateUSDValue(balance.asset, totalAmount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 코인 정보
            Column(
                modifier = Modifier.weight(2f)
            ) {
                Text(
                    balance.asset,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                if (usdValue > 0) {
                    Text(
                        "$${String.format("%.2f", usdValue)}",
                        fontSize = 10.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            // 잔고 정보
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(3f)
            ) {
                // 총 잔고
                Text(
                    formatter.format(totalAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                // 자유 자산
                if (freeAmount > 0) {
                    Text(
                        "자유: ${formatter.format(freeAmount)}",
                        fontSize = 10.sp,
                        color = Color(0xFF388E3C)
                    )
                }

                // 잠긴 자산
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

// 기존 컴포넌트들 (변경 없음)
@Composable
private fun NoApiKeyContentSection(onShowSecuritySettings: () -> Unit) {
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
private fun LoadingContentSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF2196F3)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "계좌 정보를 불러오는 중...",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ErrorContentSection(
    errorMessage: String,
    onRetryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "계좌 정보를 불러올 수 없습니다",
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

// 데이터 클래스 (사용하지 않으므로 제거)

// 헬퍼 함수들
private fun classifyBalancesByTab(
    balances: List<BalanceInfo>,
    callback: (List<BalanceInfo>, List<BalanceInfo>, List<BalanceInfo>, Double, Double, Double) -> Unit
) {
    val nonZeroBalances = BalanceUtils.getNonZeroBalances(balances)

    // Spot 자산 (일반 거래 자산)
    val spotAssets = listOf("BTC", "ETH", "BNB", "ADA", "DOT", "LINK", "LTC", "XRP", "DOGE")
    val spotBalances = nonZeroBalances.filter { it.asset in spotAssets }

    // Earn 자산 (스테이킹 가능한 자산)
    val earnAssets = listOf("BNB", "USDT", "BUSD", "ETH", "ADA", "DOT")
    val earnBalances = nonZeroBalances.filter {
        it.asset in earnAssets && BalanceUtils.getTotalBalance(it) >= 10.0 // 최소 금액 조건
    }

    // Futures 자산 (선물 거래용 마진)
    val futuresAssets = listOf("USDT", "BUSD", "USDC")
    val futuresBalances = nonZeroBalances.filter { it.asset in futuresAssets }

    // 각 탭별 USD 총액 계산
    val spotTotalUSD = calculateTotalUSDForBalances(spotBalances)
    val earnTotalUSD = calculateTotalUSDForBalances(earnBalances)
    val futuresTotalUSD = calculateTotalUSDForBalances(futuresBalances)

    callback(spotBalances, earnBalances, futuresBalances, spotTotalUSD, earnTotalUSD, futuresTotalUSD)
}

private fun calculateTotalUSDForBalances(balances: List<BalanceInfo>): Double {
    return balances.sumOf { balance ->
        val totalAmount = BalanceUtils.getTotalBalance(balance)
        calculateUSDValue(balance.asset, totalAmount)
    }
}

private fun calculateUSDValue(asset: String, amount: Double): Double {
    // 실제로는 바이낸스 API에서 현재 가격을 가져와야 하지만,
    // 여기서는 가상의 가격을 사용합니다
    val prices = mapOf(
        "USDT" to 1.0,
        "BUSD" to 1.0,
        "USDC" to 1.0,
        "BTC" to 42000.0,
        "ETH" to 2500.0,
        "BNB" to 300.0,
        "ADA" to 0.5,
        "DOT" to 7.0,
        "LINK" to 15.0,
        "LTC" to 70.0,
        "XRP" to 0.6,
        "DOGE" to 0.08
    )

    return (prices[asset] ?: 0.0) * amount
}

// 계좌 데이터 로드 함수 (기존과 동일)
private suspend fun loadAccountData(
    accountService: AccountService,
    apiKeyData: ApiKeyData,
    callback: (AccountInfo?, List<BalanceInfo>, Double, String?) -> Unit
) {
    try {
        val accountResponse = accountService.getAccountInfo(apiKeyData)

        if (accountResponse.success && accountResponse.data != null) {
            val accountInfo = accountResponse.data
            val nonZeroBalances = BalanceUtils.getNonZeroBalances(accountInfo.balances)
            val totalUSD = calculateTotalUSDForBalances(nonZeroBalances)

            callback(accountInfo, nonZeroBalances, totalUSD, null)
        } else {
            callback(null, emptyList(), 0.0, accountResponse.message)
        }
    } catch (e: Exception) {
        callback(null, emptyList(), 0.0, "네트워크 오류: ${e.message}")
    }
}