package com.zenhold.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirlineSeatFlat
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.NoCrash
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenhold.app.ui.components.NeumorphicPanel

@Composable
fun SafetyScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (maxWidth < 360.dp) 16.dp else 24.dp, vertical = 72.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeumorphicPanel(Modifier.size(54.dp), shape = CircleShape, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Безопасность", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    Text("Комфорт важнее результата", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(28.dp))
            SafetyRule(Icons.Rounded.AirlineSeatFlat, "Только сидя или лёжа", "Не выполняйте задержку стоя: при головокружении можно потерять равновесие.")
            Spacer(Modifier.height(14.dp))
            SafetyRule(Icons.Rounded.Pool, "Никогда не тренируйтесь в воде в одиночку", "Приложение предназначено для сухой практики. В воде необходим подготовленный напарник.")
            Spacer(Modifier.height(14.dp))
            SafetyRule(Icons.Rounded.NoCrash, "Не используйте за рулём", "Также остановите тренировку при входящем звонке или другом отвлекающем событии.")
            Spacer(Modifier.height(14.dp))
            SafetyRule(Icons.Rounded.WarningAmber, "Без гипервентиляции", "Дышите естественно. Частое глубокое дыхание перед задержкой повышает риск потери сознания.")
            Spacer(Modifier.height(24.dp))
            NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), elevation = 8.dp) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Остановитесь сразу", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "При боли, выраженном головокружении, нарушении зрения, онемении или необычной слабости завершите тренировку. AQUAFLOW не является медицинским устройством.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SafetyRule(icon: ImageVector, title: String, description: String) {
    NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = 9.dp) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
