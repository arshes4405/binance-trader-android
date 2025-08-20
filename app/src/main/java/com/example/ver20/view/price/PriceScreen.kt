package com.example.ver20.view.price

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.util.Log
import com.example.ver20.dao.mongoDB.UserService
import com.example.ver20.dao.mongoDB.UserData
import com.example.ver20.dao.mongoDB.MongoDbService
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.abs
import com.example.ver20.dao.trading.indicator.TechnicalIndicatorCalculator
import com.example.ver20.dao.trading.indicator.IndicatorResult
import java.text.DecimalFormat

// ===== 데이터 모델 =====

data class TechnicalIndicatorData(
    val timestamp: Long,
    val cciValue: Double,
    val rsiValue: Double
)

// CoinIndicatorInfo 데이터 클래스에 현재가/상승률 추가
data class CoinIndicatorInfo(
    val symbol: String,
    val displayName: String,
    val currentPrice: Double = 0.0,        // 현재가 추가
    val changePercent: Double = 0.0,       // 24시간 변동률 추가
    val min15: TechnicalIndicatorData?,
    val hour1: TechnicalIndicatorData?,
    val hour4: TechnicalIndicatorData?,
    val day1: TechnicalIndicatorData?,
    val isLoading: Boolean = false
)

// ===== 메인 화면 - HOME처럼 검은색 배경 적용 =====

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

    // HOME과 같은 배경 그라데이션
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D1117),
            Color(0xFF1A1A2E),
            Color(0xFF16213E)
        )
    )

    // 모든 코인의 지표 로드 함수 (먼저 정의)
    fun loadAllIndicators() {
        scope.launch {
            coinIndicators.forEach { coin ->
                loadIndicatorsForCoin(coin.symbol) { updatedCoin ->
                    scope.launch(Dispatchers.Main) {
                        coinIndicators = coinIndicators.map {
                            if (it.symbol == updatedCoin.symbol) updatedCoin else it
                        }
                    }
                }
            }
        }
    }

    // 즐겨찾기 코인 새로고침 함수
    fun refreshFavoriteCoins() {
        currentUser?.let { userData ->
            val mongoService = MongoDbService()
            mongoService.getFavoriteCoins(userData.username) { symbols: List<String>, error: String? ->
                scope.launch(Dispatchers.Main) {
                    if (error == null && symbols.isNotEmpty()) {
                        val newCoinList = symbols.map { symbol ->
                            CoinIndicatorInfo(
                                symbol = symbol,
                                displayName = formatDisplayName(symbol),
                                min15 = null,
                                hour1 = null,
                                hour4 = null,
                                day1 = null,
                                isLoading = true
                            )
                        }
                        coinIndicators = newCoinList
                        loadAllIndicators()
                    } else {
                        coinIndicators = emptyList()
                    }
                }
            }
        }
    }

    // 사용자 정보 로드
    LaunchedEffect(Unit) {
        val userService = UserService()
        val userData = userService.getUserFromPreferences(context)
        currentUser = userData
        hasUserInfo = userData != null

        if (userData != null) {
            refreshFavoriteCoins()
        }
    }

    // 코인 추가 다이얼로그
    if (showAddCoinDialog) {
        AddCoinDialog(
            onDismiss = { showAddCoinDialog = false },
            onConfirm = { symbol ->
                currentUser?.let { userData ->
                    val mongoService = MongoDbService()
                    mongoService.saveFavoriteCoin(userData.username, symbol.uppercase()) { success: Boolean, message: String? ->
                        scope.launch(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(context, "코인이 추가되었습니다", Toast.LENGTH_SHORT).show()
                                refreshFavoriteCoins()
                            } else {
                                Toast.makeText(context, message ?: "추가 실패", Toast.LENGTH_SHORT).show()
                            }
                            showAddCoinDialog = false
                        }
                    }
                }
            }
        )
    }

    // 메인 UI - 검은색 배경 적용
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== 헤더 카드 - 어두운 테마로 변경 =====
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "📊 코인 검색",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700) // 골드색
                            )
                            if (hasUserInfo && currentUser != null) {
                                Text(
                                    "환영합니다, ${currentUser!!.username}님",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC0C0C0)  // 실버색으로 변경
                                )
                            }
                        }

                        // 버튼 영역
                        if (!hasUserInfo) {
                            Button(
                                onClick = onShowCreateAccount,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700)
                                )
                            ) {
                                Text(
                                    "계정생성",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
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
                                        containerColor = Color(0xFFFFD700)
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "코인 추가",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "코인추가",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 새로고침 버튼
                                Button(
                                    onClick = {
                                        isRefreshing = true
                                        refreshFavoriteCoins()
                                        scope.launch {
                                            kotlinx.coroutines.delay(1000)
                                            isRefreshing = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFC0C0C0)  // 실버색으로 변경
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "새로고침",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isRefreshing) "업데이트중" else "새로고침",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== 코인 목록 또는 안내 메시지 - 어두운 테마로 변경 =====
        if (coinIndicators.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (hasUserInfo) Icons.Default.Add else Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (hasUserInfo) {
                                "즐겨찾기 코인이 없습니다."
                            } else {
                                "로그인 후 즐겨찾기 코인을 등록하세요."
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (hasUserInfo) {
                                "'코인추가' 버튼을 눌러 코인을 추가해보세요!"
                            } else {
                                "계정을 생성하고 다양한 기능을 이용해보세요."
                            },
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            // ===== 지표 테이블 헤더 - 어두운 테마로 변경 =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // 타이틀
                        Text(
                            "📈 기술적 지표 현황",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 테이블 헤더
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "코인",
                                modifier = Modifier.weight(0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "CCI / RSI",
                                modifier = Modifier.weight(2f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFFFFD700)  // 골드색으로 변경
                            )
                        }

                        // 시간대 헤더
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(0.6f))
                            Row(modifier = Modifier.weight(2f)) {
                                Text("15m", modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium)
                                Text("1h", modifier = Modifier.weight(1.2f), fontSize = 12.sp, textAlign = TextAlign.Center, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium)
                                Text("4h", modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium)
                                Text("1d", modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // ===== 코인별 지표 데이터 - 어두운 테마로 변경 =====
            items(coinIndicators) { coin ->
                CoinIndicatorRow(
                    coin = coin,
                    onRemoveClick = {
                        currentUser?.let { userData ->
                            val mongoService = MongoDbService()
                            mongoService.removeFavoriteCoin(userData.username, coin.symbol) { success: Boolean, message: String? ->
                                scope.launch(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(context, "코인이 제거되었습니다", Toast.LENGTH_SHORT).show()
                                        refreshFavoriteCoins()
                                    } else {
                                        Toast.makeText(context, message ?: "제거 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// ===== 코인 지표 행 컴포저블 - 어두운 테마로 변경 =====

// ===== 코인 지표 행 컴포저블 - 가격/변동률 추가 =====

@Composable
fun CoinIndicatorRow(
    coin: CoinIndicatorInfo,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 코인 이름, 가격, 변동률 및 제거 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽: 코인 정보
                Column {
                    Text(
                        coin.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        coin.symbol,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                // 중간: 가격 및 변동률
                if (coin.currentPrice > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = DecimalFormat("#,##0.##").format(coin.currentPrice),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64B5F6), // 연한 파란색
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = "${if (coin.changePercent >= 0) "+" else ""}${DecimalFormat("#0.00").format(coin.changePercent)}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (coin.changePercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }

                // 오른쪽: 제거 버튼
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "제거",
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 지표 테이블
            if (coin.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFFFD700)
                    )
                }
            } else {
                // CCI 행
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CCI",
                        modifier = Modifier.weight(1.2f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RSI",
                        modifier = Modifier.weight(1.2f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D)
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

// ===== 지표 셀 컴포저블 (기존과 동일) =====

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
        backgroundColor = Color(0xFF424242)
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
                    backgroundColor = Color(0xFF616161) // 어두운 회색 (중립)
                    textColor = Color.White
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
                    backgroundColor = Color(0xFF616161) // 어두운 회색 (중립)
                    textColor = Color.White
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

// ===== 코인 추가 다이얼로그 - UX 개선 버전 =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCoinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isValidSymbol by remember { mutableStateOf(true) }

    // 유효한 심볼인지 체크하는 함수 (더 유연하게)
    fun validateSymbol(input: String): Boolean {
        if (input.isBlank()) return false

        // BTC, ETH 같은 짧은 입력도 허용
        val trimmedInput = input.trim().uppercase()

        // 이미 USDT가 붙어있으면 그대로, 없으면 추가해서 검증
        val symbolToCheck = if (trimmedInput.endsWith("USDT")) {
            trimmedInput
        } else {
            "${trimmedInput}USDT"
        }

        // USDT 페어 패턴 검증
        val pattern = "^[A-Z]{2,10}USDT$".toRegex()
        return pattern.matches(symbolToCheck)
    }

    // 입력값을 USDT 심볼로 변환
    fun convertToUSDTSymbol(input: String): String {
        val trimmedInput = input.trim().uppercase()
        return if (trimmedInput.endsWith("USDT")) {
            trimmedInput
        } else {
            "${trimmedInput}USDT"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                "코인 추가",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    "코인 심볼을 입력하세요\n(예: BTC, ETH, BTCUSDT)",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it.uppercase()
                        isValidSymbol = validateSymbol(inputText)
                    },
                    label = {
                        Text("코인 심볼", color = Color.Gray)
                    },
                    placeholder = {
                        Text("BTC 또는 BTCUSDT", color = Color.Gray)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isValidSymbol && inputText.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,        // 입력 글자 색상
                        unfocusedTextColor = Color.White,      // 입력 글자 색상
                        cursorColor = Color(0xFFFFD700),
                        focusedLabelColor = Color(0xFFFFD700),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                // 변환 미리보기
                if (inputText.isNotBlank() && isValidSymbol) {
                    val finalSymbol = convertToUSDTSymbol(inputText)
                    if (finalSymbol != inputText.uppercase()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "→ $finalSymbol 로 추가됩니다",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isValidSymbol && inputText.isNotBlank()) {
                    Text(
                        "올바른 형식이 아닙니다 (예: BTC, BTCUSDT)",
                        color = Color(0xFFE57373),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 인기 코인 빠른 선택
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "빠른 선택:",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val popularCoins = listOf("BTC", "ETH", "BNB", "SOL")
                    popularCoins.forEach { coin ->
                        Card(
                            modifier = Modifier
                                .clickable {
                                    inputText = coin
                                    isValidSymbol = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (inputText.uppercase() == coin)
                                    Color(0xFFFFD700) else Color(0xFF2D2D2D)
                            )
                        ) {
                            Text(
                                coin,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (inputText.uppercase() == coin)
                                    Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidSymbol && inputText.isNotBlank()) {
                        val finalSymbol = convertToUSDTSymbol(inputText)
                        onConfirm(finalSymbol)
                    }
                },
                enabled = isValidSymbol && inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(
                    "추가",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    "취소",
                    color = Color.Gray
                )
            }
        }
    )
}

// ===== 유틸리티 함수들 =====

// ===== DAO 레이어를 사용한 지표 로드 =====

// loadIndicatorsForCoin 함수 수정 - 가격 정보도 함께 로드
suspend fun loadIndicatorsForCoin(
    symbol: String,
    onResult: (CoinIndicatorInfo) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val indicatorCalculator = TechnicalIndicatorCalculator()

            // 1. 기술적 지표 계산
            val indicators = indicatorCalculator.calculateMultiTimeframeIndicators(
                symbol = symbol,
                timeframes = listOf("15m", "1h", "4h", "1d"),
                cciPeriod = 20,
                rsiPeriod = 7
            )

            // 2. 현재가와 24시간 변동률 가져오기
            val priceInfo = getCurrentPriceAndChange(symbol)

            // IndicatorResult를 TechnicalIndicatorData로 변환
            val convertedIndicators = mutableMapOf<String, TechnicalIndicatorData>()
            indicators.forEach { (timeframe, result) ->
                convertedIndicators[timeframe] = TechnicalIndicatorData(
                    timestamp = result.timestamp,
                    cciValue = result.cci,
                    rsiValue = result.rsi
                )
            }

            val updatedCoin = CoinIndicatorInfo(
                symbol = symbol,
                displayName = formatDisplayName(symbol),
                currentPrice = priceInfo.first,      // 현재가
                changePercent = priceInfo.second,    // 24시간 변동률
                min15 = convertedIndicators["15m"],
                hour1 = convertedIndicators["1h"],
                hour4 = convertedIndicators["4h"],
                day1 = convertedIndicators["1d"],
                isLoading = false
            )

            Log.d("PriceScreen", "가격 정보 로드 완료: $symbol - 현재가: ${priceInfo.first}, 변동률: ${priceInfo.second}%")
            onResult(updatedCoin)

        } catch (e: Exception) {
            Log.e("PriceScreen", "데이터 로드 실패: ${e.message}")
            val errorCoin = CoinIndicatorInfo(
                symbol = symbol,
                displayName = formatDisplayName(symbol),
                currentPrice = 0.0,
                changePercent = 0.0,
                min15 = null,
                hour1 = null,
                hour4 = null,
                day1 = null,
                isLoading = false
            )
            onResult(errorCoin)
        }
    }
}

// 현재가와 24시간 변동률을 가져오는 함수 추가
suspend fun getCurrentPriceAndChange(symbol: String): Pair<Double, Double> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.binance.com/api/v3/ticker/24hr?symbol=$symbol")
                .build()

            val response = client.newCall(request).execute()
            val jsonString = response.body?.string() ?: return@withContext Pair(0.0, 0.0)

            // JSON 파싱 (간단한 방식)
            val currentPriceRegex = "\"lastPrice\":\"([^\"]+)\"".toRegex()
            val changePercentRegex = "\"priceChangePercent\":\"([^\"]+)\"".toRegex()

            val currentPriceMatch = currentPriceRegex.find(jsonString)
            val changePercentMatch = changePercentRegex.find(jsonString)

            val currentPrice = currentPriceMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val changePercent = changePercentMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

            Pair(currentPrice, changePercent)

        } catch (e: Exception) {
            Log.e("PriceScreen", "가격 정보 로드 실패: ${e.message}")
            Pair(0.0, 0.0)
        }
    }
}

// 기존 계산 함수들은 DAO로 이동되어 제거됨

fun formatDisplayName(symbol: String): String {
    return when {
        symbol.startsWith("BTC") -> "비트코인"
        symbol.startsWith("ETH") -> "이더리움"
        symbol.startsWith("BNB") -> "바이낸스코인"
        symbol.startsWith("ADA") -> "에이다"
        symbol.startsWith("SOL") -> "솔라나"
        symbol.startsWith("XRP") -> "리플"
        symbol.startsWith("DOT") -> "폴카닷"
        symbol.startsWith("DOGE") -> "도지코인"
        symbol.startsWith("AVAX") -> "아발란체"
        symbol.startsWith("MATIC") -> "폴리곤"
        else -> symbol.replace("USDT", "")
    }
}

fun getInitialCoinList(): List<CoinIndicatorInfo> {
    return emptyList()
}