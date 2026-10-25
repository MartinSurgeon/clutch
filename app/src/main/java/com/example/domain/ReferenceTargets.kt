package com.example.domain

/**
 * Reference targets shown in Guide & Targets. These numbers mirror the thresholds the
 * LayerCalc evaluators use, so what the farmer reads is what the app rates against.
 * They are indicative for commercial brown layers — always compare with the breeder's
 * management guide for the strain and age.
 */
object ReferenceTargets {

    enum class Group { PRODUCTION, FEED, EGG_WEIGHT, QUALITY, UNIFORMITY, MORTALITY }

    data class TargetRow(
        val label: String,
        val target: String,
        val note: String? = null
    )

    data class TargetGroup(
        val group: Group,
        val title: String,
        val subtitle: String,
        val rows: List<TargetRow>
    )

    const val DISCLAIMER_TARGETS =
        "These targets are a guide only. Compare them with the management guide for your layer breed and the age of your birds."

    const val DISCLAIMER_MORTALITY =
        "Deaths on one day do not tell the full story. Always check deaths for the week and total deaths since stocking."

    val groups: List<TargetGroup> = listOf(
        TargetGroup(
            group = Group.PRODUCTION,
            title = "Production",
            subtitle = "Laying %, also called hen-day %",
            rows = listOf(
                TargetRow("18–24 weeks", "Rising fast", "Birds come into lay. Laying % climbs every week."),
                TargetRow("25–35 weeks (peak)", "85–95%", "Under 80% at peak needs attention."),
                TargetRow("After 35 weeks", "Drops slowly", "About 0.5% less each week is normal."),
                TargetRow("Hen-housed %", "Close to laying %", "A big gap means many birds have been lost.")
            )
        ),
        TargetGroup(
            group = Group.FEED,
            title = "Feed and water",
            subtitle = "Per bird, per day",
            rows = listOf(
                TargetRow("Feed per bird", "110–125 g", "100–110 g or 125–130 g, watch it."),
                TargetRow("Feed for 1 kg of eggs (FCR)", "2.0–2.2 kg", "Over 2.4 needs attention. Not rated before 24 weeks."),
                TargetRow("Water per bird", "180–250 mL", "More in the hot season. A sudden drop is an early warning.")
            )
        ),
        TargetGroup(
            group = Group.EGG_WEIGHT,
            title = "Egg weight",
            subtitle = "Weigh 30 eggs once a week",
            rows = listOf(
                TargetRow("Average egg weight", "55–65 g", "From 30 weeks. Young birds lay smaller eggs first."),
                TargetRow("Egg mass per bird", "55 g or more", "From 25 weeks. Laying % × egg weight.")
            )
        ),
        TargetGroup(
            group = Group.QUALITY,
            title = "Egg quality",
            subtitle = "Out of every 100 eggs picked",
            rows = listOf(
                TargetRow("Cracked", "2% or less"),
                TargetRow("Dirty", "3% or less"),
                TargetRow("Soft-shell", "1% or less"),
                TargetRow("All bad eggs together", "4% or less", "4–6%, watch it. Over 6% needs attention.")
            )
        ),
        TargetGroup(
            group = Group.UNIFORMITY,
            title = "Uniformity and body weight",
            subtitle = "Weigh 50–100 birds every 1–2 weeks",
            rows = listOf(
                TargetRow("Uniformity", "80% or more", "Birds within 10% of the average weight. 70–80%, watch it."),
                TargetRow("Body weight", "Follow your breed guide", "Brown layers in lay are often about 1.8–2.1 kg.")
            )
        ),
        TargetGroup(
            group = Group.MORTALITY,
            title = "Deaths",
            subtitle = "Check every week, not just every day",
            rows = listOf(
                TargetRow("Deaths in a week", "Under 0.1%", "0.1–0.5%, watch it. Over 0.5% needs attention now."),
                TargetRow("Total up to peak", "Under 3%"),
                TargetRow("Total for the laying period", "Under 5–7%")
            )
        )
    )

    /** Daily monitoring habits, kept to 7 so they are easy to remember. */
    val dailyMonitoringNotes: List<String> = listOf(
        "Check water first thing every morning. Birds drink less before they lay less.",
        "Pick eggs at least 3 times a day to cut cracked and dirty eggs.",
        "Record at the same time each day, after the last egg pick.",
        "Look at every dead bird and write down what you see.",
        "Keep light at 14–16 hours a day. Never cut light hours during lay.",
        "Weigh 30 eggs every week, and 50–100 birds every 1–2 weeks.",
        "Write notes and actions each week so you can see what worked."
    )
}
