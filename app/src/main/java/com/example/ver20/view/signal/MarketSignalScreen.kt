// MarketSignalScreen.kt - 시세포착 메인 화면 (다중 전략 지원)

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
import androidx.compose.ui.graphics.vector.ImageVector
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
fun MarketSignalScreen(modifier: Modifier = Modifier) {
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
            "CCI" -> CciSignalSettingsScreen(
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
            "CORTA" -> CortaSignalSettingsScreen(
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
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "시세포착",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row {
                IconButton(
                    onClick = { loadData() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = Color(0xFFFFD700)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 통계 카드
        StatisticsCard(
            configs = signalConfigs,
            recentSignals = recentSignals
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 전략별 탭
        StrategyTabs(
            configs = signalConfigs,
            onStrategyTabClick = { strategy ->
                // 해당 전략의 설정 목록 보기 또는 새 설정 추가
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

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
                        onDelete = { configToDelete ->
                            coroutineScope.launch {
                                marketSignalService.deleteSignalConfig(configToDelete.id) { success, _ ->
                                    if (success) loadData()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsCard(
    configs: List<MarketSignalConfig>,
    recentSignals: List<MarketSignal>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "현황",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("전체 설정", configs.size, Color(0xFF2196F3))
                StatItem("활성 설정", configs.count { it.isActive }, Color(0xFF4CAF50))
                StatItem("자동매매", configs.count { it.autoTrading }, Color(0xFFFF9800))
                StatItem("오늘 신호", recentSignals.count {
                    System.currentTimeMillis() - it.timestamp < 24 * 60 * 60 * 1000
                }, Color(0xFFE91E63))
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StrategyTabs(
    configs: List<MarketSignalConfig>,
    onStrategyTabClick: (String) -> Unit
) {
    val strategies = listOf("RSI", "CCI", "CORTA")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strategy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (count > 0) color else Color.Gray
                    )
                    Text(
                        text = "${count}개",
                        fontSize = 12.sp,
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
                            text = "${config.checkInterval}분 • ${config.seedMoney}U",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 활성화 스위치
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 전략별 상세 정보
            Text(
                text = config.getSummary(),
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 액션 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = strategyColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, strategyColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("편집")
                }

                OutlinedButton(
                    onClick = { onDelete(config) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("삭제")
                }
            }
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