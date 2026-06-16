package chaos.alice.pro.di

import android.content.Context
import androidx.room.Room
import chaos.alice.pro.data.local.AppDatabase
import chaos.alice.pro.data.local.ChatDao
import chaos.alice.pro.data.local.DatabasePassphrase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphrase: DatabasePassphrase
    ): AppDatabase {
        val factory = SupportOpenHelperFactory(passphrase.getOrCreatePassphrase())
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "chaos_alice_db"
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase): ChatDao {
        return appDatabase.chatDao()
    }

    @Provides
    @Singleton
    fun provideDatabasePassphrase(@ApplicationContext context: Context): DatabasePassphrase {
        return DatabasePassphrase(context)
    }
}