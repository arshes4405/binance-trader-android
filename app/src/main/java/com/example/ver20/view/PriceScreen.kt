// 업데이트된 시세 조회 화면 - CCI/RSI 지표 표시

package com.example.ver20.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
    val scope = rememberCoroutineScope()

    // 사용자 정보 확인 및 즐겨찾기 코인 로드
    LaunchedEffect(Unit) {
        val userService = UserService()
        val userData = userService.getUserFromPreferences(context)
        hasUserInfo = userData != null
        currentUser = userData

        if (hasUserInfo && userData != null) {
            Toast.makeText(context, "안녕하세요, ${userData.username}님!", Toast.LENGTH_SHORT).show()

            // MongoDB에서 사용자별 즐겨찾기 코인 조회
            val mongoService = MongoDbService()
            mongoService.getFavoriteCoins(userData.username) { favoriteSymbols, error ->
                scope.launch {
                    if (error == null && favoriteSymbols.isNotEmpty()) {
                        // 즐겨찾기 코인들을 CoinIndicatorInfo로 변환
                        val favoriteCoins = favoriteSymbols.map { symbol ->
                            val displayName = getDisplayName(symbol)
                            CoinIndicatorInfo(
                                symbol = symbol,
                                displayName = displayName,
                                currentPrice = 0.0,
                                priceChange24h = 0.0,
                                min15 = null,
                                hour1 = null,
                                hour4 = null,
                                day1 = null,
                                isLoading = true
                            )
                        }
                        coinIndicators = favoriteCoins

                        // 즐겨찾기 코인들의 가격 정보 로드
                        refreshIndicators(favoriteCoins) { updated ->
                            coinIndicators = updated
                        }
                    } else {
                        // 즐겨찾기 코인이 없는 경우 빈 목록
                        coinIndicators = emptyList()
                        Toast.makeText(
                            context,
                            "즐겨찾기 코인이 없습니다. 코인을 추가해주세요.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            // 로그인하지 않은 경우 빈 목록
            coinIndicators = emptyList()
        }
    }

    // 자동 새로고침 (30초마다) - 즐겨찾기 코인이 있을 때만
    LaunchedEffect(coinIndicators) {
        if (coinIndicators.isNotEmpty()) {
            while (true) {
                delay(30000) // 30초
                refreshIndicators(coinIndicators) { updated ->
                    coinIndicators = updated
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        // 헤더
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "📊 기술적 지표 시세",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "CCI & RSI 실시간 모니터링",
                        fontSize = 14.sp,
                        color = Color(0xFFE3F2FD)
                    )
                    if (currentUser != null) {
                        Text(
                            "사용자: ${currentUser!!.username}",
                            fontSize = 12.sp,
                            color = Color(0xFFE3F2FD)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Spacer(modifier = Modifier.width(8.dp))
                    }

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
                        )
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF2196F3),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "새로고침",
                                tint = Color(0xFF2196F3)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "새로고침",
                            color = Color(0xFF2196F3),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 즐겨찾기 코인이 없는 경우 안내 메시지
        if (coinIndicators.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "💡 즐겨찾기 코인이 없습니다",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8F00)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (hasUserInfo) {
                            "시세조회 탭에서 코인을 추가하거나\n시세포착 설정을 통해 코인을 등록해주세요."
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    }
                )
            }
        }
    }
}

// ===== 코인 지표 카드 =====

@Composable
fun CoinIndicatorCard(
    coin: CoinIndicatorInfo,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRefresh() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 헤더 (코인명 + 현재가) - 한 줄로 배치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽: 이모지 + 코인명
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        getCoinEmoji(coin.symbol),
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        coin.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                }

                // 오른쪽: 현재가 + 변동률
                if (coin.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2196F3)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            formatPrice(coin.currentPrice),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        if (coin.priceChange24h != 0.0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val isPositive = coin.priceChange24h > 0
                            Text(
                                "${if (isPositive) "+" else ""}${String.format("%.1f", coin.priceChange24h)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 지표 데이터를 한 줄에 표시 (15M, 1H, 4H, 1D)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 15분
                InlineTimeframeIndicator("15M", coin.min15)
                // 1시간
                InlineTimeframeIndicator("1H", coin.hour1)
                // 4시간
                InlineTimeframeIndicator("4H", coin.hour4)
                // 1일
                InlineTimeframeIndicator("1D", coin.day1)
            }
        }
    }
}

// ===== 인라인 시간대별 지표 =====

@Composable
fun InlineTimeframeIndicator(
    timeframe: String,
    data: TechnicalIndicatorData?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 시간대 라벨
        Text(
            timeframe,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666)
        )

        if (data != null) {
            // CCI 칩
            InlineIndicatorChip(
                value = data.cciValue,
                isCci = true
            )

            // RSI 칩
            InlineIndicatorChip(
                value = data.rsiValue,
                isCci = false
            )
        } else {
            // 로딩 중
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "...",
                    fontSize = 8.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

// ===== 인라인 지표 칩 (더 작음) =====

@Composable
fun InlineIndicatorChip(
    value: Double,
    isCci: Boolean
) {
    val (bgColor, textColor) = if (isCci) {
        when {
            value > 100 -> Pair(Color(0xFFFFCDD2), Color(0xFFD32F2F))
            value < -100 -> Pair(Color(0xFFC8E6C9), Color(0xFF388E3C))
            else -> Pair(Color(0xFFE0E0E0), Color(0xFF424242))
        }
    } else { // RSI
        when {
            value > 70 -> Pair(Color(0xFFFFCDD2), Color(0xFFD32F2F))
            value < 30 -> Pair(Color(0xFFC8E6C9), Color(0xFF388E3C))
            else -> Pair(Color(0xFFE0E0E0), Color(0xFF424242))
        }
    }

    Box(
        modifier = Modifier
            .background(
                bgColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            String.format("%.0f", value),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// ===== 유틸리티 함수들 =====

private fun getDisplayName(symbol: String): String {
    val baseSymbol = symbol.replace("USDT", "")
    return when (baseSymbol) {
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
        "BNBUSDT" -> "🟡"
        "XRPUSDT" -> "🌊"
        "ADAUSDT" -> "🔷"
        "DOGEUSDT" -> "🐕"
        "SOLUSDT" -> "☀️"
        "DOTUSDT" -> "⚫"
        "MATICUSDT" -> "🟣"
        "LTCUSDT" -> "🥈"
        "AVAXUSDT" -> "❄️"
        "LINKUSDT" -> "🔗"
        "UNIUSDT" -> "🦄"
        "ATOMUSDT" -> "⚛️"
        "FILUSDT" -> "📁"
        else -> "💎"
    }
}

private fun getInitialCoinList(): List<CoinIndicatorInfo> {
    // 이제 빈 목록으로 시작 (사용자별 즐겨찾기에서 로드)
    return emptyList()
}

// ===== 지표 계산 함수들 =====

private suspend fun calculateCCI(priceData: List<PriceCandle>, period: Int = 20): Double {
    if (priceData.size < period) return 0.0

    val recentData = priceData.takeLast(period)
    val typicalPrices = recentData.map { (it.high + it.low + it.close) / 3.0 }
    val sma = typicalPrices.average()
    val meanDeviation = typicalPrices.map { abs(it - sma) }.average()

    val currentTypical = typicalPrices.last()
    return if (meanDeviation != 0.0) {
        (currentTypical - sma) / (0.015 * meanDeviation)
    } else {
        0.0
    }
}

private suspend fun calculateRSI(priceData: List<PriceCandle>, period: Int = 14): Double {
    if (priceData.size < period + 1) return 50.0

    val changes = mutableListOf<Double>()
    for (i in 1 until priceData.size) {
        changes.add(priceData[i].close - priceData[i-1].close)
    }

    val recentChanges = changes.takeLast(period)
    val gains = recentChanges.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0
    val losses = recentChanges.filter { it < 0 }.map { abs(it) }.average().takeIf { !it.isNaN() } ?: 0.0

    return if (losses == 0.0) {
        100.0
    } else {
        val rs = gains / losses
        100.0 - (100.0 / (1.0 + rs))
    }
}

// ===== 데이터 새로고침 함수들 =====

private suspend fun refreshIndicators(
    current: List<CoinIndicatorInfo>,
    onUpdate: (List<CoinIndicatorInfo>) -> Unit
) {
    val updated = current.map { coin ->
        try {
            // 현재가격과 24시간 변동률 가져오기
            val priceInfo = fetchCurrentPrice(coin.symbol)

            // 실제 API 호출로 가격 데이터 가져오기
            val priceData15m = fetchPriceData(coin.symbol, "15m")
            val priceData1h = fetchPriceData(coin.symbol, "1h")
            val priceData4h = fetchPriceData(coin.symbol, "4h")
            val priceData1d = fetchPriceData(coin.symbol, "1d")

            val cci15m = calculateCCI(priceData15m)
            val rsi15m = calculateRSI(priceData15m)
            val cci1h = calculateCCI(priceData1h)
            val rsi1h = calculateRSI(priceData1h)
            val cci4h = calculateCCI(priceData4h)
            val rsi4h = calculateRSI(priceData4h)
            val cci1d = calculateCCI(priceData1d)
            val rsi1d = calculateRSI(priceData1d)

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
            // 에러 발생시 현재 데이터 유지하고 로딩 상태만 해제
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
        val updated = coin.copy(isLoading = true)
        onUpdate(updated)

        delay(500) // 로딩 효과

        // 현재가격과 변동률 가져오기
        val priceInfo = fetchCurrentPrice(coin.symbol)

        // 실제 데이터 가져오기
        val priceData15m = fetchPriceData(coin.symbol, "15m")
        val priceData1h = fetchPriceData(coin.symbol, "1h")
        val priceData4h = fetchPriceData(coin.symbol, "4h")
        val priceData1d = fetchPriceData(coin.symbol, "1d")

        val cci15m = calculateCCI(priceData15m)
        val rsi15m = calculateRSI(priceData15m)
        val cci1h = calculateCCI(priceData1h)
        val rsi1h = calculateRSI(priceData1h)
        val cci4h = calculateCCI(priceData4h)
        val rsi4h = calculateRSI(priceData4h)
        val cci1d = calculateCCI(priceData1d)
        val rsi1d = calculateRSI(priceData1d)

        onUpdate(coin.copy(
            currentPrice = priceInfo.first,
            priceChange24h = priceInfo.second,
            min15 = TechnicalIndicatorData(System.currentTimeMillis(), cci15m, rsi15m),
            hour1 = TechnicalIndicatorData(System.currentTimeMillis(), cci1h, rsi1h),
            hour4 = TechnicalIndicatorData(System.currentTimeMillis(), cci4h, rsi4h),
            day1 = TechnicalIndicatorData(System.currentTimeMillis(), cci1d, rsi1d),
            isLoading = false
        ))
    } catch (e: Exception) {
        onUpdate(coin.copy(isLoading = false))
    }
}

// ===== 가격 데이터 구조체 =====

data class PriceCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

// ===== 현재가격 정보 =====

data class PriceInfo(
    val price: Double,
    val change24h: Double
)

// ===== 현재가격 조회 =====

private suspend fun fetchCurrentPrice(symbol: String): Pair<Double, Double> {
    // 실제 구현에서는 바이낸스 24hr ticker API를 호출해야 함
    delay(100) // API 호출 지연 시뮬레이션

    val basePrice = when (symbol) {
        "BTCUSDT" -> 43000.0
        "ETHUSDT" -> 2600.0
        "BNBUSDT" -> 315.0
        "XRPUSDT" -> 0.62
        "ADAUSDT" -> 0.48
        "DOGEUSDT" -> 0.083
        "SOLUSDT" -> 98.5
        "DOTUSDT" -> 7.2
        "MATICUSDT" -> 0.89
        "LTCUSDT" -> 72.5
        "AVAXUSDT" -> 36.8
        "LINKUSDT" -> 14.2
        "UNIUSDT" -> 6.8
        "ATOMUSDT" -> 9.4
        "FILUSDT" -> 5.2
        else -> 100.0
    }

    // 시뮬레이션된 현재가와 24시간 변동률
    val currentPrice = basePrice * (1 + (Math.random() - 0.5) * 0.02) // ±1% 변동
    val change24h = (Math.random() - 0.5) * 10 // ±5% 변동률

    return Pair(currentPrice, change24h)
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format("$%,.0f", price)
        price >= 1 -> String.format("$%.2f", price)
        price >= 0.01 -> String.format("$%.4f", price)
        else -> String.format("$%.6f", price)
    }
}

private suspend fun fetchPriceData(symbol: String, timeframe: String): List<PriceCandle> {
    // 실제 구현에서는 바이낸스 API를 호출해야 함
    // 여기서는 실제 데이터를 시뮬레이션
    delay(100) // API 호출 지연 시뮬레이션

    val basePrice = when (symbol) {
        "BTCUSDT" -> 43000.0
        "ETHUSDT" -> 2600.0
        "BNBUSDT" -> 315.0
        "XRPUSDT" -> 0.62
        "ADAUSDT" -> 0.48
        "DOGEUSDT" -> 0.083
        "SOLUSDT" -> 98.5
        "DOTUSDT" -> 7.2
        "MATICUSDT" -> 0.89
        "LTCUSDT" -> 72.5
        else -> 100.0
    }

    // 시뮬레이션된 가격 데이터 생성 (실제로는 바이낸스 API에서 가져와야 함)
    return (0 until 100).map { i ->
        val variation = (Math.random() - 0.5) * 0.04 // ±2% 변동
        val price = basePrice * (1 + variation)
        PriceCandle(
            timestamp = System.currentTimeMillis() - (99 - i) * 60000L, // 1분씩 과거
            open = price * 0.998,
            high = price * 1.002,
            low = price * 0.996,
            close = price,
            volume = Math.random() * 1000000
        )
    }
}