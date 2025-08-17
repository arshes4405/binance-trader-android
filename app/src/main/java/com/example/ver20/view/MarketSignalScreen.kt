// MarketSignalScreen.kt - 시세포착 메인 화면

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.dao.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun MarketSignalScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userService = remember { UserService() }
    val marketSignalService = remember { MarketSignalService() }
    val coroutineScope = rememberCoroutineScope()

    // 상태 변수들
    var currentUser by remember { mutableStateOf<UserData?>(null) }
    var showSignalTypeSelection by remember { mutableStateOf(false) }
    var showCciSettings by remember { mutableStateOf(false) }
    var showRsiSettings by remember { mutableStateOf(false) }
    var showMaSettings by remember { mutableStateOf(false) }
    var activeSignals by remember { mutableStateOf<List<MarketSignal>>(emptyList()) }
    var signalConfigs by remember { mutableStateOf<List<MarketSignalConfig>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // 사용자 정보 로드
    LaunchedEffect(Unit) {
        val userData = userService.getUserFromPreferences(context)
        currentUser = userData

        // 기존 시세포착 설정 및 신호 로드
        userData?.let { user ->
            isLoading = true
            marketSignalService.getSignalConfigs(user.username) { configs, error ->
                if (error == null && configs != null) {
                    signalConfigs = configs
                }
            }

            marketSignalService.getSignals(user.username) { signals, error ->
                if (error == null && signals != null) {
                    activeSignals = signals
                }
                isLoading = false
            }
        }
    }

    when {
        showCciSettings -> {
            CciSignalSettingsScreen(
                onBackClick = { showCciSettings = false },
                onSettingsSaved = { config ->
                    showCciSettings = false
                    // 설정 저장 후 목록 새로고침
                    currentUser?.let { user ->
                        marketSignalService.getSignalConfigs(user.username) { configs, _ ->
                            configs?.let { signalConfigs = it }
                        }
                    }
                }
            )
        }
        showRsiSettings -> {
            // TODO: RSI 설정 화면 (추후 구현)
            Card(
                modifier = modifier.fillMaxSize().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("RSI 시세포착 설정")
                    Text("추후 구현 예정", color = Color.Gray)
                    Button(onClick = { showRsiSettings = false }) {
                        Text("돌아가기")
                    }
                }
            }
        }
        showMaSettings -> {
            // TODO: MA 설정 화면 (추후 구현)
            Card(
                modifier = modifier.fillMaxSize().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("MA 시세포착 설정")
                    Text("추후 구현 예정", color = Color.Gray)
                    Button(onClick = { showMaSettings = false }) {
                        Text("돌아가기")
                    }
                }
            }
        }
        else -> {
            MainSignalScreen(
                modifier = modifier,
                currentUser = currentUser,
                activeSignals = activeSignals,
                signalConfigs = signalConfigs,
                isLoading = isLoading,
                onCciClick = { showCciSettings = true },
                onRsiClick = { showRsiSettings = true },
                onMaClick = { showMaSettings = true },
                onRefresh = {
                    currentUser?.let { user ->
                        isLoading = true
                        marketSignalService.getSignals(user.username) { signals, _ ->
                            signals?.let { activeSignals = it }
                            isLoading = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MainSignalScreen(
    modifier: Modifier,
    currentUser: UserData?,
    activeSignals: List<MarketSignal>,
    signalConfigs: List<MarketSignalConfig>,
    isLoading: Boolean,
    onCciClick: () -> Unit,
    onRsiClick: () -> Unit,
    onMaClick: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 헤더 카드
        item {
            HeaderCard(currentUser)
        }

        // 시세포착 종류 선택 카드
        item {
            SignalTypeSelectionCard(
                onCciClick = onCciClick,
                onRsiClick = onRsiClick,
                onMaClick = onMaClick
            )
        }

        // 활성 설정 요약
        item {
            ActiveConfigsCard(signalConfigs)
        }

        // 최근 시세포착 신호 제목
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "최근 시세포착 신호",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = Color(0xFF1976D2)
                    )
                }
            }
        }

        // 로딩 또는 시세포착 신호 목록
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (activeSignals.isEmpty()) {
            item {
                EmptySignalsCard()
            }
        } else {
            items(activeSignals) { signal ->
                SignalCard(signal)
            }
        }
    }
}

@Composable
private fun HeaderCard(currentUser: UserData?) {
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
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "시세포착 알림",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    if (currentUser != null) {
                        Text(
                            "사용자: ${currentUser.username}",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "• CCI, RSI, MA 지표를 활용한 자동 시세포착\n" +
                        "• 설정된 조건에 맞는 매매 신호를 실시간 감지\n" +
                        "• 다양한 시간대와 코인에 대한 동시 모니터링",
                fontSize = 13.sp,
                color = Color(0xFF424242),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SignalTypeSelectionCard(
    onCciClick: () -> Unit,
    onRsiClick: () -> Unit,
    onMaClick: () -> Unit
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
                "시세포착 종류 선택",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SignalTypeButton(
                    modifier = Modifier.weight(1f),
                    title = "CCI",
                    subtitle = "채널 지수",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF4CAF50),
                    onClick = onCciClick
                )

                SignalTypeButton(
                    modifier = Modifier.weight(1f),
                    title = "RSI",
                    subtitle = "상대강도",
                    icon = Icons.Default.Speed,
                    color = Color(0xFFFF9800),
                    onClick = onRsiClick
                )

                SignalTypeButton(
                    modifier = Modifier.weight(1f),
                    title = "MA",
                    subtitle = "이동평균",
                    icon = Icons.Default.ShowChart,
                    color = Color(0xFF9C27B0),
                    onClick = onMaClick
                )
            }
        }
    }
}

@Composable
private fun SignalTypeButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ActiveConfigsCard(signalConfigs: List<MarketSignalConfig>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "활성 설정",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (signalConfigs.isEmpty()) {
                Text(
                    "활성화된 시세포착 설정이 없습니다.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                signalConfigs.take(3).forEach { config ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${config.signalType} - ${config.symbol}",
                            fontSize = 14.sp,
                            color = Color(0xFF424242)
                        )
                        Text(
                            config.timeframe,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                if (signalConfigs.size > 3) {
                    Text(
                        "외 ${signalConfigs.size - 3}개 더...",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySignalsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "포착된 시세 신호가 없습니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
            Text(
                "시세포착 설정을 추가하고 시장 상황을 모니터링하세요.",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SignalCard(signal: MarketSignal) {
    val formatter = DecimalFormat("#,##0.00")
    val isLong = signal.direction == "LONG"
    val cardColor = if (isLong) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
    val textColor = if (isLong) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val icon = if (isLong) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${signal.symbol} ${signal.direction}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Text(
                    signal.timestamp.substring(5, 16), // MM-dd HH:mm 형식
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "가격: ${formatter.format(signal.price)} USDT",
                fontSize = 14.sp,
                color = Color(0xFF424242)
            )

            Text(
                "CCI: ${DecimalFormat("#,##0.0").format(signal.cciValue)}",
                fontSize = 14.sp,
                color = Color(0xFF424242)
            )

            Text(
                signal.reason,
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}