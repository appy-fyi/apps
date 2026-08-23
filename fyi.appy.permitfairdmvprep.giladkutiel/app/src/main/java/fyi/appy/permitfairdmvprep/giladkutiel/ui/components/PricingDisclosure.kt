package fyi.appy.permitfairdmvprep.giladkutiel.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reused on Unlock and Settings — appy build-spec feature "No deceptive trial or cancellation
 * surface". Deliberately the only place the phrase "no free trial" appears.
 */
@Composable
fun PricingDisclosure(modifier: Modifier = Modifier) {
    Text(
        text = "This app uses one optional one-time Google Play purchase. There is no subscription, " +
            "no free trial, and no in-app account to cancel.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 8.dp),
    )
}
