// 업데이트된 시세 조회 화면 - 즐겨찾기 코인 추가/삭제 기능 포함

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

    // 즐겨찾기 코인 목록 새로고침 함수
    fun refreshFavoriteCoins() {
        currentUser?.let { userData ->
            val mongoService = MongoDbService()
            mongoService.getFavoriteCoins(userData.username) { favoriteSymbols, error ->
                scope.launch {
                    if (error == null) {
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

                        // 가격 정보 로드
                        if (favoriteCoins.isNotEmpty()) {
                            refreshIndicators(favoriteCoins) { updated ->
                                coinIndicators = updated
                            }
                        }
                    }
                }
            }
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
                    } else {
                        // 코인 추가 버튼
                        Button(
                            onClick = { showAddCoinDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "코인 추가",
                                tint = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "코인추가",
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
                            "위의 '코인추가' 버튼을 눌러\n관심 코인을 추가해주세요."
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
            contentPadding = PaddingValues(bottom = 20.dp) // 하단 여백 대폭 증가
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
            containerColor = Color(0xFFF0F4F8) // 연한 블루그레이
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (coin.priceChange24h >= 0) {
                                        Icons.Default.TrendingUp
                                    } else {
                                        Icons.Default.TrendingDown
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = changeColor
                                )
                                Text(
                                    "${if (coin.priceChange24h >= 0) "+" else ""}${String.format("%.2f", coin.priceChange24h)}%",
                                    fontSize = 12.sp,
                                    color = changeColor
                                )
                            }
                        } else {
                            Text(
                                "로딩중...",
                                fontSize = 12.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 삭제 버튼
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "코인 삭제",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 기술적 지표 테이블 형태
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 헤더 행
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "",
                        fontSize = 12.sp,
                        modifier = Modifier.weight(0.8f) // 빈 공간
                    )
                    Text(
                        "15분",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "1시간",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "4시간",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "1일",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }

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
                                .padding(vertical = 2.dp)
                                .clickable { selectedSymbol = symbol },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedSymbol == symbol) {
                                    Color(0xFFE3F2FD)
                                } else {
                                    Color(0xFFF5F5F5)
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
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        symbol,
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                if (selectedSymbol == symbol) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "선택됨",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
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

// ===== 테이블 형태의 지표 셀 =====

@Composable
fun TableIndicatorCell(
    value: Double?,
    isCci: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (value != null) {
            val (bgColor, textColor) = if (isCci) {
                when {
                    value > 100 -> Pair(Color(0xFFFFEBEE), Color(0xFFD32F2F))
                    value < -100 -> Pair(Color(0xFFE8F5E8), Color(0xFF388E3C))
                    else -> Pair(Color(0xFFF5F5F5), Color(0xFF666666))
                }
            } else { // RSI
                when {
                    value > 70 -> Pair(Color(0xFFFFEBEE), Color(0xFFD32F2F))
                    value < 30 -> Pair(Color(0xFFE8F5E8), Color(0xFF388E3C))
                    else -> Pair(Color(0xFFF5F5F5), Color(0xFF666666))
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        bgColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    String.format("%.0f", value),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        } else {
            // 로딩 중
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "...",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
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

// ===== 현재가격 조회 =====

private suspend fun fetchCurrentPrice(symbol: String): Pair<Double, Double> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.binance.com/api/v3/ticker/24hr?symbol=$symbol")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: ""
                val jsonObject = org.json.JSONObject(jsonString)

                val price = jsonObject.getDouble("lastPrice")
                val change = jsonObject.getDouble("priceChangePercent")

                Pair(price, change)
            } else {
                // API 실패시 시뮬레이션 데이터 사용
                getSimulatedPrice(symbol)
            }
        } catch (e: Exception) {
            // 네트워크 오류시 시뮬레이션 데이터 사용
            getSimulatedPrice(symbol)
        }
    }
}

private fun getSimulatedPrice(symbol: String): Pair<Double, Double> {
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

private suspend fun fetchPriceData(symbol: String, timeframe: String): List<PriceCandle> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val interval = when (timeframe) {
                "15m" -> "15m"
                "1h" -> "1h"
                "4h" -> "4h"
                "1d" -> "1d"
                else -> "15m"
            }

            val request = Request.Builder()
                .url("https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=100")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: ""
                val jsonArray = org.json.JSONArray(jsonString)

                (0 until jsonArray.length()).map { i ->
                    val candle = jsonArray.getJSONArray(i)
                    PriceCandle(
                        timestamp = candle.getLong(0),
                        open = candle.getDouble(1),
                        high = candle.getDouble(2),
                        low = candle.getDouble(3),
                        close = candle.getDouble(4),
                        volume = candle.getDouble(5)
                    )
                }
            } else {
                // API 실패시 시뮬레이션 데이터 사용
                getSimulatedPriceData(symbol)
            }
        } catch (e: Exception) {
            // 네트워크 오류시 시뮬레이션 데이터 사용
            getSimulatedPriceData(symbol)
        }
    }
}

private fun getSimulatedPriceData(symbol: String): List<PriceCandle> {
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

    // 시뮬레이션된 가격 데이터 생성
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

// ===== CCI 계산 =====

private fun calculateCCI(priceData: List<PriceCandle>, period: Int = 20): Double {
    if (priceData.size < period) return 0.0

    val recentData = priceData.takeLast(period)
    val typicalPrices = recentData.map { (it.high + it.low + it.close) / 3.0 }
    val sma = typicalPrices.average()
    val meanDeviation = typicalPrices.map { abs(it - sma) }.average()

    return if (meanDeviation == 0.0) {
        0.0
    } else {
        (typicalPrices.last() - sma) / (0.015 * meanDeviation)
    }
}

// ===== RSI 계산 =====

private fun calculateRSI(priceData: List<PriceCandle>, period: Int = 7): Double {
    if (priceData.size < period + 1) return 50.0

    val recentData = priceData.takeLast(period + 1)
    val changes = (1 until recentData.size).map {
        recentData[it].close - recentData[it - 1].close
    }

    val gains = changes.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0
    val losses = changes.filter { it < 0 }.map { abs(it) }.average().takeIf { !it.isNaN() } ?: 0.0

    return if (losses == 0.0) {
        100.0
    } else {
        val rs = gains / losses
        100.0 - (100.0 / (1.0 + rs))
    }
}