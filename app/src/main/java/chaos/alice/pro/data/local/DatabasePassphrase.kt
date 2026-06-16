package chaos.alice.pro.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

class DatabasePassphrase(context: Context) {
    private val sharedPrefs = getEncryptedSharedPrefs(context)
    
    private fun getEncryptedSharedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        return EncryptedSharedPreferences.create(
            context,
            "secure_db_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun getOrCreatePassphrase(): ByteArray {
        val stored = sharedPrefs.getString("db_passphrase", null)
        return if (stored != null) {
            stored.toByteArray(Charsets.ISO_8859_1)
        } else {
            val newPassphrase = ByteArray(32)
            SecureRandom().nextBytes(newPassphrase)
            sharedPrefs.edit()
                .putString("db_passphrase", String(newPassphrase, Charsets.ISO_8859_1))
                .apply()
            newPassphrase
        }
    }
}