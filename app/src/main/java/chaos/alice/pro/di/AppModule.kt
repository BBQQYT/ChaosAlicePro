package chaos.alice.pro.di

import android.content.Context
import androidx.room.Room
import chaos.alice.pro.data.local.AppDatabase
import chaos.alice.pro.data.local.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "chaos_alice_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // 👇 ВОТ ОН, ВЕРНУЛСЯ НА СВОЕ МЕСТО
    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase): ChatDao {
        return appDatabase.chatDao()
    }
}