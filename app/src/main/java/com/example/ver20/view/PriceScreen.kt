// 업데이트된 시세 조회 화면 - 코인 추가 및 새로고침 버튼 개선

package com.example.ver20.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.ver20.dao.UserService
import com.example.ver20.dao.UserData
import com.example.ver20.dao.MongoDbService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.abs

// ===== 데이터 모델 =====

data class TechnicalIndicatorData(
    val timestamp: Long,
    val cciValue: Double,
    val rsiValue: Double
)

data class CoinIndicatorInfo(
    val symbol: String,
    val displayName: String,
    val currentPrice: Double = 0.0,
    val priceChange24h: Double = 0.0,
    val min15: TechnicalIndicatorData?,
    val hour1: TechnicalIndicatorData?,
    val hour4: TechnicalIndicatorData?,
    val day1: TechnicalIndicatorData?,
    val isLoading: Boolean = false
)

// ===== 메인 화면 =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceScreen(
    modifier: Modifier = Modifier,
    onShowCreateAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    var coinIndicators by remember { mutableStateOf(getInitialCoinList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var hasUserInfo by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<UserData?>(null) }
    var showAddCoinDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 즐겨찾기 코인 새로고침 함수
    fun refreshFavoriteCoins() {
        currentUser?.let { userData ->
            val mongoService = MongoDbService()
            mongoService.getFavoriteCoins(userData.username) { symbols, error ->
                if (error == null) {
                    scope.launch {
                        val favoriteCoins = symbols.map { symbol ->
                            CoinIndicatorInfo(
                                symbol = symbol,
                                displayName = getKoreanName(symbol),
                                min15 = null,
                                hour1 = null,
                                hour4 = null,
                                day1 = null,
                                isLoading = true
                            )
                        }
                        coinIndicators = favoriteCoins

                        // 지표 및 가격 정보 업데이트
                        refreshIndicators(coinIndicators) { updated ->
                            coinIndicators = updated
                        }
                    }
                }
            }
        }
    }

    // 사용자 정보 확인 및 즐겨찾기 코인 로드
    LaunchedEffect(Unit) {
        val userService = UserService()
        val userData = userService.getUserFromPreferences(context)
        hasUserInfo = userData != null
        currentUser = userData

        if (hasUserInfo && userData != null) {
            Toast.makeText(context, "안녕하세요, ${userData.username}님!", Toast.LENGTH_SHORT).show()
            refreshFavoriteCoins()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        // ===== 헤더 영역 =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 사용자 정보 표시
                Column {
                    Text(
                        "📊 코인 시세 조회",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (hasUserInfo && currentUser != null) {
                        Text(
                            "환영합니다, ${currentUser!!.username}님",
                            fontSize = 12.sp,
                            color = Color(0xFFE3F2FD)
                        )
                    }
                }

                // 버튼 영역
                if (!hasUserInfo) {
                    Button(
                        onClick = onShowCreateAccount,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Text(
                            "계정생성",
                            color = Color(0xFF2196F3),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 코인 추가 버튼
                        Button(
                            onClick = { showAddCoinDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "코인 추가",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "코인추가",
                                color = Color(0xFF2196F3),
                                fontSize = 11.sp
                            )
                        }

                        // 새로고침 버튼
                        Button(
                            onClick = {
                                scope.launch {
                                    isRefreshing = true
                                    refreshIndicators(coinIndicators) { updated ->
                                        coinIndicators = updated
                                        isRefreshing = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFF2196F3),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "새로고침",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isRefreshing) "업데이트중" else "새로고침",
                                color = Color(0xFF2196F3),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 코인 목록 또는 안내 메시지 =====
        if (coinIndicators.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Text(
                        modifier = Modifier.padding(20.dp),
                        text = if (hasUserInfo) {
                            "즐겨찾기 코인이 없습니다.\n'코인추가' 버튼을 눌러 코인을 추가해보세요!"
                        } else {
                            "로그인 후 즐겨찾기 코인을\n등록하실 수 있습니다."
                        },
                        fontSize = 14.sp,
                        color = Color(0xFFE65100),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 코인 목록
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(coinIndicators) { coin ->
                CoinIndicatorCard(
                    coin = coin,
                    onRefresh = {
                        scope.launch {
                            refreshSingleCoin(coin) { updated ->
                                coinIndicators = coinIndicators.map {
                                    if (it.symbol == updated.symbol) updated else it
                                }
                            }
                        }
                    },
                    onRemove = {
                        // 즐겨찾기에서 코인 삭제
                        currentUser?.let { userData ->
                            val mongoService = MongoDbService()
                            mongoService.removeFavoriteCoin(userData.username, coin.symbol) { success, message ->
                                scope.launch {
                                    if (success) {
                                        Toast.makeText(context, "코인이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                                        refreshFavoriteCoins()
                                    } else {
                                        Toast.makeText(context, message ?: "삭제 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    // 코인 추가 다이얼로그
    if (showAddCoinDialog) {
        AddCoinDialog(
            onDismiss = { showAddCoinDialog = false },
            onConfirm = { symbol ->
                currentUser?.let { userData ->
                    val mongoService = MongoDbService()
                    mongoService.saveFavoriteCoin(userData.username, symbol) { success, message ->
                        scope.launch {
                            if (success) {
                                Toast.makeText(context, "코인이 추가되었습니다", Toast.LENGTH_SHORT).show()
                                refreshFavoriteCoins()
                            } else {
                                Toast.makeText(context, message ?: "추가 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                showAddCoinDialog = false
            }
        )
    }
}

// ===== 코인 지표 카드 (삭제 버튼 추가) =====

@Composable
fun CoinIndicatorCard(
    coin: CoinIndicatorInfo,
    onRefresh: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRefresh() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0F4F8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 헤더 (코인명 + 현재가 + 삭제 버튼)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        getCoinEmoji(coin.symbol),
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            coin.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            coin.symbol,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        if (coin.currentPrice > 0) {
                            Text(
                                "$${String.format("%.4f", coin.currentPrice)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val changeColor = if (coin.priceChange24h >= 0) {
                                Color(0xFF4CAF50)
                            } else {
                                Color(0xFFF44336)
                            }

                            Text(
                                "${if (coin.priceChange24h >= 0) "+" else ""}${String.format("%.2f", coin.priceChange24h)}%",
                                fontSize = 12.sp,
                                color = changeColor,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "로딩중...",
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 삭제 버튼
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "삭제",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 지표 테이블 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Start
                )
                Text(
                    "15분",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "1시간",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "4시간",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "1일",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (coin.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF2196F3)
                    )
                }
            } else {
                // CCI 행
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CCI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Start
                    )
                    TableIndicatorCell(coin.min15?.cciValue, true, Modifier.weight(1f))
                    TableIndicatorCell(coin.hour1?.cciValue, true, Modifier.weight(1f))
                    TableIndicatorCell(coin.hour4?.cciValue, true, Modifier.weight(1f))
                    TableIndicatorCell(coin.day1?.cciValue, true, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // RSI 행
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RSI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Start
                    )
                    TableIndicatorCell(coin.min15?.rsiValue, false, Modifier.weight(1f))
                    TableIndicatorCell(coin.hour1?.rsiValue, false, Modifier.weight(1f))
                    TableIndicatorCell(coin.hour4?.rsiValue, false, Modifier.weight(1f))
                    TableIndicatorCell(coin.day1?.rsiValue, false, Modifier.weight(1f))
                }
            }
        }
    }
}

// ===== 지표 셀 컴포저블 =====

@Composable
fun TableIndicatorCell(
    value: Double?,
    isCCI: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor: Color
    val textColor: Color
    val displayValue: String

    if (value == null) {
        backgroundColor = Color(0xFFEEEEEE)
        textColor = Color(0xFF999999)
        displayValue = "-"
    } else {
        displayValue = String.format("%.1f", value)

        if (isCCI) {
            // CCI 색상 로직
            when {
                value >= 100 -> {
                    backgroundColor = Color(0xFFE57373) // 빨강 (과매수)
                    textColor = Color.White
                }
                value <= -100 -> {
                    backgroundColor = Color(0xFF81C784) // 초록 (과매도)
                    textColor = Color.White
                }
                else -> {
                    backgroundColor = Color(0xFFE0E0E0) // 회색 (중립)
                    textColor = Color.Black
                }
            }
        } else {
            // RSI 색상 로직
            when {
                value >= 70 -> {
                    backgroundColor = Color(0xFFE57373) // 빨강 (과매수)
                    textColor = Color.White
                }
                value <= 30 -> {
                    backgroundColor = Color(0xFF81C784) // 초록 (과매도)
                    textColor = Color.White
                }
                else -> {
                    backgroundColor = Color(0xFFE0E0E0) // 회색 (중립)
                    textColor = Color.Black
                }
            }
        }
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            displayValue,
            fontSize = 10.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ===== 코인 추가 다이얼로그 =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCoinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedSymbol by remember { mutableStateOf("") }

    // 인기 코인 목록
    val popularCoins = listOf(
        "BTCUSDT" to "비트코인",
        "ETHUSDT" to "이더리움",
        "BNBUSDT" to "바이낸스코인",
        "XRPUSDT" to "리플",
        "ADAUSDT" to "에이다",
        "DOGEUSDT" to "도지코인",
        "SOLUSDT" to "솔라나",
        "DOTUSDT" to "폴카닷",
        "MATICUSDT" to "폴리곤",
        "LTCUSDT" to "라이트코인",
        "AVAXUSDT" to "아발란체",
        "LINKUSDT" to "체인링크",
        "UNIUSDT" to "유니스왑",
        "ATOMUSDT" to "코스모스",
        "FILUSDT" to "파일코인"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "즐겨찾기 코인 추가",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "추가할 코인을 선택해주세요:",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(popularCoins) { (symbol, name) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSymbol = symbol
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedSymbol == symbol) {
                                    Color(0xFFE3F2FD)
                                } else {
                                    Color(0xFFFAFAFA)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    getCoinEmoji(symbol),
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        symbol,
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedSymbol.isNotEmpty()) {
                        onConfirm(selectedSymbol)
                    }
                },
                enabled = selectedSymbol.isNotEmpty()
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// ===== 유틸리티 함수들 =====

private fun getKoreanName(baseSymbol: String): String {
    return when (baseSymbol.replace("USDT", "")) {
        "BTC" -> "비트코인"
        "ETH" -> "이더리움"
        "BNB" -> "바이낸스코인"
        "XRP" -> "리플"
        "ADA" -> "에이다"
        "DOGE" -> "도지코인"
        "SOL" -> "솔라나"
        "DOT" -> "폴카닷"
        "MATIC" -> "폴리곤"
        "LTC" -> "라이트코인"
        "AVAX" -> "아발란체"
        "LINK" -> "체인링크"
        "UNI" -> "유니스왑"
        "ATOM" -> "코스모스"
        "FIL" -> "파일코인"
        else -> baseSymbol
    }
}

private fun getCoinEmoji(symbol: String): String {
    return when (symbol) {
        "BTCUSDT" -> "₿"
        "ETHUSDT" -> "Ξ"
        "BNBUSDT" -> "🔶"
        "XRPUSDT" -> "🌊"
        "ADAUSDT" -> "💎"
        "DOGEUSDT" -> "🐕"
        "SOLUSDT" -> "☀️"
        "DOTUSDT" -> "⚫"
        "MATICUSDT" -> "🔷"
        "LTCUSDT" -> "🥈"
        "AVAXUSDT" -> "🏔️"
        "LINKUSDT" -> "🔗"
        "UNIUSDT" -> "🦄"
        "ATOMUSDT" -> "⚛️"
        "FILUSDT" -> "📁"
        else -> "💰"
    }
}

// 초기 코인 목록 (빈 목록으로 시작)
private fun getInitialCoinList(): List<CoinIndicatorInfo> = emptyList()

// 가격 및 지표 정보 새로고침 함수들
private suspend fun refreshIndicators(
    coins: List<CoinIndicatorInfo>,
    onUpdate: (List<CoinIndicatorInfo>) -> Unit
) {
    val updated = coins.map { coin ->
        try {
            // 현재가격과 24시간 변동률 가져오기
            val priceInfo = fetchCurrentPrice(coin.symbol)

            // 실제 API 호출로 가격 데이터 가져오기
            val priceData15m = fetchPriceData(coin.symbol, "15m")
            val priceData1h = fetchPriceData(coin.symbol, "1h")
            val priceData4h = fetchPriceData(coin.symbol, "4h")
            val priceData1d = fetchPriceData(coin.symbol, "1d")

            val cci15m = calculateCCI(priceData15m)
            val rsi15m = calculateRSI(priceData15m, 7) // RSI 7 기간으로 복원
            val cci1h = calculateCCI(priceData1h)
            val rsi1h = calculateRSI(priceData1h, 7) // RSI 7 기간으로 복원
            val cci4h = calculateCCI(priceData4h)
            val rsi4h = calculateRSI(priceData4h, 7) // RSI 7 기간으로 복원
            val cci1d = calculateCCI(priceData1d)
            val rsi1d = calculateRSI(priceData1d, 7) // RSI 7 기간으로 복원

            coin.copy(
                currentPrice = priceInfo.first,
                priceChange24h = priceInfo.second,
                min15 = TechnicalIndicatorData(System.currentTimeMillis(), cci15m, rsi15m),
                hour1 = TechnicalIndicatorData(System.currentTimeMillis(), cci1h, rsi1h),
                hour4 = TechnicalIndicatorData(System.currentTimeMillis(), cci4h, rsi4h),
                day1 = TechnicalIndicatorData(System.currentTimeMillis(), cci1d, rsi1d),
                isLoading = false
            )
        } catch (e: Exception) {
            coin.copy(isLoading = false)
        }
    }
    onUpdate(updated)
}

private suspend fun refreshSingleCoin(
    coin: CoinIndicatorInfo,
    onUpdate: (CoinIndicatorInfo) -> Unit
) {
    try {
        val priceInfo = fetchCurrentPrice(coin.symbol)

        val priceData15m = fetchPriceData(coin.symbol, "15m")
        val priceData1h = fetchPriceData(coin.symbol, "1h")
        val priceData4h = fetchPriceData(coin.symbol, "4h")
        val priceData1d = fetchPriceData(coin.symbol, "1d")

        val cci15m = calculateCCI(priceData15m)
        val rsi15m = calculateRSI(priceData15m, 7) // RSI 7 기간으로 복원
        val cci1h = calculateCCI(priceData1h)
        val rsi1h = calculateRSI(priceData1h, 7) // RSI 7 기간으로 복원
        val cci4h = calculateCCI(priceData4h)
        val rsi4h = calculateRSI(priceData4h, 7) // RSI 7 기간으로 복원
        val cci1d = calculateCCI(priceData1d)
        val rsi1d = calculateRSI(priceData1d, 7) // RSI 7 기간으로 복원

        val updated = coin.copy(
            currentPrice = priceInfo.first,
            priceChange24h = priceInfo.second,
            min15 = TechnicalIndicatorData(System.currentTimeMillis(), cci15m, rsi15m),
            hour1 = TechnicalIndicatorData(System.currentTimeMillis(), cci1h, rsi1h),
            hour4 = TechnicalIndicatorData(System.currentTimeMillis(), cci4h, rsi4h),
            day1 = TechnicalIndicatorData(System.currentTimeMillis(), cci1d, rsi1d),
            isLoading = false
        )
        onUpdate(updated)
    } catch (e: Exception) {
        onUpdate(coin.copy(isLoading = false))
    }
}

// 가격 정보 가져오기 함수
private suspend fun fetchCurrentPrice(symbol: String): Pair<Double, Double> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.binance.com/api/v3/ticker/24hr?symbol=$symbol")
                .build()

            val response = client.newCall(request).execute()
            val jsonString = response.body?.string() ?: ""

            // JSON 파싱 (간단한 방식)
            val priceRegex = """"lastPrice":"([^"]+)"""".toRegex()
            val changeRegex = """"priceChangePercent":"([^"]+)"""".toRegex()

            val priceMatch = priceRegex.find(jsonString)
            val changeMatch = changeRegex.find(jsonString)

            val price = priceMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val change = changeMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

            Pair(price, change)
        } catch (e: Exception) {
            Pair(0.0, 0.0)
        }
    }
}

// 캔들 데이터 가져오기 함수 (개선된 버전)
private suspend fun fetchPriceData(symbol: String, interval: String): List<Double> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            // limit을 50으로 늘려서 더 많은 데이터 확보
            val request = Request.Builder()
                .url("https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=50")
                .build()

            val response = client.newCall(request).execute()
            val jsonString = response.body?.string() ?: ""

            // JSON 파싱으로 종가만 추출
            val prices = mutableListOf<Double>()

            // JSON 배열 파싱
            val cleanJson = jsonString.trim()
            if (cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
                val jsonContent = cleanJson.substring(1, cleanJson.length - 1)
                val candleArrays = jsonContent.split("],[")

                for (candleStr in candleArrays) {
                    val cleanCandleStr = candleStr.replace("[", "").replace("]", "")
                    val values = cleanCandleStr.split(",")

                    if (values.size >= 5) {
                        // 4번째 인덱스가 종가(close price)
                        val closePrice = values[4].replace("\"", "").toDoubleOrNull()
                        closePrice?.let { prices.add(it) }
                    }
                }
            }

            prices
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// CCI 계산 함수
private fun calculateCCI(prices: List<Double>, period: Int = 20): Double {
    if (prices.size < period) return 0.0

    val recentPrices = prices.takeLast(period)
    val sma = recentPrices.average()
    val meanDeviation = recentPrices.map { abs(it - sma) }.average()

    val currentPrice = prices.last()
    return if (meanDeviation != 0.0) {
        (currentPrice - sma) / (0.015 * meanDeviation)
    } else {
        0.0
    }
}

// RSI 계산 함수 (정확한 표준 공식)
private fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
    if (prices.size < period + 1) return 50.0

    // 가격 변화량 계산
    val priceChanges = mutableListOf<Double>()
    for (i in 1 until prices.size) {
        priceChanges.add(prices[i] - prices[i - 1])
    }

    if (priceChanges.size < period) return 50.0

    // 첫 번째 RS 계산 (단순 평균 방식)
    val firstPeriodChanges = priceChanges.take(period)
    var avgGain = firstPeriodChanges.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0
    var avgLoss = firstPeriodChanges.filter { it < 0 }.map { -it }.average().takeIf { !it.isNaN() } ?: 0.0

    // 이후 값들은 지수 이동평균 방식으로 계산
    for (i in period until priceChanges.size) {
        val change = priceChanges[i]
        val gain = if (change > 0) change else 0.0
        val loss = if (change < 0) -change else 0.0

        // Wilder's smoothing (지수 이동평균)
        avgGain = (avgGain * (period - 1) + gain) / period
        avgLoss = (avgLoss * (period - 1) + loss) / period
    }

    // RSI 계산
    return if (avgLoss == 0.0) {
        100.0
    } else {
        val rs = avgGain / avgLoss
        100.0 - (100.0 / (1.0 + rs))
    }
}