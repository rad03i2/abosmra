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
        shareFile(context, file, "image/png", message, whatsappOnly)
    }

    fun sharePdf(
        context: Context,
        file: File,
        message: String
    ) {
        shareFile(context, file, "application/pdf", message, false)
    }

    private fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        message: String,
        whatsappOnly: Boolean
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        fun buildIntent(targetWhatsapp: Boolean): Intent =
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                clipData = ClipData.newRawUri("statement", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetWhatsapp) setPackage("com.whatsapp")
            }

        if (whatsappOnly) {
            runCatching { context.startActivity(buildIntent(true)) }
                .onFailure {
                    context.startActivity(
                        Intent.createChooser(
                            buildIntent(false),
                            "مشاركة كشف الحساب"
                        )
                    )
                }
        } else {
            context.startActivity(
                Intent.createChooser(
                    buildIntent(false),
                    "مشاركة كشف الحساب"
                )
            )
        }
    }
}
