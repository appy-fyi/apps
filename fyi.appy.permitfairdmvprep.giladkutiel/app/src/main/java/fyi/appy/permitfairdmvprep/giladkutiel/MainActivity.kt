package fyi.appy.permitfairdmvprep.giladkutiel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import fyi.appy.permitfairdmvprep.giladkutiel.ui.navigation.AppNavGraph
import fyi.appy.permitfairdmvprep.giladkutiel.ui.theme.PermitFairTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PermitFairApp

        setContent {
            PermitFairTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        contentRepository = app.contentRepository,
                        progressRepository = app.progressRepository,
                        billingRepository = app.billingRepository,
                    )
                }
            }
        }
    }
}
