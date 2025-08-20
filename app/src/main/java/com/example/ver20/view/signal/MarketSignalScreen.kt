// MarketSignalScreen.kt - 시세포착 메인 화면 (콤팩트 버전)

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
fun MarketSignalScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val userService = remember { UserService() }
    val marketSignalService = remember { MarketSignalService() }
    val coroutineScope = rememberCoroutineScope()

    // 상태 변수
    var currentUser by remember { mutableStateOf<UserData?>(null) }
    var showCciSettings by remember { mutableStateOf(false) }
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

    // arshes 계정은 제한 없음
    val isUnlimitedUser = currentUser?.username == "arshes"
    val maxConfigs = if (isUnlimitedUser) Int.MAX_VALUE else 4

    // CCI 설정 화면
    if (showCciSettings) {
        CciSignalSettingsScreen(
            editConfig = selectedConfig,
            onBackClick = {
                showCciSettings = false
                selectedConfig = null
            },
            onSettingsSaved = { config ->
                loadData()
                showCciSettings = false
                selectedConfig = null
            }
        )
        return
    }

    // 롱/숏 신호 분리
    val longSignals = recentSignals.filter { it.direction == "LONG" }
    val shortSignals = recentSignals.filter { it.direction == "SHORT" }

    // 메인 화면
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F23))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 콤팩트 헤더
        item {
            CompactHeader(
                currentUser = currentUser,
                activeCount = signalConfigs.count { it.isActive },
                longSignalCount = longSignals.size,
                shortSignalCount = shortSignals.size,
                configCount = signalConfigs.size,
                maxConfigs = maxConfigs,
                isUnlimitedUser = isUnlimitedUser,
                onRefresh = { loadData() },
                onAddNew = {
                    if (signalConfigs.size >= maxConfigs) {
                        // 최대 제한 알림 (일반 사용자만)
                        return@CompactHeader
                    }
                    selectedConfig = null
                    showCciSettings = true
                }
            )
        }

        // 활성 설정 목록
        if (signalConfigs.isNotEmpty()) {
            // arshes는 모든 설정 표시, 일반 사용자는 4개까지만
            val displayConfigs = if (isUnlimitedUser) signalConfigs else signalConfigs.take(4)

            items(displayConfigs) { config ->
                CompactConfigCard(
                    config = config,
                    onEdit = {
                        selectedConfig = config
                        showCciSettings = true
                    },
                    onToggle = {
                        coroutineScope.launch {
                            val updatedConfig = config.copy(isActive = !config.isActive)
                            marketSignalService.saveSignalConfig(updatedConfig) { success, _ ->
                                if (success) loadData()
                            }
                        }
                    },
                    onDelete = {
                        coroutineScope.launch {
                            marketSignalService.deleteSignalConfig(config.id) { success, _ ->
                                if (success) loadData()
                            }
                        }
                    }
                )
            }

            // 일반 사용자만 4개 제한 안내
            if (!isUnlimitedUser && signalConfigs.size >= 4) {
                item {
                    Text(
                        text = "⚠️ 최대 4개까지 설정 가능",
                        color = Color(0xFFFF9800),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    )
                }
            }

            // arshes 계정 표시
            if (isUnlimitedUser && signalConfigs.size > 4) {
                item {
                    Text(
                        text = "🔓 무제한 계정 (${signalConfigs.size}개 설정)",
                        color = Color(0xFF4FC3F7),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    )
                }
            }
        }

        // 롱 신호 섹션
        if (longSignals.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "롱 신호 (${longSignals.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Divider(
                        modifier = Modifier.weight(2f),
                        color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                }
            }

            items(longSignals.take(5)) { signal ->
                CompactSignalCard(signal = signal)
            }

            if (longSignals.size > 5) {
                item {
                    Text(
                        text = "+${longSignals.size - 5}개 더",
                        color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(2.dp)
                    )
                }
            }
        }

        // 숏 신호 섹션
        if (shortSignals.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "숏 신호 (${shortSignals.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Divider(
                        modifier = Modifier.weight(2f),
                        color = Color(0xFFF44336).copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                }
            }

            items(shortSignals.take(5)) { signal ->
                CompactSignalCard(signal = signal)
            }

            if (shortSignals.size > 5) {
                item {
                    Text(
                        text = "+${shortSignals.size - 5}개 더",
                        color = Color(0xFFF44336).copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(2.dp)
                    )
                }
            }
        }

        // 빈 상태 (콤팩트)
        if (!isLoading && signalConfigs.isEmpty()) {
            item {
                CompactEmptyState(
                    isUnlimitedUser = isUnlimitedUser,
                    onAddStrategy = {
                        selectedConfig = null
                        showCciSettings = true
                    }
                )
            }
        }

        // 로딩
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4FC3F7),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactHeader(
    currentUser: UserData?,
    activeCount: Int,
    longSignalCount: Int,
    shortSignalCount: Int,
    configCount: Int,
    maxConfigs: Int,
    isUnlimitedUser: Boolean,
    onRefresh: () -> Unit,
    onAddNew: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽: 제목과 통계
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "시세포착",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isUnlimitedUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔓",
                            fontSize = 14.sp
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "활성 $activeCount",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "롱 $longSignalCount",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "숏 $shortSignalCount",
                        fontSize = 11.sp,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        text = if (isUnlimitedUser) "$configCount 개" else "$configCount/4",
                        fontSize = 11.sp,
                        color = if (isUnlimitedUser) Color(0xFF4FC3F7)
                        else if (configCount >= 4) Color(0xFFFF9800)
                        else Color(0xFF90A4AE)
                    )
                }
            }

            // 오른쪽: 버튼들
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = Color(0xFF4FC3F7),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onAddNew,
                    modifier = Modifier.size(36.dp),
                    enabled = isUnlimitedUser || configCount < maxConfigs
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "추가",
                        tint = if (isUnlimitedUser || configCount < maxConfigs)
                            Color(0xFF4CAF50) else Color(0xFF666666),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactConfigCard(
    config: MarketSignalConfig,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isActive) Color(0xFF0D4F3C) else Color(0xFF1A1A2E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽: 정보
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = config.symbol.replace("USDT", ""),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.isActive) Color(0xFF4CAF50) else Color.White
                    )
                    Text(
                        text = config.timeframe,
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier
                            .background(
                                Color(0xFF90A4AE).copy(alpha = 0.2f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    if (config.autoTrading) {
                        Text(
                            text = "AUTO",
                            fontSize = 9.sp,
                            color = Color(0xFFFF9800),
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
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE)
                )
            }

            // 오른쪽: 컨트롤
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "편집",
                        tint = Color(0xFF4FC3F7),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Switch(
                    checked = config.isActive,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.height(24.dp),
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
private fun CompactSignalCard(signal: MarketSignal) {
    val isLong = signal.direction == "LONG"
    val textColor = when {
        signal.isRead -> Color(0xFF90A4AE)
        isLong -> Color(0xFF4CAF50)
        else -> Color(0xFFF44336)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (signal.isRead) Color(0xFF2A2A2A).copy(alpha = 0.3f)
                else if (isLong) Color(0xFF0D4F3C).copy(alpha = 0.3f)
                else Color(0xFF4F0D0D).copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = signal.getDirectionIcon(),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = signal.symbol.replace("USDT", ""),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = signal.direction,
                fontSize = 10.sp,
                color = textColor
            )
        }

        Text(
            text = String.format("%.1f", signal.price),
            fontSize = 11.sp,
            color = Color(0xFF90A4AE)
        )

        Text(
            text = signal.getFormattedTime(),
            fontSize = 10.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun CompactEmptyState(isUnlimitedUser: Boolean, onAddStrategy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = Color(0xFF4FC3F7),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "시세포착 전략 없음",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isUnlimitedUser)
                    "🔓 무제한으로 CCI 전략을 추가할 수 있습니다"
                else
                    "최대 4개까지 CCI 전략을 추가할 수 있습니다",
                fontSize = 12.sp,
                color = if (isUnlimitedUser) Color(0xFF4FC3F7) else Color(0xFF90A4AE),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAddStrategy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("첫 전략 추가", fontSize = 12.sp)
            }
        }
    }
}