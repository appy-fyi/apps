package fyi.appy.taponceremote.giladkutiel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import fyi.appy.taponceremote.giladkutiel.ui.navigation.TapOnceNavGraph
import fyi.appy.taponceremote.giladkutiel.ui.theme.TapOnceRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TapOnceRemoteApp()
        }
    }
}

@Composable
fun TapOnceRemoteApp() {
    TapOnceRemoteTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            TapOnceNavGraph(navController)
        }
    }
}
