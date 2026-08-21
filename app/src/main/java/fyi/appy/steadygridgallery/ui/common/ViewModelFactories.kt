package fyi.appy.steadygridgallery.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import fyi.appy.steadygridgallery.SteadyGalleryApp
import fyi.appy.steadygridgallery.data.AppContainer

/** No DI framework is in the approved dependency list, so ViewModels reach the composition root through this. */
fun CreationExtras.appContainer(): AppContainer {
    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SteadyGalleryApp
    return app.container
}
