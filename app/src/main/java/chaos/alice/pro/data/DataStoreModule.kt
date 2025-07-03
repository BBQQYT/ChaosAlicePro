package chaos.alice.pro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * ЕДИНСТВЕННЫЙ ИСТОЧНИК DataStore для файла настроек "settings".
 * Это свойство-расширение будет использоваться всеми репозиториями.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")