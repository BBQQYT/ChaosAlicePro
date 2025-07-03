package chaos.alice.pro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// Увеличиваем версию до 2
@Database(entities = [MessageEntity::class, ChatEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}