package fyi.appy.inksend.giladkutiel.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import fyi.appy.inksend.giladkutiel.InkSendApp
import fyi.appy.inksend.giladkutiel.data.AppContainer

/** The single [AppContainer] instance owned by [InkSendApp], for ViewModel factories. */
@Composable
fun localAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as InkSendApp).container
