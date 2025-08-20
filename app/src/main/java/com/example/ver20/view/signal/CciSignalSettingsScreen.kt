// CciSignalSettingsScreen.kt - CCI 시세포착 설정 화면 (간단한 버전)

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
fun CciSignalSettingsScreen(
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

    // 기본 설정값들
    var selectedSymbol by remember { mutableStateOf(editConfig?.symbol ?: "BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf(editConfig?.timeframe ?: "15m") }
    var checkInterval by remember { mutableStateOf(editConfig?.checkInterval?.toString() ?: "15") }
    var seedMoney by remember { mutableStateOf(editConfig?.seedMoney?.toString() ?: "1000") }
    var isActive by remember { mutableStateOf(editConfig?.isActive ?: true) }
    var enableAutoTrading by remember { mutableStateOf(editConfig?.autoTrading ?: false) }

    // UI 상태
    var isSaving by remember { mutableStateOf(false) }
    var showSymbolMenu by remember { mutableStateOf(false) }
    var showTimeframeMenu by remember { mutableStateOf(false) }

    // 사용자 정보 로드
    LaunchedEffect(Unit) {
        currentUser = userService.getUserFromPreferences(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F23))
    ) {
        // 상단 앱바
        TopAppBar(
            title = {
                Text(
                    text = if (editConfig != null) "설정 편집" else "CCI 전략 설정",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1A1A2E)
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 전략 정보
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
                    onIntervalChange = { checkInterval = it }
                )
            }

            // 투자 설정
            item {
                InvestmentCard(
                    seedMoney = seedMoney,
                    enableAutoTrading = enableAutoTrading,
                    onSeedMoneyChange = { seedMoney = it },
                    onAutoTradingChange = { enableAutoTrading = it }
                )
            }

            // 활성화 설정
            item {
                ActivationCard(
                    isActive = isActive,
                    onToggle = { isActive = it }
                )
            }

            // 저장 버튼
            item {
                Button(
                    onClick = {
                        currentUser?.let { user ->
                            isSaving = true
                            try {
                                val config = MarketSignalConfig(
                                    id = editConfig?.id ?: "",
                                    username = user.username,
                                    signalType = "CCI",
                                    symbol = selectedSymbol,
                                    timeframe = selectedTimeframe,
                                    checkInterval = checkInterval.toIntOrNull() ?: 15,
                                    seedMoney = seedMoney.toDoubleOrNull() ?: 1000.0,
                                    isActive = isActive,
                                    autoTrading = enableAutoTrading
                                )

                                coroutineScope.launch {
                                    marketSignalService.saveSignalConfig(config) { success, message ->
                                        isSaving = false
                                        if (success) {
                                            Toast.makeText(context, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
                                            onSettingsSaved(config)
                                        } else {
                                            Toast.makeText(context, "저장 실패: $message", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                isSaving = false
                                Toast.makeText(context, "입력값을 확인해주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "CCI 시세포착 전략",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "과매수/과매도 구간 포착",
                    fontSize = 14.sp,
                    color = Color(0xFF90A4AE)
                )
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
    onIntervalChange: (String) -> Unit
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

            // 코인 선택
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSymbolClick() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("코인", color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedSymbol, color = Color(0xFF4FC3F7), fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF90A4AE))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 시간대 선택
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTimeframeClick() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("시간대", color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedTimeframe, color = Color(0xFF4FC3F7), fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF90A4AE))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 체크 간격
            Text("체크 간격 (분)", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = checkInterval,
                onValueChange = onIntervalChange,
                placeholder = { Text("15", color = Color(0xFF90A4AE)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4FC3F7),
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
    onSeedMoneyChange: (String) -> Unit,
    onAutoTradingChange: (Boolean) -> Unit
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
                    focusedBorderColor = Color(0xFF4FC3F7),
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

            if (enableAutoTrading) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "⚠️ 실제 자금으로 거래가 실행됩니다",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivationCard(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF0D4F3C) else Color(0xFF1A1A2E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "전략 활성화",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFF4CAF50) else Color.White
                )
                Text(
                    text = if (isActive) "시세를 모니터링합니다" else "비활성화 상태입니다",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                )
            )
        }
    }
}