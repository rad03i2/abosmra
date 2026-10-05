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

    private fun render(snapshot: StatementSnapshot): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        canvas.drawColor(Color.WHITE)

        fill.color = greenDark
        canvas.drawRoundRect(54f, 54f, (WIDTH - 54).toFloat(), 320f, 42f, 42f, fill)
        text(canvas, "دفتر الغاز", 90, 90, WIDTH - 180, 54f, Color.WHITE, true)
        text(canvas, "كشف حساب الدين", 90, 162, WIDTH - 180, 32f, Color.WHITE)
        text(
            canvas,
            "التاريخ: " + formatDate(snapshot.generatedAt),
            90,
            230,
            WIDTH - 180,
            26f,
            Color.rgb(216, 236, 230)
        )

        var y = 366
        section(canvas, "بيانات الزبون", y)
        y += 58
        info(canvas, "الاسم", snapshot.customer.name, y)
        y += 52
        if (!snapshot.customer.phone.isNullOrBlank()) {
            info(canvas, "رقم الهاتف", snapshot.customer.phone.orEmpty(), y)
            y += 52
        }
        if (snapshot.customer.area.isNotBlank()) {
            info(canvas, "المنطقة", snapshot.customer.area, y)
            y += 52
        }

        y += 18
        fill.color = if (snapshot.currentBalance > 0) redSoft else greenSoft
        canvas.drawRoundRect(
            70f,
            y.toFloat(),
            (WIDTH - 70).toFloat(),
            (y + 202).toFloat(),
            32f,
            32f,
            fill
        )
        text(canvas, "الدين الحالي", 100, y + 28, WIDTH - 200, 30f, muted, false, Layout.Alignment.ALIGN_CENTER)
        text(
            canvas,
            formatMoney(snapshot.currentBalance),
            100,
            y + 78,
            WIDTH - 200,
            58f,
            if (snapshot.currentBalance > 0) red else green,
            true,
            Layout.Alignment.ALIGN_CENTER
        )
        text(
            canvas,
            if (snapshot.currentBalance > 0) "حساب مفتوح" else "الحساب مسدد بالكامل",
            100,
            y + 150,
            WIDTH - 200,
            25f,
            if (snapshot.currentBalance > 0) red else green,
            true,
            Layout.Alignment.ALIGN_CENTER
        )

        y += 235
        val gap = 24
        val cardWidth = (WIDTH - 140 - gap) / 2
        mini(canvas, 70, y, cardWidth, "إجمالي الديون", formatMoney(snapshot.totalDebts), redSoft, red)
        mini(canvas, 70 + cardWidth + gap, y, cardWidth, "إجمالي المدفوع", formatMoney(snapshot.totalPaid), greenSoft, green)

        y += 148
        section(canvas, "آخر العمليات", y)
        y += 56
        val recent = snapshot.entries.sortedByDescending { it.createdAt }.take(6)
        if (recent.isEmpty()) {
            text(canvas, "لا توجد عمليات مسجلة.", 80, y + 16, WIDTH - 160, 28f, muted)
            y += 72
        } else {
            recent.forEach {
                movement(canvas, it, y)
                y += 104
            }
        }

        val footerY = maxOf(y + 24, HEIGHT - 132)
        fill.color = Color.rgb(247, 248, 246)
        canvas.drawRoundRect(
            70f,
            footerY.toFloat(),
            (WIDTH - 70).toFloat(),
            (HEIGHT - 48).toFloat(),
            28f,
            28f,
            fill
        )
        text(
            canvas,
            "شكرًا لحسن تعاملكم 🌹",
            100,
            footerY + 28,
            WIDTH - 200,
            29f,
            greenDark,
            true,
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
