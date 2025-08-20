// RsiSignalSettingsScreen.kt - RSI 시세포착 설정 화면 (다이얼로그 방식)

package com.example.ver20.view.signal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.dao.mongoDB.UserData
import com.example.ver20.dao.mongoDB.UserService
import com.example.ver20.dao.mongoDB.MongoDbService
import com.example.ver20.dao.trading.signal.MarketSignalConfig
import com.example.ver20.dao.trading.signal.MarketSignalService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RsiSignalSettingsScreen(
    editConfig: MarketSignalConfig? = null,
    onBackClick: () -> Unit,
    onSettingsSaved: (MarketSignalConfig) -> Unit
) {
    val context = LocalContext.current
    val userService = remember { UserService() }
    val marketSignalService = remember { MarketSignalService() }
    val coroutineScope = rememberCoroutineScope()

    // 사용자 정보
    var currentUser by remember { mutableStateOf<UserData?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var favoriteCoins by remember { mutableStateOf<List<String>>(emptyList()) }

    // RSI 설정값들
    var selectedSymbol by remember { mutableStateOf(editConfig?.symbol ?: "BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf(editConfig?.timeframe ?: "15m") }
    var checkInterval by remember { mutableStateOf(editConfig?.checkInterval?.toString() ?: "15") }
    var seedMoney by remember { mutableStateOf(editConfig?.seedMoney?.toString() ?: "1000") }
    var isActive by remember { mutableStateOf(editConfig?.isActive ?: true) }
    var enableAutoTrading by remember { mutableStateOf(editConfig?.autoTrading ?: false) }
    var selectedDirection by remember { mutableStateOf("LONG") } // LONG, SHORT

    // RSI 전용 설정
    var rsiPeriod by remember { mutableStateOf(editConfig?.rsiPeriod?.toString() ?: "14") }
    var rsiOverbought by remember { mutableStateOf(editConfig?.rsiOverbought?.toString() ?: "70") }
    var rsiOversold by remember { mutableStateOf(editConfig?.rsiOversold?.toString() ?: "30") }

    // UI 상태
    var showSymbolDialog by remember { mutableStateOf(false) }
    var showTimeframeDialog by remember { mutableStateOf(false) }
    var showDirectionDialog by remember { mutableStateOf(false) }

    // 초기 사용자 정보 및 즐겨찾기 코인 로드
    LaunchedEffect(Unit) {
        val userData = userService.getUserFromPreferences(context)
        currentUser = userData

        // 즐겨찾기 코인 목록 로드
        userData?.let { user ->
            val mongoService = MongoDbService()
            mongoService.getFavoriteCoins(user.username) { symbols, error ->
                if (error == null && symbols.isNotEmpty()) {
                    favoriteCoins = symbols
                } else {
                    // 기본 인기 코인 목록
                    favoriteCoins = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "XRPUSDT", "SOLUSDT")
                }
            }
        } ?: run {
            // 로그인 안된 경우 기본 목록
            favoriteCoins = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "XRPUSDT", "SOLUSDT")
        }
    }

    // 유효성 검증 함수
    fun validateInputs(): String? {
        val intervalInt = checkInterval.toIntOrNull()
        val seedMoneyDouble = seedMoney.toDoubleOrNull()
        val rsiPeriodInt = rsiPeriod.toIntOrNull()
        val rsiOverboughtDouble = rsiOverbought.toDoubleOrNull()
        val rsiOversoldDouble = rsiOversold.toDoubleOrNull()

        // arshes 계정은 1분부터, 다른 계정은 5분부터
        val minInterval = if (currentUser?.username == "arshes") 1 else 5

        return when {
            intervalInt == null || intervalInt < minInterval -> "체크 인터벌은 ${minInterval}분 이상이어야 합니다"
            intervalInt > 1440 -> "체크 인터벌은 24시간(1440분) 이하여야 합니다"
            seedMoneyDouble == null || seedMoneyDouble < 10 -> "시드머니는 10 USDT 이상이어야 합니다"
            seedMoneyDouble > 100000 -> "시드머니는 100,000 USDT 이하여야 합니다"
            rsiPeriodInt == null || rsiPeriodInt < 2 -> "RSI 기간은 2 이상이어야 합니다"
            rsiPeriodInt > 200 -> "RSI 기간은 200 이하여야 합니다"
            rsiOverboughtDouble == null || rsiOverboughtDouble <= 50 -> "과매수 값은 50 초과여야 합니다"
            rsiOverboughtDouble > 100 -> "과매수 값은 100 이하여야 합니다"
            rsiOversoldDouble == null || rsiOversoldDouble < 0 -> "과매도 값은 0 이상이어야 합니다"
            rsiOversoldDouble >= 50 -> "과매도 값은 50 미만이어야 합니다"
            rsiOversoldDouble >= rsiOverboughtDouble -> "과매도 값은 과매수 값보다 작아야 합니다"
            else -> null
        }
    }

    // 저장 함수
    fun saveSettings() {
        val validationError = validateInputs()
        if (validationError != null) {
            Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
            return
        }

        currentUser?.let { user ->
            isSaving = true
            val config = MarketSignalConfig(
                id = editConfig?.id ?: "",
                username = user.username,
                signalType = "RSI",
                symbol = selectedSymbol,
                timeframe = selectedTimeframe,
                checkInterval = checkInterval.toInt(),
                isActive = isActive,
                autoTrading = enableAutoTrading,
                seedMoney = seedMoney.toDouble(),
                rsiPeriod = rsiPeriod.toInt(),
                rsiOverbought = rsiOverbought.toDouble(),
                rsiOversold = rsiOversold.toDouble()
            )

            coroutineScope.launch {
                marketSignalService.saveSignalConfig(config) { success, message ->
                    isSaving = false
                    if (success) {
                        Toast.makeText(context, "RSI 설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
                        onSettingsSaved(config)
                    } else {
                        Toast.makeText(context, message ?: "저장 실패", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // 커스텀 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A237E))
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }
                Text(
                    text = "RSI 전략 설정",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            // 저장 버튼
            TextButton(
                onClick = { saveSettings() },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFFD700)
                    )
                } else {
                    Text(
                        text = "저장",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RSI 전략 설명
            item {
                InfoCard()
            }

            // 기본 설정
            item {
                RsiBasicSettingsCard(
                    selectedSymbol = selectedSymbol,
                    selectedTimeframe = selectedTimeframe,
                    selectedDirection = selectedDirection,
                    checkInterval = checkInterval,
                    onSymbolClick = { showSymbolDialog = true },
                    onTimeframeClick = { showTimeframeDialog = true },
                    onDirectionClick = { showDirectionDialog = true },
                    onCheckIntervalChange = { checkInterval = it },
                    currentUser = currentUser
                )
            }

            // RSI 지표 설정
            item {
                RsiIndicatorCard(
                    rsiPeriod = rsiPeriod,
                    rsiOverbought = rsiOverbought,
                    rsiOversold = rsiOversold,
                    onRsiPeriodChange = { rsiPeriod = it },
                    onRsiOverboughtChange = { rsiOverbought = it },
                    onRsiOversoldChange = { rsiOversold = it }
                )
            }

            // 투자 설정
            item {
                RsiInvestmentCard(
                    seedMoney = seedMoney,
                    enableAutoTrading = enableAutoTrading,
                    isActive = isActive,
                    onSeedMoneyChange = { seedMoney = it },
                    onAutoTradingChange = { enableAutoTrading = it },
                    onActiveChange = { isActive = it }
                )
            }
        }
    }

    // 코인 선택 다이얼로그
    if (showSymbolDialog) {
        CoinSelectionDialog(
            coins = favoriteCoins,
            selectedCoin = selectedSymbol,
            onCoinSelected = { coin ->
                selectedSymbol = coin
                showSymbolDialog = false
            },
            onDismiss = { showSymbolDialog = false }
        )
    }

    // 시간대 선택 다이얼로그
    if (showTimeframeDialog) {
        TimeframeSelectionDialog(
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = { timeframe ->
                selectedTimeframe = timeframe
                showTimeframeDialog = false
            },
            onDismiss = { showTimeframeDialog = false }
        )
    }

    // 매매 방향 선택 다이얼로그
    if (showDirectionDialog) {
        DirectionSelectionDialog(
            selectedDirection = selectedDirection,
            onDirectionSelected = { direction ->
                selectedDirection = direction
                showDirectionDialog = false
            },
            onDismiss = { showDirectionDialog = false }
        )
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "RSI 시세포착 전략",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "상대강도지수(RSI)를 활용한 과매수/과매도 구간 포착",
                    fontSize = 14.sp,
                    color = Color(0xFF90A4AE),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun RsiBasicSettingsCard(
    selectedSymbol: String,
    selectedTimeframe: String,
    selectedDirection: String,
    checkInterval: String,
    onSymbolClick: () -> Unit,
    onTimeframeClick: () -> Unit,
    onDirectionClick: () -> Unit,
    onCheckIntervalChange: (String) -> Unit,
    currentUser: UserData? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "기본 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 심볼 선택
                Column(modifier = Modifier.weight(1f)) {
                    Text("코인", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedSymbol.replace("USDT", ""), // USDT 제거
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = onSymbolClick) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 시간대 선택
                Column(modifier = Modifier.weight(1f)) {
                    Text("시간대", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = when(selectedTimeframe) {
                            "15m" -> "15분"
                            "1h" -> "1시간"
                            "4h" -> "4시간"
                            "1d" -> "1일"
                            else -> selectedTimeframe
                        },
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = onTimeframeClick) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 매매 방향 선택
            Text("매매 방향", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = when(selectedDirection) {
                    "LONG" -> "롱 (Long)"
                    "SHORT" -> "숏 (Short)"
                    else -> selectedDirection
                },
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = onDirectionClick) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFF90A4AE)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 체크 인터벌
            Text("체크 인터벌 (분)", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = checkInterval,
                onValueChange = onCheckIntervalChange,
                placeholder = { Text(if (currentUser?.username == "arshes") "1" else "5", color = Color(0xFF90A4AE)) },
                supportingText = {
                    Text(
                        if (currentUser?.username == "arshes") "최소 1분부터 설정 가능" else "최소 5분부터 설정 가능",
                        color = Color(0xFF90A4AE),
                        fontSize = 12.sp
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFF90A4AE)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RsiIndicatorCard(
    rsiPeriod: String,
    rsiOverbought: String,
    rsiOversold: String,
    onRsiPeriodChange: (String) -> Unit,
    onRsiOverboughtChange: (String) -> Unit,
    onRsiOversoldChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "RSI 지표 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // RSI 기간
            Text("RSI 기간", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = rsiPeriod,
                onValueChange = onRsiPeriodChange,
                placeholder = { Text("14", color = Color(0xFF90A4AE)) },
                supportingText = { Text("일반적으로 14일 기간 사용", color = Color(0xFF90A4AE), fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFF90A4AE)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 과매수 구간
                Column(modifier = Modifier.weight(1f)) {
                    Text("과매수 (Short 신호)", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rsiOverbought,
                        onValueChange = onRsiOverboughtChange,
                        placeholder = { Text("70", color = Color(0xFF90A4AE)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF44336),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 과매도 구간
                Column(modifier = Modifier.weight(1f)) {
                    Text("과매도 (Long 신호)", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rsiOversold,
                        onValueChange = onRsiOversoldChange,
                        placeholder = { Text("30", color = Color(0xFF90A4AE)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // RSI 설명
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "💡 RSI 전략 가이드",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• RSI 30 이하: 과매도 구간 → Long 진입 신호\n" +
                                "• RSI 70 이상: 과매수 구간 → Short 진입 신호\n" +
                                "• 높은 신뢰도의 역추세 매매 전략",
                        fontSize = 12.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RsiInvestmentCard(
    seedMoney: String,
    enableAutoTrading: Boolean,
    isActive: Boolean,
    onSeedMoneyChange: (String) -> Unit,
    onAutoTradingChange: (Boolean) -> Unit,
    onActiveChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "투자 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 시드머니
            Text("시드머니 (USDT)", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = seedMoney,
                onValueChange = onSeedMoneyChange,
                placeholder = { Text("1000", color = Color(0xFF90A4AE)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFF90A4AE)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 자동매매 스위치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("자동매매", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("신호 발생시 자동 실행", color = Color(0xFF90A4AE), fontSize = 12.sp)
                }
                Switch(
                    checked = enableAutoTrading,
                    onCheckedChange = onAutoTradingChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CAF50),
                        checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 활성화 스위치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("설정 활성화", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("시세포착 모니터링 시작", color = Color(0xFF90A4AE), fontSize = 12.sp)
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = onActiveChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CAF50),
                        checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
private fun CoinSelectionDialog(
    coins: List<String>,
    selectedCoin: String,
    onCoinSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "코인 선택",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(coins.size) { index ->
                    val coin = coins[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCoinSelected(coin)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = coin == selectedCoin,
                            onClick = { onCoinSelected(coin) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF4CAF50),
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = coin.replace("USDT", ""), // USDT 제거하여 표시
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "취소",
                    color = Color(0xFF4CAF50)
                )
            }
        },
        containerColor = Color(0xFF1A1A2E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun DirectionSelectionDialog(
    selectedDirection: String,
    onDirectionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val directions = listOf(
        "LONG" to "롱 (Long)",
        "SHORT" to "숏 (Short)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "매매 방향 선택",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                directions.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDirectionSelected(value)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selectedDirection,
                            onClick = { onDirectionSelected(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF4CAF50),
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = when(value) {
                                    "LONG" -> "과매도 구간에서 Long 진입 신호"
                                    "SHORT" -> "과매수 구간에서 Short 진입 신호"
                                    else -> ""
                                },
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "취소",
                    color = Color(0xFF4CAF50)
                )
            }
        },
        containerColor = Color(0xFF1A1A2E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun TimeframeSelectionDialog(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val timeframes = listOf(
        "15m" to "15분",
        "1h" to "1시간",
        "4h" to "4시간",
        "1d" to "1일"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "시간대 선택",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                timeframes.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTimeframeSelected(value)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selectedTimeframe,
                            onClick = { onTimeframeSelected(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF4CAF50),
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "취소",
                    color = Color(0xFF4CAF50)
                )
            }
        },
        containerColor = Color(0xFF1A1A2E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}