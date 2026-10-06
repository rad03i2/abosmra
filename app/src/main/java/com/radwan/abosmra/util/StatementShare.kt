package com.radwan.abosmra.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object StatementShare {
    fun shareImage(
        context: Context,
        file: File,
        message: String,
        whatsappOnly: Boolean
    ) {
        shareFile(context, file, "image/png", message, whatsappOnly, null)
    }

    fun shareImageToWhatsappContact(
        context: Context,
        file: File,
        message: String,
        phone: String
    ) {
        val normalized = normalizeIraqPhone(phone)
            .filter(Char::isDigit)
            .removePrefix("00")
        require(normalized.isNotBlank()) { "رقم الهاتف غير صالح." }
        shareFile(
            context = context,
            file = file,
            mimeType = "image/png",
            message = message,
            whatsappOnly = true,
            whatsappJid = normalized + "@s.whatsapp.net"
        )
    }

    fun sharePdf(
        context: Context,
        file: File,
        message: String
    ) {
        shareFile(context, file, "application/pdf", message, false, null)
    }

    private fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        message: String,
        whatsappOnly: Boolean,
        whatsappJid: String?
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        fun buildIntent(packageName: String? = null): Intent =
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                clipData = ClipData.newRawUri("statement", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                packageName?.let(::setPackage)
                whatsappJid?.let { putExtra("jid", it) }
            }

        if (whatsappOnly) {
            val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
            val launched = packages.any { packageName ->
                runCatching {
                    context.startActivity(buildIntent(packageName))
                    true
                }.getOrDefault(false)
            }
            if (!launched) {
                context.startActivity(
                    Intent.createChooser(
                        buildIntent(),
                        "مشاركة كشف الحساب"
                    )
                )
            }
        } else {
            context.startActivity(
                Intent.createChooser(
                    buildIntent(),
                    "مشاركة كشف الحساب"
                )
            )
        }
    }
}
