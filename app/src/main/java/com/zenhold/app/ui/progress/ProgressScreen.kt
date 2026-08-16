package com.zenhold.app.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.components.NeumorphicAction
import com.zenhold.app.ui.util.formatDuration
import java.time.YearMonth

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onPeriodSelected: (ProgressPeriod) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier.fillMaxSize()) {
      val compactWidth = maxWidth < 420.dp
      Column(
        Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .widthIn(max = 900.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(if (maxWidth < 360.dp) 16.dp else 24.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Text("Мой прогресс", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
        PeriodSelector(state.selectedPeriod, onPeriodSelected)
        state.comparisonPercent?.let { percent ->
            Spacer(Modifier.height(12.dp))
            Text(
                if (percent >= 0f) "Среднее выше предыдущего периода на ${percent.toInt()}%"
                else "Среднее ниже предыдущего периода на ${kotlin.math.abs(percent).toInt()}%",
                color = if (percent >= 0f) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        if (compactWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Личный рекорд", formatDuration(state.personalBestMillis), Modifier.fillMaxWidth())
                MetricCard("Среднее · 10", formatDuration(state.recentAverageMillis), Modifier.fillMaxWidth())
                MetricCard(
                    "Комфортное среднее",
                    if (state.ratedAttemptCount == 0) "—" else formatDuration(state.comfortableAverageMillis),
                    Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Личный рекорд", formatDuration(state.personalBestMillis), Modifier.weight(1f))
                MetricCard("Среднее · 10", formatDuration(state.recentAverageMillis), Modifier.weight(1f))
                MetricCard(
                    "Комфортное среднее",
                    if (state.ratedAttemptCount == 0) "—" else formatDuration(state.comfortableAverageMillis),
                    Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        NeumorphicPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Динамика по тренировкам", fontWeight = FontWeight.SemiBold)
                Text("Максимум и среднее", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                if (state.sessions.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().height(220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Здесь появится ваша динамика", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Завершите первую тренировку", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.38f))
                    }
                } else {
                    LineChart(points = state.sessions, modifier = Modifier.fillMaxWidth().height(220.dp))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Legend(accent, "Максимум")
                    Legend(MaterialTheme.colorScheme.onSurface.copy(alpha = .52f), "Среднее")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        MonthCalendar(state)
        Spacer(Modifier.height(20.dp))
        Text("Достижения", fontWeight = FontWeight.SemiBold)
        Text(
            "Без соревнования с другими — только спокойные личные ориентиры",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
        ) {
            items(state.achievements, key = { it.title }) { achievement ->
                AchievementCard(achievement)
            }
        }
        Spacer(Modifier.height(20.dp))
        if (state.months.isNotEmpty()) {
            Text("Прогресс по месяцам", fontWeight = FontWeight.SemiBold)
            Text(
                "Среднее и лучший результат сохраняются за каждый месяц",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(190.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.months.asReversed(), key = { it.yearMonth.toString() }) { month ->
                    MonthCard(month)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        Text("Путь к спокойствию", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.sessions.size / 20f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
        Text("${state.sessions.size} из 20 тренировок", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
      }
    }
}

@Composable
private fun PeriodSelector(selected: ProgressPeriod, onSelected: (ProgressPeriod) -> Unit) {
    val periods = listOf(
        ProgressPeriod.Week to "7 дней",
        ProgressPeriod.Month to "Месяц",
        ProgressPeriod.Quarter to "3 месяца",
        ProgressPeriod.Year to "Год",
        ProgressPeriod.All to "Всё",
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(periods, key = { it.first.name }) { (period, label) ->
            val active = selected == period
            NeumorphicAction(
                onClick = { onSelected(period) },
                modifier = Modifier.heightIn(min = 42.dp).widthIn(min = 78.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    label,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MonthCalendar(state: ProgressUiState) {
    val month = YearMonth.now()
    val accent = MaterialTheme.colorScheme.primary
    val activity = state.calendarDays.associateBy { it.dayOfMonth }
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = List<Int?>(offset) { null } + (1..month.lengthOfMonth()).map { it }
    NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = 9.dp) {
        Column(Modifier.padding(18.dp)) {
            Text("Календарь · ${state.calendarMonthLabel}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("П", "В", "С", "Ч", "П", "С", "В").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
                    (week + List(7 - week.size) { null }).forEach { day ->
                        val trained = day?.let(activity::get)
                        Box(
                            Modifier.weight(1f).height(34.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day != null) {
                                if (trained != null) {
                                    Canvas(Modifier.size(30.dp)) {
                                        drawCircle(accent.copy(alpha = .2f))
                                    }
                                }
                                Text(
                                    day.toString(),
                                    color = if (trained != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (trained != null) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    NeumorphicPanel(
        modifier = Modifier.width(216.dp).heightIn(min = 104.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = if (achievement.unlocked) 10.dp else 5.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (achievement.unlocked) "ОТКРЫТО" else "В ПРОЦЕССЕ",
                color = if (achievement.unlocked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
            )
            Text(achievement.title, fontWeight = FontWeight.SemiBold)
            Text(achievement.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MonthCard(month: MonthPoint) {
    NeumorphicPanel(
        modifier = Modifier.width(224.dp).heightIn(min = 158.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 9.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(month.label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Лучшее", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(month.maximumMillis), fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Среднее", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(month.averageMillis))
            }
            Text(
                "${month.sessionCount} сесс. · ${month.attemptCount} подх.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
            )
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    NeumorphicPanel(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Text("  $text", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LineChart(points: List<SessionPoint>, modifier: Modifier = Modifier) {
    val maxValue = points.maxOfOrNull { it.maximumMillis }?.coerceAtLeast(1L) ?: 1L
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)
    val averageColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f)
    val accent = MaterialTheme.colorScheme.primary
    val lastPoint = points.lastOrNull()
    val chartDescription = if (lastPoint == null) {
        "Данных для графика пока нет"
    } else {
        "График за ${points.size} тренировок. Последний максимум ${formatDuration(lastPoint.maximumMillis)}, среднее ${formatDuration(lastPoint.averageMillis)}"
    }
    Canvas(modifier.semantics { contentDescription = chartDescription }) {
        val horizontalPadding = 8.dp.toPx()
        val verticalPadding = 12.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - verticalPadding * 2
        repeat(4) { index ->
            val y = verticalPadding + chartHeight * index / 3f
            drawLine(gridColor, Offset(horizontalPadding, y), Offset(size.width - horizontalPadding, y), 1.dp.toPx())
        }

        fun xAt(index: Int): Float = if (points.size == 1) size.width / 2f
        else horizontalPadding + chartWidth * index / (points.lastIndex.toFloat())
        fun yAt(value: Long): Float = verticalPadding + chartHeight * (1f - value.toFloat() / maxValue)
        fun pathFor(selector: (SessionPoint) -> Long) = Path().apply {
            points.forEachIndexed { index, point ->
                val x = xAt(index)
                val y = yAt(selector(point))
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(pathFor { it.averageMillis }, averageColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawPath(pathFor { it.maximumMillis }, accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { index, point ->
            drawCircle(accent, 4.dp.toPx(), Offset(xAt(index), yAt(point.maximumMillis)))
        }
    }
}
