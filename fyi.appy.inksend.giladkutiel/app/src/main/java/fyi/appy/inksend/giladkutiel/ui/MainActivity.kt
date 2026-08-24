package fyi.appy.inksend.giladkutiel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import fyi.appy.inksend.giladkutiel.InkSendApp
import fyi.appy.inksend.giladkutiel.theme.InkSendTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as InkSendApp).container

        setContent {
            InkSendTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InkSendNavGraph()
                }
                LaunchedEffect(Unit) {
                    launch { container.billingRepository.refreshPurchaseState() }
                }
            }
        }
    }
}
