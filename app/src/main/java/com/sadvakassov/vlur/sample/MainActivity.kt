package com.sadvakassov.vlur.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sadvakassov.vlur.sample.theme.VlurTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableTimber()

        System.loadLibrary("vlur")

        enableEdgeToEdge()
        setContent {
            VlurTheme {
                MainView()
            }
        }
    }

    private fun enableTimber() {
        Timber.plant(Timber.DebugTree())
    }
}
