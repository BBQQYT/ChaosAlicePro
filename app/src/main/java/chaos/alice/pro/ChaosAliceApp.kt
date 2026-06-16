package chaos.alice.pro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChaosAliceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // SQLCipher (net.zetetic:sqlcipher-android) does NOT auto-load its native
        // library — it must be loaded before Room opens the encrypted database.
        System.loadLibrary("sqlcipher")
    }
}
