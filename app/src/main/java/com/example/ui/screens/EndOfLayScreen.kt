package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.EndOfLayCalc
import com.example.domain.LayerCalc
import com.example.domain.Money
import com.example.domain.TrendInsights
import com.example.ui.ClutchUiState
import com.example.ui.ClutchViewModel
import com.example.ui.EndOfLayPrices
import com.example.ui.components.BrandHeroCard
import com.example.ui.components.ChartDatum
import com.example.ui.components.ChartKind
import com.example.ui.components.ChartSpec
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InfoNote
import com.example.ui.components.LedgerTextField
import com.example.ui.components.PerformanceChartCard
import com.example.ui.components.ScreenColumn
import com.example.ui.components.ScreenTitle
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusChip
import com.example.ui.components.SummaryRow
import com.example.ui.theme.ClutchIcons
import com.example.ui.theme.OnBrandDeepGreen
import com.example.ui.theme.OnBrandDeepGreenMuted

@Composable
fun EndOfLayScreen(
    uiState: ClutchUiState,
    viewModel: ClutchViewModel,
    onRecordToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboard = uiState.activeDashboard
    val latest = dashboard?.latestRecord
    if (dashboard == null || latest == null) {
        ScreenColumn(modifier = modifier, testTag = "end_of_lay_screen") {
            EmptyStateCard(
                icon = ClutchIcons.Calculator,
                title = "Record a few weeks first",
                body = "The planner uses your own laying, feed and deaths to project the future.",
                primaryLabel = "Record today's count",
                primaryIcon = ClutchIcons.Record,
                onPrimary = onRecordToday
            )
        }
        return
    }
    val flock = dashboard.flock
    val saved = remember(flock.id) { viewModel.endOfLayPrices(flock.id) }

    fun fmt(v: Double?) = v?.let { if (it == kotlin.math.floor(it)) it.toLong().toString() else it.toString() } ?: ""
    var eggPrice by rememberSaveable(flock.id) { mutableStateOf(fmt(saved.eggPricePerCrate)) }
    var feedPrice by rememberSaveable(flock.id) { mutableStateOf(fmt(saved.feedPricePerBag)) }
    var henPrice by rememberSaveable(flock.id) { mutableStateOf(fmt(saved.spentHenPrice)) }
    var otherCost by rememberSaveable(flock.id) { mutableStateOf(fmt(saved.otherCostPerBirdPerMonth)) }

    val egg = eggPrice.toDoubleOrNull()?.takeIf { it > 0 }
    val feed = feedPrice.toDoubleOrNull()?.takeIf { it > 0 }
    val hen = henPrice.toDoubleOrNull()?.takeIf { it >= 0 }
    val other = otherCost.toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0
    val ready = egg != null && feed != null && hen != null

    // Use the last 7 recorded days for "now", so one odd day does not swing the answer
    val recent = dashboard.allRecordsAsc.takeLast(7)
    val henDayNow = recent.map { it.metrics.henDayPct }.average()
    val feedNow = recent.map { it.metrics.feedPerBirdG }.filter { it > 0 }.ifEmpty { listOf(115.0) }.average()
    val weeks = dashboard.weeklySummaries.sortedBy { it.ageWeek }
    val weeklyMortality = weeks.takeLast(4).map { it.mortalityPct }.ifEmpty { listOf(0.0) }.average()

    val plan = if (ready) {
        EndOfLayCalc.plan(
            EndOfLayCalc.FlockNow(
                ageWeeks = dashboard.ageWeeksToday,
                birds = latest.metrics.closingHens,
                henDayPct = henDayNow,
                feedPerBirdG = feedNow,
                weeklyMortalityPct = weeklyMortality,
                recentWeeklyHenDay = weeks.filter { it.ageWeek >= 35 }.map { it.henDayPct }
            ),
            EndOfLayCalc.Prices(egg!!, feed!!, hen!!, other)
        )
    } else null

    ScreenColumn(modifier = modifier, testTag = "end_of_lay_screen") {
        ScreenTitle(
            title = "End-of-lay planner",
            subtitle = "${flock.name} • ${dashboard.ageWeeksToday} weeks old • ${latest.metrics.closingHens} birds"
        )

        if (plan != null) {
            VerdictCard(plan = plan, henDayNow = henDayNow)
            CompareCard(plan = plan)
            ProjectionChart(plan = plan)
        }

        // Prices: the only thing the farmer must type (Tesler's Law — the app does the rest)
        SectionHeader(
            title = "Your prices",
            subtitle = if (ready) "Change them when market prices change" else "Enter three prices to see your plan"
        )
        ClutchCard(modifier = Modifier.testTag("eol_prices")) {
            PriceField(eggPrice, { eggPrice = it }, "Egg price", "per crate of 30", "input_eol_egg")
            PriceField(feedPrice, { feedPrice = it }, "Feed price", "per 50 kg bag", "input_eol_feed")
            PriceField(henPrice, { henPrice = it }, "Old layer price", "per bird when sold", "input_eol_hen")
            PriceField(otherCost, { otherCost = it }, "Other costs (optional)", "per bird per month: labour, drugs, light", "input_eol_other")
            ClutchPrimaryButton(
                text = "Save prices",
                icon = ClutchIcons.Save,
                enabled = ready,
                onClick = {
                    viewModel.saveEndOfLayPrices(
                        flock.id,
                        EndOfLayPrices(egg, feed, hen, otherCost.toDoubleOrNull())
                    )
                },
                testTag = "save_eol_prices"
            )
        }

        if (plan != null) {
            InfoNote(
                title = "How this is worked out",
                text = "Uses your last 7 days (laying ${LayerCalc.format1(henDayNow)}%, feed ${LayerCalc.format0(feedNow)} g per bird) " +
                    "and assumes laying falls ${LayerCalc.format1(plan.declinePerWeek)} points a week" +
                    (if (plan.declineFromFlock) " (your flock's own trend)" else " (typical for brown layers)") +
                    ". It is an estimate — prices and flock health can change it."
            )
        }
    }
}

@Composable
private fun PriceField(value: String, onChange: (String) -> Unit, label: String, helper: String, tag: String) {
    LedgerTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }.take(9)) },
        label = label,
        suffix = "GH₵",
        helperText = helper,
        errorText = null,
        keyboardType = KeyboardType.Decimal,
        testTag = tag
    )
}

@Composable
private fun VerdictCard(plan: EndOfLayCalc.Plan, henDayNow: Double) {
    val (state, word, headline) = when (plan.advice) {
        EndOfLayCalc.Advice.KEEP -> Triple(
            LayerCalc.TargetState.ON_TARGET,
            "Keep laying",
            plan.weeksUntilBreakEven?.let { "Your birds should keep paying for about $it more weeks." }
                ?: "Your birds should keep paying for a long time yet."
        )
        EndOfLayCalc.Advice.PLAN_TO_SELL -> Triple(
            LayerCalc.TargetState.WATCH,
            "Plan to sell",
            "In about ${plan.weeksUntilBreakEven} weeks the eggs will no longer pay for the feed. Start looking for buyers."
        )
        EndOfLayCalc.Advice.SELL_NOW -> Triple(
            LayerCalc.TargetState.OFF_TARGET,
            "Sell now",
            "The eggs no longer pay for the feed. Keeping the birds loses ${Money.format(-plan.weeklyMarginNow)} a week."
        )
    }

    BrandHeroCard(modifier = Modifier.testTag("eol_verdict")) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(word, style = MaterialTheme.typography.headlineMedium, color = OnBrandDeepGreen, modifier = Modifier.weight(1f))
            StatusChip(state = state)
        }
        Text(headline, style = MaterialTheme.typography.bodyLarge, color = OnBrandDeepGreen)
        HorizontalDivider(color = OnBrandDeepGreen.copy(alpha = 0.2f))
        Row(modifier = Modifier.fillMaxWidth()) {
            HeroNumber("Laying now", "${LayerCalc.format1(henDayNow)}%", Modifier.weight(1f))
            HeroNumber("Pays above", "${LayerCalc.format1(plan.breakEvenHenDayPct)}%", Modifier.weight(1f))
            HeroNumber(
                if (plan.weeklyMarginNow >= 0) "Profit a week" else "Loss a week",
                Money.format(kotlin.math.abs(plan.weeklyMarginNow)),
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroNumber(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = OnBrandDeepGreenMuted)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = OnBrandDeepGreen)
    }
}

@Composable
private fun CompareCard(plan: EndOfLayCalc.Plan) {
    val keepLabel = when {
        plan.weeksUntilBreakEven == 0 -> null
        plan.sellAtAgeWeeks != null -> "Keep until week ${plan.sellAtAgeWeeks}, then sell"
        else -> "Keep for a year, then sell"
    }
    ClutchCard(modifier = Modifier.testTag("eol_compare")) {
        SectionHeader(title = "Sell now or keep?", subtitle = "Money you end up with from this flock")
        SummaryRow(label = "Sell all birds now", caption = "Old layers at your price", value = Money.format(plan.sellNowValue))
        if (keepLabel != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow(label = keepLabel, caption = "Egg profit until then + sale", value = Money.format(plan.keepThenSellValue))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val extra = plan.extraFromKeeping
            Text(
                text = if (extra >= 0) "Keeping earns about ${Money.format(extra)} more." else "Selling now is better by about ${Money.format(-extra)}.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ProjectionChart(plan: EndOfLayCalc.Plan) {
    val horizon = (plan.weeksUntilBreakEven?.plus(4) ?: 26).coerceIn(8, EndOfLayCalc.MAX_WEEKS_AHEAD)
    val points = plan.projection.take(horizon + 1).map {
        ChartDatum(
            shortLabel = "Wk ${it.ageWeeks}",
            fullLabel = if (it.weeksFromNow == 0) "This week (week ${it.ageWeeks})" else "In ${it.weeksFromNow} weeks (week ${it.ageWeeks})",
            value = it.margin,
            detail = "Laying ${LayerCalc.format1(it.henDayPct)}% • eggs ${Money.format(it.income)} • costs ${Money.format(it.costs)}"
        )
    }
    val maxAbs = points.maxOf { kotlin.math.abs(it.value) }.coerceAtLeast(1.0)
    PerformanceChartCard(
        spec = ChartSpec(
            id = "eol_projection",
            title = "Projected weekly profit",
            meaning = "Egg money minus feed and other costs, week by week",
            unit = "",
            decimals = 0,
            data = points,
            bandMin = 0.0,
            bandMax = null,
            targetText = "Above zero = still paying",
            goodness = TrendInsights.Goodness.HIGHER_IS_BETTER,
            yFloor = -maxAbs * 0.2,
            yCeil = maxAbs,
            kinds = listOf(ChartKind.BARS, ChartKind.LINE),
            pointNoun = "week",
            prefix = "GH₵ "
        )
    )
}
