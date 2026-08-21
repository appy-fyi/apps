package fyi.appy.steadygridgallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import fyi.appy.steadygridgallery.data.AppContainer

class SteadyGalleryApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    // Grid/viewer thumbnails load straight from each MediaItem's contentUri via AsyncImage,
    // videos included -- without this decoder Coil has no way to pull a still frame out of a
    // video URI and the tile renders blank/black instead of a preview.
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .build()
}
