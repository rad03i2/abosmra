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

enum class OperationSoundPreset(
    val storageValue: String,
    val title: String,
    val description: String
) {
    CASH_REGISTER(
        "cash_register",
        "Cash Register",
        "رنين نقدي واضح يشبه إغلاق عملية في صندوق المحاسبة."
    ),
    COIN_CASCADE(
        "coin_cascade",
        "Coin Cascade",
        "لمعة عملات معدنية سريعة وواضحة بطابع مالي."
    ),
    POS_PREMIUM(
        "pos_premium",
        "POS Premium",
        "نغمة دفع إلكتروني نظيفة وفاخرة ومريحة."
    );

    companion object {
        fun fromStorage(value: String?): OperationSoundPreset =
            entries.firstOrNull { it.storageValue == value } ?: CASH_REGISTER
    }
}

enum class NotificationSoundPreset(
    val storageValue: String,
    val title: String,
    val description: String
) {
    CASH_PING(
        "cash_ping",
        "Cash Ping",
        "تنبيه نقدي قصير ولامع بعد حفظ العملية."
    ),
    SOFT_BELL(
        "soft_bell",
        "Soft Bell",
        "جرس ناعم وواضح دون حدة مزعجة."
    ),
    DOUBLE_CHIME(
        "double_chime",
        "Double Chime",
        "نغمتان واضحتان بطابع تطبيقات الدفع."
    );

    companion object {
        fun fromStorage(value: String?): NotificationSoundPreset =
            entries.firstOrNull { it.storageValue == value } ?: CASH_PING
    }
}

data class FinancialFeedbackSettings(
    val operationSound: OperationSoundPreset,
    val notificationSound: NotificationSoundPreset
)

class FinancialFeedbackStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun state(): FinancialFeedbackSettings =
        FinancialFeedbackSettings(
            operationSound = OperationSoundPreset.fromStorage(
                prefs.getString(KEY_OPERATION_SOUND, null)
            ),
            notificationSound = NotificationSoundPreset.fromStorage(
                prefs.getString(KEY_NOTIFICATION_SOUND, null)
            )
        )

    fun setOperationSound(preset: OperationSoundPreset) {
        prefs.edit().putString(KEY_OPERATION_SOUND, preset.storageValue).apply()
    }

    fun setNotificationSound(preset: NotificationSoundPreset) {
        prefs.edit().putString(KEY_NOTIFICATION_SOUND, preset.storageValue).apply()
    }

    companion object {
        private const val PREFS_NAME = "gas_ledger_financial_feedback"
        private const val KEY_OPERATION_SOUND = "operation_sound"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
    }
}

data class FinancialOperationReceipt(
    val kind: FinancialOperationKind,
    val customerId: String,
    val customerName: String,
    val amount: Long,
    val balanceAfter: Long
)

object FinancialOperationFeedback {
    private const val CHANNEL_ID = "financial_operation_confirmations_v2"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "تأكيد العمليات المالية",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "تنبيه بعد تسجيل الدين أو التحصيل"
            setSound(null, null)
            enableVibration(true)
            setShowBadge(true)
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

        val settings = FinancialFeedbackStore(context).state()
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                operationNotificationId(receipt),
                notification
            )
            playNotificationSound(settings.notificationSound)
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and posting.
        }
    }

    fun playSelectedOperationSound(context: Context) {
        val preset = FinancialFeedbackStore(context).state().operationSound
        playOperationSound(preset)
    }

    fun playOperationSound(preset: OperationSoundPreset) {
        when (preset) {
            OperationSoundPreset.CASH_REGISTER -> playSequence(
                notes = listOf(
                    Note(1_318.51, 70, 0.34, true),
                    Note(1_760.00, 95, 0.38, true),
                    Note(2_637.02, 185, 0.40, true)
                ),
                gapMs = 24
            )
            OperationSoundPreset.COIN_CASCADE -> playSequence(
                notes = listOf(
                    Note(2_093.00, 65, 0.36, true),
                    Note(2_637.02, 70, 0.38, true),
                    Note(2_349.32, 75, 0.36, true),
                    Note(3_135.96, 150, 0.40, true)
                ),
                gapMs = 18
            )
            OperationSoundPreset.POS_PREMIUM -> playSequence(
                notes = listOf(
                    Note(783.99, 105, 0.31, false),
                    Note(1_174.66, 125, 0.34, false),
                    Note(1_568.00, 215, 0.36, false)
                ),
                gapMs = 30
            )
        }
    }

    fun playNotificationSound(preset: NotificationSoundPreset) {
        when (preset) {
            NotificationSoundPreset.CASH_PING -> playSequence(
                notes = listOf(
                    Note(1_568.00, 80, 0.34, true),
                    Note(2_349.32, 155, 0.38, true)
                ),
                gapMs = 22
            )
            NotificationSoundPreset.SOFT_BELL -> playSequence(
                notes = listOf(
                    Note(987.77, 120, 0.30, false),
                    Note(1_479.98, 220, 0.32, false)
                ),
                gapMs = 34
            )
            NotificationSoundPreset.DOUBLE_CHIME -> playSequence(
                notes = listOf(
                    Note(1_046.50, 115, 0.33, false),
                    Note(1_568.00, 115, 0.35, false),
                    Note(2_093.00, 170, 0.36, false)
                ),
                gapMs = 45
            )
        }
    }

    private fun playSequence(notes: List<Note>, gapMs: Int) {
        runCatching {
            val sampleRate = 44_100
            val totalMs = notes.sumOf { it.durationMs } + gapMs * (notes.size - 1)
            val totalSamples = (sampleRate * totalMs / 1000.0).toInt()
            val pcm = ShortArray(totalSamples)

            var cursor = 0
            notes.forEachIndexed { index, note ->
                val noteSamples = (sampleRate * note.durationMs / 1000.0).toInt()
                for (i in 0 until noteSamples) {
                    val progress = i.toDouble() / noteSamples.coerceAtLeast(1)
                    val t = i.toDouble() / sampleRate
                    val attack = (progress / 0.07).coerceIn(0.0, 1.0)
                    val release = ((1.0 - progress) / 0.82).coerceIn(0.0, 1.0)
                    val envelope = attack * release

                    val fundamental = sin(2.0 * PI * note.frequency * t)
                    val overtone = if (note.metallic) {
                        0.32 * sin(2.0 * PI * note.frequency * 2.71 * t) +
                            0.14 * sin(2.0 * PI * note.frequency * 4.19 * t)
                    } else {
                        0.20 * sin(2.0 * PI * note.frequency * 2.0 * t) +
                            0.08 * sin(2.0 * PI * note.frequency * 3.0 * t)
                    }

                    val sample = ((fundamental + overtone) * envelope * note.volume * Short.MAX_VALUE)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                    if (cursor + i < pcm.size) {
                        pcm[cursor + i] = sample.toShort()
                    }
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

            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(0)

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(maxOf(pcm.size * 2, minBuffer))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            try {
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(totalMs.toLong() + 90L)
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
        val durationMs: Int,
        val volume: Double,
        val metallic: Boolean
    )
}
