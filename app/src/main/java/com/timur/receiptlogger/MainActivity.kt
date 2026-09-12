package com.timur.receiptlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.timur.receiptlogger.ui.theme.ReceiptLoggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReceiptLoggerApp()
        }
    }
}

@Composable
private fun ReceiptLoggerApp() {
    ReceiptLoggerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ReceiptApp()
        }
    }
}
