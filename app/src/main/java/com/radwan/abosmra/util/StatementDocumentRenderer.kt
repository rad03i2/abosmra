package com.radwan.abosmra.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.radwan.abosmra.data.Customer
import com.radwan.abosmra.data.EntryType
import com.radwan.abosmra.data.LedgerEntry
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class StatementSnapshot(
    val customer: Customer,
    val entries: List<LedgerEntry>,
    val currentBalance: Long,
    val totalDebts: Long,
    val totalPaid: Long,
    val generatedAt: Long = System.currentTimeMillis()
)

object StatementDocumentRenderer {
    const val WIDTH = 1240
    const val HEIGHT = 1754

    private val green = Color.rgb(11, 98, 82)
    private val greenDark = Color.rgb(7, 62, 53)
    private val greenSoft = Color.rgb(221, 243, 236)
    private val red = Color.rgb(179, 58, 50)
    private val redSoft = Color.rgb(255, 231, 228)
    private val ink = Color.rgb(21, 32, 29)
    private val muted = Color.rgb(104, 116, 111)
    private val line = Color.rgb(229, 234, 232)

    fun createPng(context: Context, snapshot: StatementSnapshot): File {
        val bitmap = render(snapshot)
        val file = File(shareDir(context), fileName(snapshot, "png"))
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    fun createPdf(context: Context, snapshot: StatementSnapshot): File {
        val file = File(shareDir(context), fileName(snapshot, "pdf"))
        FileOutputStream(file).use { writePdf(snapshot, it) }
        return file
    }

    fun writePdf(snapshot: StatementSnapshot, output: OutputStream) {
        val bitmap = render(snapshot)
        val document = PdfDocument()
        try {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, 1).create()
            )
            page.canvas.drawBitmap(
                bitmap,
                null,
                Rect(0, 0, WIDTH, HEIGHT),
                Paint(Paint.ANTI_ALIAS_FLAG)
            )
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            bitmap.recycle()
            document.close()
        }
    }

    fun message(snapshot: StatementSnapshot): String = buildString {
        appendLine("السلام عليكم")
        appendLine("كشف حساب الدين")
        appendLine("الزبون: " + snapshot.customer.name)
        appendLine("الدين الحالي: " + formatMoney(snapshot.currentBalance))
        appendLine("شكرًا لحسن تعاملكم 🌹")
    }

    fun suggestedPdfName(snapshot: StatementSnapshot): String =
        "DaftarAlGas-" + safeName(snapshot.customer.name) + "-" +
            formatDate(snapshot.generatedAt).replace("/", "-") + ".pdf"

    fun currentCycleEntries(snapshot: StatementSnapshot): List<LedgerEntry> {
        if (snapshot.currentBalance <= 0L) return emptyList()

        val ordered = snapshot.entries.sortedBy { it.createdAt }
        var running = snapshot.customer.openingDebt.coerceAtLeast(0L)
        var cycleStart = 0

        ordered.forEachIndexed { index, entry ->
            running = when (entry.type) {
                EntryType.DEBT -> running + entry.amount
                EntryType.PAYMENT -> (running - entry.amount).coerceAtLeast(0L)
            }
            if (running == 0L) cycleStart = index + 1
        }

        return ordered.drop(cycleStart).sortedByDescending { it.createdAt }
    }

    private fun render(snapshot: StatementSnapshot): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        canvas.drawColor(Color.rgb(250, 250, 247))

        fill.color = greenDark
        canvas.drawRoundRect(46f, 46f, (WIDTH - 46).toFloat(), 330f, 46f, 46f, fill)

        fill.color = Color.argb(28, 255, 255, 255)
        canvas.drawCircle((WIDTH - 128).toFloat(), 112f, 84f, fill)
        fill.color = Color.argb(22, 255, 255, 255)
        canvas.drawCircle(126f, 286f, 116f, fill)

        text(canvas, "دفتر الغاز", 84, 78, WIDTH - 168, 56f, Color.WHITE, true)
        text(canvas, "كشف حساب الدين", 84, 154, WIDTH - 168, 31f, Color.WHITE)
        text(
            canvas,
            "تاريخ الإصدار • " + formatDate(snapshot.generatedAt),
            84,
            214,
            WIDTH - 168,
            25f,
            Color.rgb(211, 235, 228)
        )

        val customerCardTop = 365
        fill.color = Color.WHITE
        canvas.drawRoundRect(
            64f,
            customerCardTop.toFloat(),
            (WIDTH - 64).toFloat(),
            (customerCardTop + 190).toFloat(),
            34f,
            34f,
            fill
        )

        text(canvas, "الزبون", 92, customerCardTop + 28, 230, 23f, muted)
        text(
            canvas,
            snapshot.customer.name,
            310,
            customerCardTop + 22,
            WIDTH - 402,
            34f,
            ink,
            true
        )

        val secondary = buildList {
            snapshot.customer.phone?.takeIf { it.isNotBlank() }?.let { add(it) }
            snapshot.customer.area.takeIf { it.isNotBlank() }?.let { add(it) }
        }.joinToString("  •  ")

        if (secondary.isNotBlank()) {
            text(
                canvas,
                secondary,
                92,
                customerCardTop + 91,
                WIDTH - 184,
                25f,
                muted
            )
        }

        fill.color = line
        canvas.drawRoundRect(
            92f,
            (customerCardTop + 148).toFloat(),
            (WIDTH - 92).toFloat(),
            (customerCardTop + 151).toFloat(),
            2f,
            2f,
            fill
        )

        val balanceTop = 590
        fill.color = if (snapshot.currentBalance > 0) redSoft else greenSoft
        canvas.drawRoundRect(
            64f,
            balanceTop.toFloat(),
            (WIDTH - 64).toFloat(),
            (balanceTop + 286).toFloat(),
            42f,
            42f,
            fill
        )

        text(
            canvas,
            "الدين الحالي",
            92,
            balanceTop + 36,
            WIDTH - 184,
            30f,
            muted,
            false,
            Layout.Alignment.ALIGN_CENTER
        )
        text(
            canvas,
            formatMoney(snapshot.currentBalance),
            92,
            balanceTop + 94,
            WIDTH - 184,
            72f,
            if (snapshot.currentBalance > 0) red else green,
            true,
            Layout.Alignment.ALIGN_CENTER
        )
        text(
            canvas,
            if (snapshot.currentBalance > 0) "حساب مفتوح" else "الحساب مسدد بالكامل",
            92,
            balanceTop + 205,
            WIDTH - 184,
            27f,
            if (snapshot.currentBalance > 0) red else green,
            true,
            Layout.Alignment.ALIGN_CENTER
        )

        val summaryTop = 914
        val gap = 22
        val cardWidth = (WIDTH - 128 - gap) / 2
        mini(
            canvas,
            64,
            summaryTop,
            cardWidth,
            "إجمالي الديون",
            formatMoney(snapshot.totalDebts),
            Color.WHITE,
            red
        )
        mini(
            canvas,
            64 + cardWidth + gap,
            summaryTop,
            cardWidth,
            "إجمالي المدفوع",
            formatMoney(snapshot.totalPaid),
            Color.WHITE,
            green
        )

        var y = 1078
        text(
            canvas,
            if (snapshot.currentBalance > 0) {
                "الحركات التي تكوّن الرصيد الحالي"
            } else {
                "حالة الحساب"
            },
            64,
            y,
            WIDTH - 128,
            31f,
            ink,
            true
        )
        y += 58

        val currentCycle = currentCycleEntries(snapshot).take(6)
        if (snapshot.currentBalance <= 0L) {
            fill.color = greenSoft
            canvas.drawRoundRect(
                64f,
                y.toFloat(),
                (WIDTH - 64).toFloat(),
                (y + 122).toFloat(),
                28f,
                28f,
                fill
            )
            text(
                canvas,
                "لا يوجد دين حالي. الحساب مسدد بالكامل.",
                92,
                y + 37,
                WIDTH - 184,
                28f,
                greenDark,
                true,
                Layout.Alignment.ALIGN_CENTER
            )
            y += 150
        } else if (currentCycle.isEmpty()) {
            fill.color = Color.WHITE
            canvas.drawRoundRect(
                64f,
                y.toFloat(),
                (WIDTH - 64).toFloat(),
                (y + 110).toFloat(),
                28f,
                28f,
                fill
            )
            text(
                canvas,
                "الرصيد الحالي ناتج عن الدين السابق المسجل للزبون.",
                90,
                y + 32,
                WIDTH - 180,
                27f,
                muted,
                true,
                Layout.Alignment.ALIGN_CENTER
            )
            y += 138
        } else {
            currentCycle.forEach { entry ->
                movement(canvas, entry, y)
                y += 98
            }
        }

        val footerY = maxOf(y + 24, HEIGHT - 142)
        fill.color = Color.WHITE
        canvas.drawRoundRect(
            64f,
            footerY.toFloat(),
            (WIDTH - 64).toFloat(),
            (HEIGHT - 42).toFloat(),
            30f,
            30f,
            fill
        )
        text(
            canvas,
            "شكرًا لحسن تعاملكم 🌹",
            92,
            footerY + 29,
            WIDTH - 184,
            29f,
            greenDark,
            true,
            Layout.Alignment.ALIGN_CENTER
        )
        text(
            canvas,
            "تم إنشاء هذا الكشف من تطبيق دفتر الغاز",
            92,
            footerY + 72,
            WIDTH - 184,
            21f,
            muted,
            false,
            Layout.Alignment.ALIGN_CENTER
        )

        return bitmap
    }

    private fun section(canvas: Canvas, value: String, y: Int) {
        text(canvas, value, 70, y, WIDTH - 140, 32f, ink, true)
    }

    private fun info(canvas: Canvas, label: String, value: String, y: Int) {
        text(canvas, label, 70, y, 300, 25f, muted)
        text(canvas, value, 380, y, WIDTH - 450, 28f, ink, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = line
            strokeWidth = 2f
        }
        canvas.drawLine(70f, (y + 44).toFloat(), (WIDTH - 70).toFloat(), (y + 44).toFloat(), paint)
    }

    private fun mini(
        canvas: Canvas,
        x: Int,
        y: Int,
        width: Int,
        label: String,
        value: String,
        background: Int,
        foreground: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = background
        }
        canvas.drawRoundRect(
            x.toFloat(),
            y.toFloat(),
            (x + width).toFloat(),
            (y + 124).toFloat(),
            26f,
            26f,
            paint
        )
        text(canvas, label, x + 20, y + 18, width - 40, 24f, muted)
        text(canvas, value, x + 20, y + 58, width - 40, 31f, foreground, true)
    }

    private fun movement(canvas: Canvas, entry: LedgerEntry, y: Int) {
        val debt = entry.type == EntryType.DEBT
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (debt) redSoft else greenSoft
        }
        canvas.drawRoundRect(70f, y.toFloat(), (WIDTH - 70).toFloat(), (y + 86).toFloat(), 22f, 22f, paint)
        val details = entry.bottles?.let { " • " + it.toString() + " قنينة" }.orEmpty()
        text(
            canvas,
            (if (debt) "دين" else "تحصيل") + " • " + formatDate(entry.createdAt) + details,
            92,
            y + 15,
            650,
            24f,
            ink,
            true
        )
        text(
            canvas,
            (if (debt) "+" else "-") + formatMoney(entry.amount),
            760,
            y + 15,
            WIDTH - 850,
            28f,
            if (debt) red else green,
            true
        )
    }

    private fun text(
        canvas: Canvas,
        value: String,
        x: Int,
        y: Int,
        width: Int,
        size: Float,
        color: Int,
        bold: Boolean = false,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_OPPOSITE
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }
        val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width.coerceAtLeast(1))
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .setMaxLines(2)
            .build()
        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    private fun shareDir(context: Context): File =
        File(context.cacheDir, "shared").apply { mkdirs() }

    private fun fileName(snapshot: StatementSnapshot, extension: String): String =
        "statement-" + safeName(snapshot.customer.name) + "-" + snapshot.generatedAt + "." + extension

    private fun safeName(value: String): String =
        value.trim()
            .replace(Regex("[^\\p{L}\\p{N}._-]+"), "-")
            .trim('-')
            .ifBlank { "customer" }
}
