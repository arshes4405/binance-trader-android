// MarketSignalScreen.kt - 시세포착 메인 화면 (컴팩트 버전)

package com.example.ver20.view.signal

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.dao.mongoDB.UserData
import com.example.ver20.dao.mongoDB.UserService
import com.example.ver20.dao.trading.signal.MarketSignal
import com.example.ver20.dao.trading.signal.MarketSignalConfig
import com.example.ver20.dao.trading.signal.MarketSignalService
import kotlinx.coroutines.launch

@Composable
fun CompactMarketSignalScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val userService = remember { UserService() }
    val marketSignalService = remember { MarketSignalService() }
    val coroutineScope = rememberCoroutineScope()

    // 상태 변수
    var currentUser by remember { mutableStateOf<UserData?>(null) }
    var showStrategySelection by remember { mutableStateOf(false) }
    var showStrategySettings by remember { mutableStateOf(false) }
    var selectedStrategy by remember { mutableStateOf("") }
    var signalConfigs by remember { mutableStateOf<List<MarketSignalConfig>>(emptyList()) }
    var recentSignals by remember { mutableStateOf<List<MarketSignal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedConfig by remember { mutableStateOf<MarketSignalConfig?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var configToDelete by remember { mutableStateOf<MarketSignalConfig?>(null) }

    // 데이터 로드 함수
    fun loadData() {
        currentUser?.let { user ->
            isLoading = true
            marketSignalService.getSignalConfigs(user.username) { configs, _ ->
                configs?.let { signalConfigs = it }
                marketSignalService.getSignals(user.username) { signals, _ ->
                    signals?.let { recentSignals = it }
                    isLoading = false
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog && configToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                configToDelete = null
            },
            title = {
                Text(
                    text = "설정 삭제",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${configToDelete!!.signalType} ${configToDelete!!.symbol.replace("USDT", "")} 설정을 삭제하시겠습니까?",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        configToDelete?.let { config ->
                            coroutineScope.launch {
                                marketSignalService.deleteSignalConfig(config.id) { success, _ ->
                                    if (success) {
                                        loadData()
                                    }
                                    showDeleteDialog = false
                                    configToDelete = null
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = "삭제",
                        color = Color(0xFFF44336)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        configToDelete = null
                    }
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

    // 초기 로드
    LaunchedEffect(Unit) {
        val userData = userService.getUserFromPreferences(context)
        currentUser = userData
        loadData()
    }

    // arshes 계정은 제한 없음, 일반 사용자는 10개까지
    val isUnlimitedUser = currentUser?.username == "arshes"
    val maxConfigs = if (isUnlimitedUser) 10 else 4

    // 전략 선택 화면
    if (showStrategySelection) {
        StrategySelectionScreen(
            onBackClick = { showStrategySelection = false },
            onStrategySelected = { strategy ->
                selectedStrategy = strategy
                showStrategySelection = false
                showStrategySettings = true
            }
        )
        return
    }

    // 전략별 설정 화면
    if (showStrategySettings) {
        when (selectedStrategy) {
            "RSI" -> RsiSignalSettingsScreen(
                editConfig = selectedConfig,
                onBackClick = {
                    showStrategySettings = false
                    selectedConfig = null
                    selectedStrategy = ""
                },
                onSettingsSaved = { config ->
                    showStrategySettings = false
                    selectedConfig = null
                    selectedStrategy = ""
                    loadData()
                }
            )
            "CCI" -> {
                // CCI 설정 화면 (추후 구현)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CCI 설정 화면",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showStrategySettings = false
                            selectedConfig = null
                            selectedStrategy = ""
                        }
                    ) {
                        Text("뒤로가기")
                    }
                }
                return
            }
            "CORTA" -> {
                // 코르타 설정 화면 (추후 구현)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "코르타 설정 화면",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showStrategySettings = false
                            selectedConfig = null
                            selectedStrategy = ""
                        }
                    ) {
                        Text("뒤로가기")
                    }
                }
                return
            }
        }
        return
    }

    // 메인 화면
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // 헤더 (컴팩트 버전)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "시세포착",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = { loadData() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "새로고침",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 통계 카드 (컴팩트 버전)
        CompactStatisticsCard(
            configs = signalConfigs,
            recentSignals = recentSignals
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 전략별 탭 (컴팩트 버전)
        CompactStrategyTabs(
            configs = signalConfigs,
            onStrategyTabClick = { strategy ->
                // 해당 전략의 설정 목록 보기 또는 새 설정 추가
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 설정 목록 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUnlimitedUser) {
                    "설정 목록 (${signalConfigs.size}개)"
                } else {
                    "설정 목록 (${signalConfigs.size}/$maxConfigs)"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (signalConfigs.size < maxConfigs) {
                FloatingActionButton(
                    onClick = { showStrategySelection = true },
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "전략 추가"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 설정 목록
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFFD700)
                )
            }
        } else if (signalConfigs.isEmpty()) {
            EmptyStateCard(
                onAddFirstStrategy = { showStrategySelection = true }
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(signalConfigs) { config ->
                    SignalConfigCard(
                        config = config,
                        onEdit = {
                            selectedConfig = config
                            selectedStrategy = config.signalType
                            showStrategySettings = true
                        },
                        onToggleActive = { newConfig ->
                            // 활성화/비활성화 토글
                            coroutineScope.launch {
                                marketSignalService.saveSignalConfig(newConfig) { success, _ ->
                                    if (success) loadData()
                                }
                            }
                        },
                        onDelete = { configToDeleteParam ->
                            configToDelete = configToDeleteParam
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactStatisticsCard(
    configs: List<MarketSignalConfig>,
    recentSignals: List<MarketSignal>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CompactStatItem("전체", configs.size, Color(0xFF2196F3))
            CompactStatItem("활성", configs.count { it.isActive }, Color(0xFF4CAF50))
            CompactStatItem("자동", configs.count { it.autoTrading }, Color(0xFFFF9800))
            CompactStatItem("신호", recentSignals.count {
                System.currentTimeMillis() - it.timestamp < 24 * 60 * 60 * 1000
            }, Color(0xFFE91E63))
        }
    }
}

@Composable
private fun CompactStatItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun CompactStrategyTabs(
    configs: List<MarketSignalConfig>,
    onStrategyTabClick: (String) -> Unit
) {
    val strategies = listOf("RSI", "CCI", "CORTA")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        strategies.forEach { strategy ->
            val count = configs.count { it.signalType == strategy }
            val color = when (strategy) {
                "RSI" -> Color(0xFF4CAF50)
                "CCI" -> Color(0xFF2196F3)
                "CORTA" -> Color(0xFFFFD700)
                else -> Color.Gray
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStrategyTabClick(strategy) },
                colors = CardDefaults.cardColors(
                    containerColor = if (count > 0) color.copy(alpha = 0.2f) else Color(0xFF2A2A2A)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strategy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (count > 0) color else Color.Gray
                    )
                    Text(
                        text = "${count}개",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalConfigCard(
    config: MarketSignalConfig,
    onEdit: () -> Unit,
    onToggleActive: (MarketSignalConfig) -> Unit,
    onDelete: (MarketSignalConfig) -> Unit
) {
    val strategyColor = Color(config.getStrategyColor())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isActive) Color(0xFF1A1A1A) else Color(0xFF151515)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 전략 아이콘
                    val icon = when (config.signalType) {
                        "RSI" -> Icons.Default.ShowChart
                        "CCI" -> Icons.Default.TrendingUp
                        "CORTA" -> Icons.Default.AutoAwesome
                        else -> Icons.Default.Analytics
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = config.signalType,
                        tint = strategyColor,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = config.signalType,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = strategyColor
                            )

                            Text(
                                text = config.symbol.replace("USDT", ""),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )

                            Text(
                                text = config.timeframe,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .background(
                                        Color.Gray.copy(alpha = 0.2f),
                                        RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )

                            // 롱/숏 표시 추가 (RSI 전략만)
                            if (config.signalType == "RSI") {
                                // 임시로 "롱" 표시, 추후 config.direction 필드 추가 필요
                                Text(
                                    text = "롱", // TODO: config.direction으로 교체
                                    fontSize = 9.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            Color(0xFF4CAF50).copy(alpha = 0.2f),
                                            RoundedCornerShape(3.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            if (config.autoTrading) {
                                Text(
                                    text = "AUTO",
                                    fontSize = 9.sp,
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            Color(0xFFFF9800).copy(alpha = 0.2f),
                                            RoundedCornerShape(3.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = "${config.checkInterval}분 • ${String.format("%.0f", config.seedMoney)}U",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 활성화 스위치와 삭제 버튼
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = config.isActive,
                        onCheckedChange = { isActive ->
                            onToggleActive(config.copy(isActive = isActive))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = strategyColor,
                            checkedTrackColor = strategyColor.copy(alpha = 0.5f)
                        )
                    )

                    IconButton(
                        onClick = { onDelete(config) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 전략별 상세 정보
            Text(
                text = config.getSummary(),
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    onAddFirstStrategy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "시세포착 없음",
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "시세포착 설정이 없습니다",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "RSI, CCI, 코르타 전략 중에서\n원하는 전략을 선택하여 시작하세요",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddFirstStrategy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "첫 번째 전략 추가",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}