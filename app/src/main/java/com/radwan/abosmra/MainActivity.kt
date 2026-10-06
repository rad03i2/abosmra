package com.radwan.abosmra

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

class MainActivity : FragmentActivity() {
    private val notificationCustomerId = mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationCustomerId.value = intent?.getStringExtra(EXTRA_CUSTOMER_ID)
        setContent {
            GasLedgerApp(
                notificationCustomerId = notificationCustomerId.value,
                onNotificationHandled = { notificationCustomerId.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationCustomerId.value = intent.getStringExtra(EXTRA_CUSTOMER_ID)
    }

    companion object {
        const val EXTRA_CUSTOMER_ID = "notification_customer_id"
    }
}
