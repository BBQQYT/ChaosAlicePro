package chaos.alice.pro

import android.app.Application
import chaos.alice.pro.data.network.GithubMirrorFallbackInterceptor
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ChaosAliceApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // SQLCipher (net.zetetic:sqlcipher-android) does NOT auto-load its native
        // library — it must be loaded before Room opens the encrypted database.
        System.loadLibrary("sqlcipher")
    }

    // Coil использует собственный OkHttp-клиент, поэтому даём ему свой ImageLoader
    // с тем же перехватчиком — иконки персонажей тоже падают на зеркало при сбое GitHub.
    override fun newImageLoader(): ImageLoader {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(GithubMirrorFallbackInterceptor())
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttp)
            .build()
    }
}
