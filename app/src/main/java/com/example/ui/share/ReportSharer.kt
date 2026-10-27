package com.example.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.LayerCalc
import com.example.domain.ReportBuilder
import com.example.ui.currentIsoDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Turns a [ReportBuilder.Report] into a branded image + text and hands it to WhatsApp
 * (or any app if WhatsApp is not installed).
 */
object ReportSharer {

    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun whatsappPackage(context: Context): String? = WHATSAPP_PACKAGES.firstOrNull { pkg ->
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(pkg, 0)
        }.isSuccess
    }

    fun hasWhatsApp(context: Context) = whatsappPackage(context) != null

    /**
     * Shares the report. With [toWhatsApp] it opens WhatsApp straight away (one tap);
     * otherwise, or if WhatsApp is missing, the normal share sheet.
     */
    suspend fun share(context: Context, report: ReportBuilder.Report, toWhatsApp: Boolean) {
        val text = ReportBuilder.toText(report)
        val file = withContext(Dispatchers.Default) { renderToCache(context, report) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pkg = if (toWhatsApp) whatsappPackage(context) else null
        try {
            if (pkg != null) {
                context.startActivity(send.setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                context.startActivity(
                    Intent.createChooser(send, "Send report with…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent.createChooser(send.setPackage(null), "Send report with…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    // --- Image ---

    private const val W = 1080
    private const val PAD = 64f
    private const val GAP = 24f

    private val INK = 0xFF14201A.toInt()
    private val INK_SOFT = 0xFF46544C.toInt()
    private val PAPER = 0xFFF5F7F2.toInt()
    private val CARD = 0xFFFFFFFF.toInt()
    private val LINE = 0xFFDDE3DA.toInt()
    private val GREEN_DARK = 0xFF0B4D33.toInt()
    private val GREEN = 0xFF136F4A.toInt()
    private val MINT = 0xFFCDE8DA.toInt()

    private fun toneColor(tone: ReportBuilder.Tone): Int = when (tone) {
        ReportBuilder.Tone.GOOD -> 0xFF2E8B57.toInt()
        ReportBuilder.Tone.WATCH -> 0xFFD99000.toInt()
        ReportBuilder.Tone.BAD -> 0xFFDC2626.toInt()
        ReportBuilder.Tone.NEUTRAL -> 0x00000000
    }

    private fun font(context: Context, id: Int): Typeface =
        runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: Typeface.DEFAULT

    private fun paint(typeface: Typeface, sizePx: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = sizePx
        this.color = color
    }

    private fun layout(text: String, p: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()

    fun renderToCache(context: Context, report: ReportBuilder.Report): File {
        val regular = font(context, R.font.sora_regular)
        val semibold = font(context, R.font.sora_semibold)
        val bold = font(context, R.font.sora_bold)

        val headerH = 380f
        val cardW = (W - PAD * 2 - GAP) / 2f
        val cardH = 236f
        val rows = (report.stats.size + 1) / 2
        val contentW = (W - PAD * 2).toInt()

        val alertsText = report.alerts.joinToString("\n") { "• $it" }
        val alertsLayout = if (report.alerts.isNotEmpty()) layout(alertsText, paint(regular, 34f, 0xFF6E4700.toInt()), contentW - 64) else null
        val notesLayout = report.notes?.let { layout(it, paint(regular, 34f, INK), contentW - 64) }

        var height = headerH + PAD + rows * (cardH + GAP)
        alertsLayout?.let { height += 90f + it.height + 48f + GAP }
        notesLayout?.let { height += 90f + it.height + 48f + GAP }
        height += 120f

        val bmp = Bitmap.createBitmap(W, height.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(PAPER)

        // Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, W.toFloat(), headerH, GREEN_DARK, GREEN, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), headerH, headerPaint)
        BitmapFactory.decodeResource(context.resources, R.drawable.clutch_logo)?.let { logo ->
            val size = 132f
            val dst = RectF(PAD, PAD, PAD + size, PAD + size)
            c.drawBitmap(logo, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        c.drawText("FARM REPORT", PAD + 160f, PAD + 58f, paint(semibold, 30f, MINT).apply { letterSpacing = 0.12f })
        c.drawText(LayerCalc.friendlyDate(currentIsoDate(), withYear = true), PAD + 160f, PAD + 108f, paint(regular, 32f, MINT))
        c.drawText(report.title, PAD, 292f, paint(bold, 72f, 0xFFFFFFFF.toInt()))
        c.drawText(report.subtitle, PAD, 346f, paint(regular, 32f, MINT))

        // Stat cards, two per row
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LINE; style = Paint.Style.STROKE; strokeWidth = 2f }
        val labelPaint = paint(semibold, 30f, INK_SOFT)
        val valuePaint = paint(bold, 64f, INK)
        val notePaint = paint(regular, 28f, INK_SOFT)
        var y = headerH + PAD
        report.stats.forEachIndexed { i, s ->
            val col = i % 2
            if (i > 0 && col == 0) y += cardH + GAP
            val x = PAD + col * (cardW + GAP)
            val rect = RectF(x, y, x + cardW, y + cardH)
            c.drawRoundRect(rect, 36f, 36f, cardPaint)
            c.drawRoundRect(rect, 36f, 36f, borderPaint)
            c.drawText("${s.emoji}  ${s.label}", x + 32f, y + 62f, labelPaint)
            c.drawText(s.value, x + 32f, y + 142f, valuePaint)
            val tone = toneColor(s.tone)
            var noteX = x + 32f
            if (s.tone != ReportBuilder.Tone.NEUTRAL) {
                c.drawCircle(noteX + 9f, y + 189f, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tone })
                noteX += 30f
            }
            s.note?.let {
                val shown = TextUtilsEllipsize.fit(it, notePaint, cardW - (noteX - x) - 24f)
                c.drawText(shown, noteX, y + 199f, notePaint)
            }
        }
        if (report.stats.isNotEmpty()) y += cardH + GAP

        // Alerts
        alertsLayout?.let { l ->
            val boxH = 90f + l.height + 48f
            val rect = RectF(PAD, y, W - PAD, y + boxH)
            c.drawRoundRect(rect, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF1CC.toInt() })
            c.drawText("⚠️  Things to check", PAD + 32f, y + 66f, paint(bold, 36f, 0xFF6E4700.toInt()))
            c.save()
            c.translate(PAD + 32f, y + 90f)
            l.draw(c)
            c.restore()
            y += boxH + GAP
        }

        // Notes
        notesLayout?.let { l ->
            val boxH = 90f + l.height + 48f
            val rect = RectF(PAD, y, W - PAD, y + boxH)
            c.drawRoundRect(rect, 36f, 36f, cardPaint)
            c.drawRoundRect(rect, 36f, 36f, borderPaint)
            c.drawText("📝  Notes", PAD + 32f, y + 66f, paint(bold, 36f, INK))
            c.save()
            c.translate(PAD + 32f, y + 90f)
            l.draw(c)
            c.restore()
            y += boxH + GAP
        }

        c.drawText("Sent from Clutch • layer farm records", PAD, height - 52f, paint(regular, 28f, INK_SOFT))

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "clutch_report_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }

    /** Keeps notes on one line inside a card, adding "…" when they do not fit. */
    private object TextUtilsEllipsize {
        fun fit(text: String, p: TextPaint, maxWidth: Float): String =
            android.text.TextUtils.ellipsize(text, p, maxWidth, android.text.TextUtils.TruncateAt.END).toString()
    }
}
