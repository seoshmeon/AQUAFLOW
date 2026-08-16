package com.zenhold.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenhold.app.ui.components.NeoTactilePrimaryAction
import com.zenhold.app.ui.components.NeumorphicAction
import com.zenhold.app.ui.components.NeumorphicPanel

@Composable
fun OnboardingScreen(onComplete: () -> Unit, modifier: Modifier = Modifier) {
    var page by remember { mutableIntStateOf(0) }
    var accepted by remember { mutableStateOf(false) }
    BackHandler(enabled = page > 0) { page-- }

    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .safeDrawingPadding()
                .padding(horizontal = if (maxWidth < 360.dp) 16.dp else 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AQUAFLOW", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier.width(if (index == page) 30.dp else 9.dp)
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            AnimatedContent(targetState = page, label = "onboarding") { current ->
                when (current) {
                    0 -> IntroCard(
                        icon = Icons.Rounded.Air,
                        title = "Спокойная сила дыхания",
                        body = "AQUAFLOW помогает тренировать комфортную задержку без гонки за рекордом. Останавливайтесь при первых признаках дискомфорта.",
                    )
                    1 -> IntroCard(
                        icon = Icons.Rounded.VisibilityOff,
                        title = "Во время задержки — без цифр",
                        body = "Таймер работает скрыто. Завершите подход двойным тапом по выделенной зоне — результат появится только во время восстановления.",
                    )
                    else -> SafetyConsent(accepted = accepted, onAccepted = { accepted = it })
                }
            }
            Spacer(Modifier.height(24.dp))
            NeoTactilePrimaryAction(
                onClick = { if (page < 2) page++ else onComplete() },
                enabled = page < 2 || accepted,
                modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp)
                    .alpha(if (page < 2 || accepted) 1f else .48f),
            ) {
                Text(
                    if (page < 2) "Продолжить" else "Начать спокойно",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (page > 0) {
                Spacer(Modifier.height(12.dp))
                NeumorphicAction(
                    onClick = { page-- },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Назад") }
            }
        }
    }
}

@Composable
private fun IntroCard(icon: ImageVector, title: String, body: String) {
    NeumorphicPanel(
        Modifier.fillMaxWidth().heightIn(min = 330.dp),
        shape = RoundedCornerShape(32.dp),
        elevation = 13.dp,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            NeumorphicPanel(Modifier.size(84.dp), shape = CircleShape, contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SafetyConsent(accepted: Boolean, onAccepted: (Boolean) -> Unit) {
    NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), elevation = 13.dp) {
        Column(Modifier.padding(24.dp)) {
            Icon(Icons.Rounded.HealthAndSafety, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(18.dp))
            Text("Сначала безопасность", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Text("• Практикуйте только сидя или лёжа\n• Никогда не тренируйтесь в воде в одиночку\n• Не используйте приложение за рулём\n• Не гипервентилируйте перед задержкой", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = accepted, onCheckedChange = onAccepted)
                Text("Я понимаю правила и прекращу подход при дискомфорте", Modifier.padding(start = 6.dp))
            }
        }
    }
}
