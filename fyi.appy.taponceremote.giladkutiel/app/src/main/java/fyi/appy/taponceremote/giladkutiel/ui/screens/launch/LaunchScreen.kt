package fyi.appy.taponceremote.giladkutiel.ui.screens.launch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.taponceremote.giladkutiel.TapOnceApplication

@Composable
fun LaunchScreen(onNavigate: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as TapOnceApplication
    val viewModel: LaunchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LaunchViewModel(app.database.savedDeviceDao()) }
        },
    )

    LaunchedEffect(Unit) {
        viewModel.navigateTo.collect { route -> onNavigate(route) }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "TapOnce Remote",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text("Preparing remote", style = MaterialTheme.typography.bodyLarge)
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
