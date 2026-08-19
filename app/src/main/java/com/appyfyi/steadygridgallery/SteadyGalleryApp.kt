package com.appyfyi.steadygridgallery

import android.app.Application
import com.appyfyi.steadygridgallery.data.AppContainer

class SteadyGalleryApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
