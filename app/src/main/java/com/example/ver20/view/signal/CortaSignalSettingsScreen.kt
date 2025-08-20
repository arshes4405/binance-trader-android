// CortaSignalSettingsScreen.kt - 코르타 시세포착 설정 화면

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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CortaSignalSettingsScreen(
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

    // 기본 설정값들
    var selectedSymbol by remember { mutableStateOf(editConfig?.symbol ?: "BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf(editConfig?.timeframe ?: "15m") }
    var checkInterval by remember { mutableStateOf(editConfig?.checkInterval?.toString() ?: "15") }
    var seedMoney by remember { mutableStateOf(editConfig?.seedMoney?.toString() ?: "1000") }
    var isActive by remember { mutableStateOf(editConfig?.isActive ?: true) }
    var enableAutoTrading by remember { mutableStateOf(editConfig?.autoTrading ?: false) }

    // 코르타 전용 설정
    var cortaFastMa by remember { mutableStateOf(editConfig?.cortaFastMa?.toString() ?: "12") }
    var cortaSlowMa by remember { mutableStateOf(editConfig?.cortaSlowMa?.toString() ?: "26") }
    var cortaSignalLine by remember { mutableStateOf(editConfig?.cortaSignalLine?.toString() ?: "9") }
    var cortaVolumeFactor by remember { mutableStateOf(editConfig?.cortaVolumeFactor?.toString() ?: "1.5") }
    var cortaRsiConfirm by remember { mutableStateOf(editConfig?.cortaRsiConfirm ?: true) }

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
        val fastMaInt = cortaFastMa.toIntOrNull()
        val slowMaInt = cortaSlowMa.toIntOrNull()
        val signalLineInt = cortaSignalLine.toIntOrNull()
        val volumeFactorDouble = cortaVolumeFactor.toDoubleOrNull()

        return when {
            intervalInt == null || intervalInt < 15 -> "체크 인터벌은 15분 이상이어야 합니다"
            intervalInt > 1440 -> "체크 인터벌은 24시간(1440분) 이하여야 합니다"
            seedMoneyDouble == null || seedMoneyDouble < 10 -> "시드머니는 10 USDT 이상이어야 합니다"
            seedMoneyDouble > 100000 -> "시드머니는 100,000 USDT 이하여야 합니다"
            fastMaInt == null || fastMaInt < 1 -> "빠른 이동평균은 1 이상이어야 합니다"
            fastMaInt > 50 -> "빠른 이동평균은 50 이하여야 합니다"
            slowMaInt == null || slowMaInt <= fastMaInt -> "느린 이동평균은 빠른 이동평균보다 커야 합니다"
            slowMaInt > 100 -> "느린 이동평균은 100 이하여야 합니다"
            signalLineInt == null || signalLineInt < 1 -> "시그널 라인은 1 이상이어야 합니다"
            signalLineInt > 20 -> "시그널 라인은 20 이하여야 합니다"
            volumeFactorDouble == null || volumeFactorDouble < 1.0 -> "거래량 배율은 1.0 이상이어야 합니다"
            volumeFactorDouble > 10.0 -> "거래량 배율은 10.0 이하여야 합니다"
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
                signalType = "CORTA",
                symbol = selectedSymbol,
                timeframe = selectedTimeframe,
                checkInterval = checkInterval.toInt(),
                isActive = isActive,
                autoTrading = enableAutoTrading,
                seedMoney = seedMoney.toDouble(),
                cortaFastMa = cortaFastMa.toInt(),
                cortaSlowMa = cortaSlowMa.toInt(),
                cortaSignalLine = cortaSignalLine.toInt(),
                cortaVolumeFactor = cortaVolumeFactor.toDouble(),
                cortaRsiConfirm = cortaRsiConfirm
            )

            coroutineScope.launch {
                marketSignalService.saveSignalConfig(config) { success, message ->
                    isSaving = false
                    if (success) {
                        Toast.makeText(context, "코르타 설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
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
                    text = "코르타 전략 설정",
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
            // 코르타 전략 설명
            item {
                InfoCard()
            }

            // 기본 설정
            item {
                BasicSettingsCard(
                    selectedSymbol = selectedSymbol,
                    selectedTimeframe = selectedTimeframe,
                    checkInterval = checkInterval,
                    onSymbolClick = { showSymbolMenu = true },
                    onTimeframeClick = { showTimeframeMenu = true },
                    onCheckIntervalChange = { checkInterval = it }
                )
            }

            // MACD 설정
            item {
                MacdSettingsCard(
                    cortaFastMa = cortaFastMa,
                    cortaSlowMa = cortaSlowMa,
                    cortaSignalLine = cortaSignalLine,
                    onFastMaChange = { cortaFastMa = it },
                    onSlowMaChange = { cortaSlowMa = it },
                    onSignalLineChange = { cortaSignalLine = it }
                )
            }

            // 고급 설정
            item {
                AdvancedSettingsCard(
                    cortaVolumeFactor = cortaVolumeFactor,
                    cortaRsiConfirm = cortaRsiConfirm,
                    onVolumeFactorChange = { cortaVolumeFactor = it },
                    onRsiConfirmChange = { cortaRsiConfirm = it }
                )
            }

            // 투자 설정
            item {
                InvestmentCard(
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
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "코르타 복합 전략",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PREMIUM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .background(
                                Color(0xFFFFD700),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "MACD + RSI + 거래량을 결합한 고정밀 트레이딩 전략",
                    fontSize = 14.sp,
                    color = Color(0xFF90A4AE),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun MacdSettingsCard(
    cortaFastMa: String,
    cortaSlowMa: String,
    cortaSignalLine: String,
    onFastMaChange: (String) -> Unit,
    onSlowMaChange: (String) -> Unit,
    onSignalLineChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "MACD 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 빠른 이동평균
                Column(modifier = Modifier.weight(1f)) {
                    Text("빠른 MA", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cortaFastMa,
                        onValueChange = onFastMaChange,
                        placeholder = { Text("12", color = Color(0xFF90A4AE)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 느린 이동평균
                Column(modifier = Modifier.weight(1f)) {
                    Text("느린 MA", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cortaSlowMa,
                        onValueChange = onSlowMaChange,
                        placeholder = { Text("26", color = Color(0xFF90A4AE)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 시그널 라인
                Column(modifier = Modifier.weight(1f)) {
                    Text("시그널", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cortaSignalLine,
                        onValueChange = onSignalLineChange,
                        placeholder = { Text("9", color = Color(0xFF90A4AE)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MACD 설명
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "📊 MACD (12,26,9) 기본 설정",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• MACD Line: EMA(12) - EMA(26)\n" +
                               "• Signal Line: EMA(9) of MACD Line\n" +
                               "• Histogram: MACD Line - Signal Line",
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
private fun AdvancedSettingsCard(
    cortaVolumeFactor: String,
    cortaRsiConfirm: Boolean,
    onVolumeFactorChange: (String) -> Unit,
    onRsiConfirmChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "고급 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 거래량 배율
            Text("거래량 배율", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cortaVolumeFactor,
                onValueChange = onVolumeFactorChange,
                placeholder = { Text("1.5", color = Color(0xFF90A4AE)) },
                supportingText = { Text("평균 거래량 대비 배율 (1.0 = 100%)", color = Color(0xFF90A4AE), fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFF90A4AE)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // RSI 확인
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("RSI 이중 확인", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("RSI로 신호 신뢰도 검증", color = Color(0xFF90A4AE), fontSize = 12.sp)
                }
                Switch(
                    checked = cortaRsiConfirm,
                    onCheckedChange = onRsiConfirmChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFD700),
                        checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 코르타 전략 설명
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "🚀 코르타 복합 전략",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• MACD 크로스오버 + 히스토그램 확인\n" +
                               "• 거래량 급증 검증 (평균 대비 ${cortaVolumeFactor}배)\n" +
                               "• ${if (cortaRsiConfirm) "RSI 이중 확인으로 정확도 향상" else "RSI 확인 비활성화"}\n" +
                               "• 다중 지표 결합으로 높은 신뢰도",
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
private fun BasicSettingsCard(
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
                            focusedBorderColor = Color(0xFFFFD700),
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
                            focusedBorderColor = Color(0xFFFFD700),
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
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFF90A4AE)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InvestmentCard(
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
                    focusedBorderColor = Color(0xFFFFD700),
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
                        checkedThumbColor = Color(0xFFFFD700),
                        checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.5f)
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
                        checkedThumbColor = Color(0xFFFFD700),
                        checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}