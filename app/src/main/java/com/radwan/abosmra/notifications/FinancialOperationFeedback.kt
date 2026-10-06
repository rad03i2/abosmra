package com.radwan.abosmra.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.radwan.abosmra.MainActivity
import com.radwan.abosmra.R
import com.radwan.abosmra.security.AppSecurityStore
import com.radwan.abosmra.util.formatMoney
import kotlin.math.PI
import kotlin.math.sin

enum class FinancialOperationKind {
    DEBT,
    PAYMENT,
    FULL_SETTLEMENT
}

data class FinancialOperationReceipt(
    val kind: FinancialOperationKind,
    val customerId: String,
    val customerName: String,
    val amount: Long,
    val balanceAfter: Long
)

object FinancialOperationFeedback {
    private const val CHANNEL_ID = "financial_operation_confirmations"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "تأكيد العمليات المالية",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "إشعار هادئ بعد تسجيل الدين أو التحصيل"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun postNotification(context: Context, receipt: FinancialOperationReceipt) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)

        val privacy = AppSecurityStore(context).state()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_CUSTOMER_ID, receipt.customerId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            receipt.customerId.hashCode() xor receipt.kind.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (receipt.kind) {
            FinancialOperationKind.DEBT -> "تم تسجيل الدين"
            FinancialOperationKind.PAYMENT -> "تم تسجيل التحصيل"
            FinancialOperationKind.FULL_SETTLEMENT -> "تم تسديد الحساب بالكامل"
        }

        val body = if (privacy.hideAmounts) {
            when (receipt.kind) {
                FinancialOperationKind.DEBT ->
                    "تمت إضافة حركة دين إلى حساب " + receipt.customerName + "."
                FinancialOperationKind.PAYMENT ->
                    "تم تسجيل تحصيل من " + receipt.customerName + "."
                FinancialOperationKind.FULL_SETTLEMENT ->
                    "تم تسديد حساب " + receipt.customerName + " بالكامل."
            }
        } else {
            when (receipt.kind) {
                FinancialOperationKind.DEBT ->
                    receipt.customerName + " • " + formatMoney(receipt.amount) +
                        " • الرصيد " + formatMoney(receipt.balanceAfter)
                FinancialOperationKind.PAYMENT ->
                    receipt.customerName + " • " + formatMoney(receipt.amount) +
                        " • المتبقي " + formatMoney(receipt.balanceAfter)
                FinancialOperationKind.FULL_SETTLEMENT ->
                    receipt.customerName + " • " + formatMoney(receipt.amount) +
                        " • الرصيد الآن 0 د.ع"
            }
        }

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("دفتر الغاز")
            .setContentText("تم حفظ عملية مالية بنجاح.")
            .setSilent(true)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setSilent(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                operationNotificationId(receipt),
                notification
            )
        } catch (_: SecurityException) {
            // Android can revoke notification permission between the check and post.
        }
    }

    fun playComfortableMoneySound(kind: FinancialOperationKind) {
        val notes = when (kind) {
            FinancialOperationKind.DEBT -> listOf(
                Note(523.25, 135),
                Note(659.25, 175)
            )
            FinancialOperationKind.PAYMENT -> listOf(
                Note(659.25, 120),
                Note(783.99, 135),
                Note(987.77, 190)
            )
            FinancialOperationKind.FULL_SETTLEMENT -> listOf(
                Note(659.25, 115),
                Note(783.99, 125),
                Note(987.77, 145),
                Note(1174.66, 220)
            )
        }

        runCatching {
            val sampleRate = 44_100
            val gapMs = 28
            val totalMs = notes.sumOf { it.durationMs } + gapMs * (notes.size - 1)
            val totalSamples = (sampleRate * totalMs / 1000.0).toInt()
            val pcm = ShortArray(totalSamples)

            var cursor = 0
            notes.forEachIndexed { index, note ->
                val noteSamples = (sampleRate * note.durationMs / 1000.0).toInt()
                for (i in 0 until noteSamples) {
                    val progress = i.toDouble() / noteSamples.coerceAtLeast(1)
                    val t = i.toDouble() / sampleRate
                    val attack = (progress / 0.12).coerceIn(0.0, 1.0)
                    val release = ((1.0 - progress) / 0.72).coerceIn(0.0, 1.0)
                    val envelope = attack * release
                    val fundamental = sin(2.0 * PI * note.frequency * t)
                    val shimmer = 0.17 * sin(2.0 * PI * note.frequency * 2.0 * t)
                    val sample = ((fundamental + shimmer) * envelope * 0.115 * Short.MAX_VALUE)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    if (cursor + i < pcm.size) pcm[cursor + i] = sample.toShort()
                }
                cursor += noteSamples
                if (index < notes.lastIndex) {
                    cursor += (sampleRate * gapMs / 1000.0).toInt()
                }
            }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            try {
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(totalMs.toLong() + 80L)
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }
    }

    private fun operationNotificationId(receipt: FinancialOperationReceipt): Int =
        30_000 + ((receipt.customerId.hashCode() * 31 + receipt.kind.ordinal) and 0x3FFF)

    private data class Note(
        val frequency: Double,
        val durationMs: Int
    )
}
