// CciSignalSettingsScreen.kt - CCI 시세포착 설정 화면 (분 단위 인터벌)

package com.example.ver20.view.signal

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
fun CciSignalSettingsScreen(
    onBackClick: () -> Unit,
    onSettingsSaved: (MarketSignalConfig) -> Unit
) {
    val context = LocalContext.current
    val userService = remember { UserService() }
    val marketSignalService = remember { MarketSignalService() }
    val coroutineScope = rememberCoroutineScope()

    // 사용자 정보
    var currentUser by remember { mutableStateOf<UserData?>(null) }

    // 설정값들 (진입체크 인터벌을 분 단위로 변경)
    var selectedSymbol by remember { mutableStateOf("BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf("15m") }
    var checkInterval by remember { mutableStateOf("15") } // 분 단위로 변경
    var cciPeriod by remember { mutableStateOf("20") }
    var cciBreakoutValue by remember { mutableStateOf("100") }
    var cciEntryValue by remember { mutableStateOf("90") }
    var seedMoney by remember { mutableStateOf("1000") }
    var isActive by remember { mutableStateOf(true) }

    // UI 상태
    var isSaving by remember { mutableStateOf(false) }
    var showSymbolDialog by remember { mutableStateOf(false) }

    // 사용자 정보 로드
    LaunchedEffect(Unit) {
        currentUser = userService.getUserFromPreferences(context)
    }

    // 시간대 옵션
    val timeframeOptions = listOf(
        "15m" to "15분",
        "1h" to "1시간",
        "4h" to "4시간"
    )

    // 인기 심볼 목록
    val popularSymbols = listOf(
        "BTCUSDT", "ETHUSDT", "BNBUSDT", "XRPUSDT", "ADAUSDT",
        "DOGEUSDT", "SOLUSDT", "DOTUSDT", "MATICUSDT", "LTCUSDT"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CCI 시세포착 설정") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CCI 전략 설명 카드
                CciExplanationCard()

                // 기본 설정 카드
                BasicSettingsCard(
                    selectedSymbol = selectedSymbol,
                    selectedTimeframe = selectedTimeframe,
                    checkInterval = checkInterval,
                    seedMoney = seedMoney,
                    onSymbolClick = { showSymbolDialog = true },
                    onTimeframeChange = { selectedTimeframe = it },
                    onCheckIntervalChange = { checkInterval = it },
                    onSeedMoneyChange = { seedMoney = it },
                    timeframeOptions = timeframeOptions
                )

                // CCI 지표 설정 카드
                CciIndicatorSettingsCard(
                    cciPeriod = cciPeriod,
                    cciBreakoutValue = cciBreakoutValue,
                    cciEntryValue = cciEntryValue,
                    onCciPeriodChange = { cciPeriod = it },
                    onBreakoutValueChange = { cciBreakoutValue = it },
                    onEntryValueChange = { cciEntryValue = it }
                )

                // 활성화 상태 설정
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "설정 활성화",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }

                // 저장 버튼
                Button(
                    onClick = {
                        if (currentUser == null) {
                            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (validateInputs(
                                context = context,
                                interval = checkInterval,
                                period = cciPeriod,
                                breakout = cciBreakoutValue,
                                entry = cciEntryValue,
                                seed = seedMoney
                            )) {

                            isSaving = true
                            coroutineScope.launch {
                                // 분 단위를 초 단위로 변환하여 저장
                                val intervalInSeconds = checkInterval.toInt() * 60

                                val config = MarketSignalConfig(
                                    username = currentUser!!.username,
                                    signalType = "CCI",
                                    symbol = selectedSymbol,
                                    timeframe = selectedTimeframe,
                                    checkInterval = intervalInSeconds,
                                    cciPeriod = cciPeriod.toInt(),
                                    cciBreakoutValue = cciBreakoutValue.toDouble(),
                                    cciEntryValue = cciEntryValue.toDouble(),
                                    seedMoney = seedMoney.toDouble(),
                                    isActive = isActive
                                )

                                marketSignalService.saveSignalConfig(config) { success, message ->
                                    isSaving = false
                                    if (success) {
                                        Toast.makeText(context, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                        onSettingsSaved(config)
                                    } else {
                                        Toast.makeText(context, "저장 실패: $message", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "설정 저장",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 추가 하단 여백
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 심볼 선택 다이얼로그
    if (showSymbolDialog) {
        SymbolSelectionDialog(
            symbols = popularSymbols,
            selectedSymbol = selectedSymbol,
            onSymbolSelected = {
                selectedSymbol = it
                showSymbolDialog = false
            },
            onDismiss = { showSymbolDialog = false }
        )
    }
}

@Composable
private fun CciExplanationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "CCI 시세포착 전략",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "• 롱 진입: CCI가 -돌파값 아래로 이탈 이후 -진입값 안으로 진입시\n" +
                        "• 숏 진입: CCI가 +돌파값 위로 이탈 이후 +진입값 안으로 진입시\n" +
                        "• 설정된 인터벌마다 자동으로 조건을 체크합니다\n" +
                        "• 조건 충족 시 시세포착 알림이 생성됩니다",
                fontSize = 13.sp,
                color = Color(0xFF424242),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun BasicSettingsCard(
    selectedSymbol: String,
    selectedTimeframe: String,
    checkInterval: String,
    seedMoney: String,
    onSymbolClick: () -> Unit,
    onTimeframeChange: (String) -> Unit,
    onCheckIntervalChange: (String) -> Unit,
    onSeedMoneyChange: (String) -> Unit,
    timeframeOptions: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "기본 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 코인 선택
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("코인:", fontSize = 14.sp, color = Color(0xFF666666))
                Button(
                    onClick = onSymbolClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE3F2FD)
                    )
                ) {
                    Text(
                        selectedSymbol,
                        color = Color(0xFF1976D2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 시간대 선택
            Text("시간대:", fontSize = 14.sp, color = Color(0xFF666666))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeframeOptions.forEach { (value, label) ->
                    FilterChip(
                        onClick = { onTimeframeChange(value) },
                        label = { Text(label) },
                        selected = selectedTimeframe == value
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 진입체크 인터벌 (분 단위)
            OutlinedTextField(
                value = checkInterval,
                onValueChange = onCheckIntervalChange,
                label = { Text("진입체크 인터벌 (분)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "최소 15분부터 설정 가능 (백그라운드 제약)",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 진입시드
            OutlinedTextField(
                value = seedMoney,
                onValueChange = onSeedMoneyChange,
                label = { Text("진입시드 (USDT)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CciIndicatorSettingsCard(
    cciPeriod: String,
    cciBreakoutValue: String,
    cciEntryValue: String,
    onCciPeriodChange: (String) -> Unit,
    onBreakoutValueChange: (String) -> Unit,
    onEntryValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "CCI 지표 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9C27B0)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CCI 기간
            OutlinedTextField(
                value = cciPeriod,
                onValueChange = onCciPeriodChange,
                label = { Text("CCI 기간") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CCI 돌파값
            OutlinedTextField(
                value = cciBreakoutValue,
                onValueChange = onBreakoutValueChange,
                label = { Text("CCI 돌파값") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "CCI가 이 값을 벗어나야 신호 감지 조건 시작",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CCI 진입값
            OutlinedTextField(
                value = cciEntryValue,
                onValueChange = onEntryValueChange,
                label = { Text("CCI 진입값") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "CCI가 이 값 안으로 들어오면 신호 발생",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            )
        }
    }
}

// 검증 함수 (분 단위 검증)
private fun validateInputs(
    context: Context,
    interval: String,
    period: String,
    breakout: String,
    entry: String,
    seed: String
): Boolean {
    try {
        val intervalInt = interval.toInt()
        val periodInt = period.toInt()
        val breakoutDouble = breakout.toDouble()
        val entryDouble = entry.toDouble()
        val seedDouble = seed.toDouble()

        when {
            intervalInt < 15 -> {
                Toast.makeText(context, "진입체크 인터벌은 15분 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            intervalInt > 1440 -> {
                Toast.makeText(context, "진입체크 인터벌은 24시간(1440분) 이하여야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            periodInt < 5 -> {
                Toast.makeText(context, "CCI 기간은 5 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            periodInt > 200 -> {
                Toast.makeText(context, "CCI 기간은 200 이하여야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            breakoutDouble <= 0 -> {
                Toast.makeText(context, "CCI 돌파값은 0보다 커야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            entryDouble <= 0 -> {
                Toast.makeText(context, "CCI 진입값은 0보다 커야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            entryDouble >= breakoutDouble -> {
                Toast.makeText(context, "CCI 진입값은 돌파값보다 작아야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            seedDouble < 10 -> {
                Toast.makeText(context, "진입시드는 10 USDT 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            seedDouble > 100000 -> {
                Toast.makeText(context, "진입시드는 100,000 USDT 이하여야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true

    } catch (e: NumberFormatException) {
        Toast.makeText(context, "올바른 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show()
        return false
    }
}

@Composable
private fun SymbolSelectionDialog(
    symbols: List<String>,
    selectedSymbol: String,
    onSymbolSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("코인 선택") },
        text = {
            LazyColumn {
                items(symbols) { symbol ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSymbolSelected(symbol) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = symbol == selectedSymbol,
                            onClick = { onSymbolSelected(symbol) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(symbol)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}