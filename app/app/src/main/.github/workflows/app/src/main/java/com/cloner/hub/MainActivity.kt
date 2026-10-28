package com.cloner.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0B0C16)
private val Panel = Color(0x1AFFFFFF)
private val PanelStroke = Color(0x33FFFFFF)
private val Violet = Color(0xFF7C5CFF)
private val Aqua = Color(0xFF29E0C6)
private val Rose = Color(0xFFFF6B9A)
private val TextHi = Color(0xFFF4F4FA)
private val TextLo = Color(0xFF9A9CB5)

data class AiApp(val name: String, val pkg: String, val accent: Color)

val aiApps = listOf(
    AiApp("ChatGPT", "com.openai.chatgpt", Aqua),
    AiApp("Gemini", "com.google.android.apps.bard", Violet),
    AiApp("Claude", "com.anthropic.claude", Rose),
    AiApp("Perplexity", "ai.perplexity.app.android", Aqua),
    AiApp("Copilot", "com.microsoft.copilot", Violet),
    AiApp("DeepSeek", "com.deepseek.chat", Rose),
)

const val MAX_CLONES = 10

interface CloneEngine {
    fun create(pkg: String, index: Int)
    fun remove(pkg: String, index: Int)
}

class PlaceholderEngine : CloneEngine {
    override fun create(pkg: String, index: Int) {}
    override fun remove(pkg: String, index: Int) {}
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CloneHubApp(PlaceholderEngine()) }
    }
}

@Composable
fun CloneHubApp(engine: CloneEngine) {
    val ctx = LocalContext.current
    val counts = remember { mutableStateMapOf<String, Int>() }
    val installed = remember {
        aiApps.associate { app ->
            app.pkg to runCatching { ctx.packageManager.getPackageInfo(app.pkg, 0) }.isSuccess
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Ink)) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            GlowOrbs()
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Header(total = counts.values.sum()) }
                items(aiApps, key = { it.pkg }) { app ->
                    AppCard(
                        app = app,
                        installed = installed[app.pkg] == true,
                        count = counts[app.pkg] ?: 0,
                        onAdd = {
                            val n = counts[app.pkg] ?: 0
                            if (n < MAX_CLONES) { engine.create(app.pkg, n + 1); counts[app.pkg] = n + 1 }
                        },
                        onRemove = {
                            val n = counts[app.pkg] ?: 0
                            if (n > 0) { engine.remove(app.pkg, n); counts[app.pkg] = n - 1 }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(total: Int) {
    Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Text("Clone Hub", color = TextHi, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("$total active clones", color = TextLo, fontSize = 15.sp)
    }
}

@Composable
private fun GlowOrbs() {
    val t = rememberInfiniteTransition(label = "orbs")
    val shift by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "s"
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.size(280.dp).offset(x = (-60 + 80 * shift).dp, y = (40 + 30 * shift).dp)
                .blur(90.dp).background(Violet.copy(alpha = .55f), CircleShape)
        )
        Box(
            Modifier.size(240.dp).align(Alignment.BottomEnd)
                .offset(x = (50 - 70 * shift).dp, y = (-80 + 40 * shift).dp)
                .blur(90.dp).background(Aqua.copy(alpha = .40f), CircleShape)
        )
    }
}

@Composable
private fun AppCard(
    app: AiApp, installed: Boolean, count: Int, onAdd: () -> Unit, onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Panel).border(1.dp, PanelStroke, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(app.accent, app.accent.copy(alpha = .35f)))),
            contentAlignment = Alignment.Center
        ) { Text(app.name.first().toString(), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.name, color = TextHi, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (installed) "Installed" else "Not installed",
                color = if (installed) Aqua else TextLo, fontSize = 13.sp
            )
        }

        PressButton("−", enabled = count > 0, filled = false, onClick = onRemove)
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                    (slideOutVertically { if (up) -it else it } + fadeOut())
            },
            label = "count", modifier = Modifier.width(44.dp)
        ) { n ->
            Text("$n", color = TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        PressButton("+", enabled = installed && count < MAX_CLONES, filled = true, onClick = onAdd)
    }
}

@Composable
private fun PressButton(label: String, enabled: Boolean, filled: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) .88f else 1f, spring(dampingRatio = .5f, stiffness = 600f), label = "press"
    )
    Box(
        Modifier.size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else .35f }
            .clip(CircleShape)
            .background(if (filled) Brush.linearGradient(listOf(Violet, Rose)) else Brush.linearGradient(listOf(Panel, Panel)))
            .border(1.dp, PanelStroke, CircleShape)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
}
