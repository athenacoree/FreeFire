package com.athenacoree.music

import android.app.Application
import coil3.ImageLoader
import coil3.ImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache

class MusicApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.18)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("music-artwork"))
                    .maxSizeBytes(48L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
