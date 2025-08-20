// RsiSignalSettingsScreen.kt - RSI 시세포착 설정 화면

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
import com.example.ver20.dao.trading.signal.MarketSignalConfig
import com.example.ver20.dao.trading.signal.MarketSignalService
import com.example.ver20.dao.trading.signal.StrategyDefaults
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

    // RSI 설정값들
    var selectedSymbol by remember { mutableStateOf(editConfig?.symbol ?: "BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf(editConfig?.timeframe ?: "15m") }
    var checkInterval by remember { mutableStateOf(editConfig?.checkInterval?.toString() ?: "15") }
    var seedMoney by remember { mutableStateOf(editConfig?.seedMoney?.toString() ?: "1000") }
    var isActive by remember { mutableStateOf(editConfig?.isActive ?: true) }
    var enableAutoTrading by remember { mutableStateOf(editConfig?.autoTrading ?: false) }

    // RSI 전용 설정
    var rsiPeriod by remember { mutableStateOf(editConfig?.rsiPeriod?.toString() ?: "14") }
    var rsiOverbought by remember { mutableStateOf(editConfig?.rsiOverbought?.toString() ?: "70") }
    var rsiOversold by remember { mutableStateOf(editConfig?.rsiOversold?.toString() ?: "30") }

    // UI 상태
    var showSymbolMenu by remember { mutableStateOf(false) }
    var showTimeframeMenu by remember { mutableStateOf(false) }

    // 초기 사용자 정보 로드
    LaunchedEffect(Unit) {
        val userData = userService.getUserFromPreferences(context)
        currentUser = userData
    }

    // 유효성 검증 함수
    fun validateInputs(): String? {
        val intervalInt = checkInterval.toIntOrNull()
        val seedMoneyDouble = seedMoney.toDoubleOrNull()
        val rsiPeriodInt = rsiPeriod.toIntOrNull()
        val rsiOverboughtDouble = rsiOverbought.toDoubleOrNull()
        val rsiOversoldDouble = rsiOversold.toDoubleOrNull()

        return when {
            intervalInt == null || intervalInt < 15 -> "체크 인터벌은 15분 이상이어야 합니다"
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
        // 헤더
        TopAppBar(
            title = {
                Text(
                    text = "RSI 전략 설정",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }
            },
            actions = {
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
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1A237E)
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                    checkInterval = checkInterval,
                    onSymbolClick = { showSymbolMenu = true },
                    onTimeframeClick = { showTimeframeMenu = true },
                    onCheckIntervalChange = { checkInterval = it }
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

    // 심볼 선택 메뉴
    if (showSymbolMenu) {
        val symbols = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "XRPUSDT", "SOLUSDT")
        DropdownMenu(
            expanded = showSymbolMenu,
            onDismissRequest = { showSymbolMenu = false }
        ) {
            symbols.forEach { symbol ->
                DropdownMenuItem(
                    text = { Text(symbol) },
                    onClick = {
                        selectedSymbol = symbol
                        showSymbolMenu = false
                    }
                )
            }
        }
    }

    // 시간대 선택 메뉴
    if (showTimeframeMenu) {
        val timeframes = listOf("15m", "1h", "4h", "1d")
        DropdownMenu(
            expanded = showTimeframeMenu,
            onDismissRequest = { showTimeframeMenu = false }
        ) {
            timeframes.forEach { timeframe ->
                DropdownMenuItem(
                    text = { Text(timeframe) },
                    onClick = {
                        selectedTimeframe = timeframe
                        showTimeframeMenu = false
                    }
                )
            }
        }
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
private fun RsiBasicSettingsCard(
    selectedSymbol: String,
    selectedTimeframe: String,
    checkInterval: String,
    onSymbolClick: () -> Unit,
    onTimeframeClick: () -> Unit,
    onCheckIntervalChange: (String) -> Unit
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
                        value = selectedSymbol,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSymbolClick() }
                    )
                }

                // 시간대 선택
                Column(modifier = Modifier.weight(1f)) {
                    Text("시간대", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedTimeframe,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimeframeClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 체크 인터벌
            Text("체크 인터벌 (분)", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = checkInterval,
                onValueChange = onCheckIntervalChange,
                placeholder = { Text("15", color = Color(0xFF90A4AE)) },
                supportingText = { Text("최소 15분부터 설정 가능", color = Color(0xFF90A4AE), fontSize = 12.sp) },
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