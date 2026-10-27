package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.ClutchLogo
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.IconBadge
import com.example.ui.theme.BrandDeepGreen
import com.example.ui.theme.ClutchIcons
import com.example.ui.theme.EggYolkAmber
import com.example.ui.theme.StatusAmberBorderLight
import com.example.ui.theme.StatusGreenBgLight
import com.example.ui.theme.StatusGreenBorderLight
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
    val illustration: @Composable () -> Unit
)

/**
 * First-launch welcome: swipe (or tap Next) through what Clutch does, then Get started.
 * One idea per page, one primary button, and Skip for people in a hurry.
 */
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPage(
            title = "Akwaaba! Welcome to Clutch",
            body = "Your layer farm records, weekly summaries and targets — all in one place."
        ) { WelcomeIllustration() },
        OnboardingPage(
            title = "Daily records in a minute",
            body = "Eggs in crates, feed, deaths and egg weight. The sums are done for you."
        ) { DailyIllustration() },
        OnboardingPage(
            title = "See your performance",
            body = "Laying %, feed, egg mass, deaths and egg quality as simple charts."
        ) { ChartIllustration() },
        OnboardingPage(
            title = "Weekly summary and notes",
            body = "Totals for each week, with space for what you did about it."
        ) { WeeklyIllustration() },
        OnboardingPage(
            title = "Works without data",
            body = "Everything is saved on your phone. No internet needed to record."
        ) { OfflineIllustration() }
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("onboarding_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
        ) {
            // Skip sits where people expect it (top right); hidden on the last page
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLast) {
                    TextButton(onClick = onGetStarted, modifier = Modifier.testTag("onboarding_skip")) {
                        Text("Skip", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                val page = pages[index]
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val compact = maxHeight < 520.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (compact) 200.dp else 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            page.illustration()
                        }
                        Spacer(modifier = Modifier.height(if (compact) 20.dp else 36.dp))
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = page.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PageDots(count = pages.size, current = pagerState.currentPage)
                ClutchPrimaryButton(
                    text = if (isLast) "Get started" else "Next",
                    icon = if (isLast) ClutchIcons.Check else ClutchIcons.ChevronRight,
                    onClick = {
                        if (isLast) {
                            onGetStarted()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    testTag = if (isLast) "onboarding_get_started" else "onboarding_next"
                )
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "Page ${current + 1} of $count" }
    ) {
        repeat(count) { i ->
            val active = i == current
            val width by animateDpAsState(if (active) 24.dp else 8.dp, label = "dot_width")
            val color by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dot_color"
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

// --- Illustrations: small previews of the real app, so each page shows rather than tells ---

@Composable
private fun Halo(content: @Composable () -> Unit) {
    val ring = MaterialTheme.colorScheme.primaryContainer
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(Brush.radialGradient(listOf(ring, ring.copy(alpha = 0f))), CircleShape)
        )
        content()
    }
}

@Composable
private fun MockCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.width(250.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun WelcomeIllustration() {
    Halo { ClutchLogo(size = 190.dp) }
}

@Composable
private fun MockRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon = icon, size = 32.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DailyIllustration() {
    Halo {
        MockCard {
            Text("Today", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            MockRow(ClutchIcons.Egg, "Eggs", "14 crates + 5")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MockRow(ClutchIcons.Feed, "Feed", "57.5 kg")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MockRow(ClutchIcons.Deaths, "Died", "0")
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hen-day", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("92.4%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun ChartIllustration() {
    val bars = listOf(78f, 84f, 88f, 91f, 93f, 92f, 86f)
    val good = MaterialTheme.colorScheme.primary
    Halo {
        MockCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hen-day", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("92.4%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = good)
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val minV = 70f
                val maxV = 100f
                fun y(v: Float) = size.height * (1f - (v - minV) / (maxV - minV))
                drawRect(StatusGreenBgLight, topLeft = Offset(0f, y(95f)), size = Size(size.width, y(85f) - y(95f)))
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                drawLine(StatusGreenBorderLight, Offset(0f, y(85f)), Offset(size.width, y(85f)), 3f, pathEffect = dash)
                val slot = size.width / bars.size
                bars.forEachIndexed { i, v ->
                    val w = slot * 0.58f
                    val inBand = v >= 85f
                    drawRoundRect(
                        color = if (inBand) good else StatusAmberBorderLight,
                        topLeft = Offset(i * slot + (slot - w) / 2f, y(v)),
                        size = Size(w, size.height - y(v)),
                        cornerRadius = CornerRadius(8f)
                    )
                }
            }
            Text("Green band = target", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyIllustration() {
    Halo {
        MockCard {
            Text("Week 32 of age", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            MockRow(ClutchIcons.Egg, "Eggs", "3,150")
            MockRow(ClutchIcons.Feed, "Feed used", "402 kg")
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(ClutchIcons.Actions, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                    Text(
                        "Flushed the drinkers. Called the vet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineIllustration() {
    Halo {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(width = 130.dp, height = 210.dp),
                shape = RoundedCornerShape(28.dp),
                color = BrandDeepGreen,
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(ClutchIcons.Egg, contentDescription = null, tint = EggYolkAmber, modifier = Modifier.size(54.dp))
                }
            }
            Surface(
                modifier = Modifier
                    .padding(start = 120.dp, top = 150.dp)
                    .heightIn(min = 36.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(ClutchIcons.Good, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Text("Saved", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
