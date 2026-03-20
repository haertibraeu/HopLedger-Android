package com.haertibraeu.hopledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.haertibraeu.hopledger.ui.navigation.HopLedgerNavHost
import com.haertibraeu.hopledger.ui.theme.HopLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HopLedgerTheme {
                HopLedgerNavHost()
            }
        }
    }
}
