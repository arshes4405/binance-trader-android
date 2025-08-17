// CciSignalSettingsScreen.kt - CCI 시세포착 설정 화면

package com.example.ver20.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import com.example.ver20.dao.*
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
    
    // 설정값들
    var selectedSymbol by remember { mutableStateOf("BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf("15m") }
    var checkInterval by remember { mutableStateOf("30") } // 초
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

    // 인기 코인 목록
    val popularSymbols = listOf(
        "BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "DOTUSDT",
        "LINKUSDT", "LTCUSDT", "XRPUSDT", "SOLUSDT", "DOGEUSDT"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CCI 시세포착 설정",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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

            // 활성화 설정
            ActiveSettingCard(
                isActive = isActive,
                onActiveChange = { isActive = it }
            )

            // 저장 버튼
            SaveButton(
                isSaving = isSaving,
                onClick = {
                    if (currentUser == null) {
                        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                        return@SaveButton
                    }

                    // 입력값 검증
                    if (!validateInputs(
                        checkInterval, cciPeriod, cciBreakoutValue, 
                        cciEntryValue, seedMoney, context
                    )) {
                        return@SaveButton
                    }

                    isSaving = true

                    val config = MarketSignalConfig(
                        username = currentUser!!.username,
                        signalType = "CCI",
                        symbol = selectedSymbol,
                        timeframe = selectedTimeframe,
                        checkInterval = checkInterval.toInt(),
                        cciPeriod = cciPeriod.toInt(),
                        cciBreakoutValue = cciBreakoutValue.toDouble(),
                        cciEntryValue = cciEntryValue.toDouble(),
                        seedMoney = seedMoney.toDouble(),
                        isActive = isActive
                    )

                    coroutineScope.launch {
                        marketSignalService.saveSignalConfig(config) { success, message ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, "CCI 시세포착 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                onSettingsSaved(config)
                            } else {
                                Toast.makeText(context, "저장 실패: $message", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
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

            // 진입체크 인터벌
            OutlinedTextField(
                value = checkInterval,
                onValueChange = onCheckIntervalChange,
                label = { Text("진입체크 인터벌 (초)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
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
                        "돌파 후 이 값 안으로 들어오면 신호 발생",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            )
        }
    }
}

@Composable
private fun ActiveSettingCard(
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
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
                    "시세포착 활성화",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
                Text(
                    if (isActive) "설정된 조건에 따라 실시간 모니터링" else "시세포착이 비활성화됨",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
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

@Composable
private fun SaveButton(
    isSaving: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2196F3)
        )
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            if (isSaving) "저장 중..." else "설정 저장",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
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
        title = {
            Text("코인 선택")
        },
        text = {
            Column {
                symbols.chunked(2).forEach { rowSymbols ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSymbols.forEach { symbol ->
                            FilterChip(
                                onClick = { onSymbolSelected(symbol) },
                                label = { 
                                    Text(
                                        symbol.replace("USDT", ""),
                                        fontSize = 12.sp
                                    )
                                },
                                selected = selectedSymbol == symbol,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // 홀수 개수일 때 빈 공간 채우기
                        if (rowSymbols.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

// 입력값 검증 함수
private fun validateInputs(
    checkInterval: String,
    cciPeriod: String,
    cciBreakoutValue: String,
    cciEntryValue: String,
    seedMoney: String,
    context: android.content.Context
): Boolean {
    try {
        val intervalInt = checkInterval.toInt()
        val periodInt = cciPeriod.toInt()
        val breakoutDouble = cciBreakoutValue.toDouble()
        val entryDouble = cciEntryValue.toDouble()
        val seedDouble = seedMoney.toDouble()

        when {
            intervalInt < 10 -> {
                Toast.makeText(context, "진입체크 인터벌은 10초 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            intervalInt > 3600 -> {
                Toast.makeText(context, "진입체크 인터벌은 1시간(3600초) 이하여야 합니다.", Toast.LENGTH_SHORT).show()
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