package com.appyfyi.steadygridgallery.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.appyfyi.steadygridgallery.SteadyGalleryApp
import com.appyfyi.steadygridgallery.data.AppContainer

/** No DI framework is in the approved dependency list, so ViewModels reach the composition root through this. */
fun CreationExtras.appContainer(): AppContainer {
    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SteadyGalleryApp
    return app.container
}
