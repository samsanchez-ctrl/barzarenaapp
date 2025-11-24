package cl.samuel.barzarena.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.samuel.barzarena.data.local.AppDatabase
import cl.samuel.barzarena.data.local.dao.BetDao
import cl.samuel.barzarena.data.local.dao.ItemDao
import cl.samuel.barzarena.data.local.dao.UserDao
import cl.samuel.barzarena.data.local.model.Item
import cl.samuel.barzarena.data.remote.ApiService
import cl.samuel.barzarena.data.repository.BetRepository
import cl.samuel.barzarena.data.repository.ItemRepository
import cl.samuel.barzarena.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Base de Datos Local ---
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context, itemDaoProvider: javax.inject.Provider<ItemDao>): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "barzarena_database"
        ).fallbackToDestructiveMigration()
         .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val itemDao = itemDaoProvider.get()
                    itemDao.insertItem(Item(name = "Microfono de Oro", price = 15000.0, stock = 10, imageName = "microfonodeoro"))
                    itemDao.insertItem(Item(name = "Pulsera de Lujo", price = 8000.0, stock = 20, imageName = "pulseradelujo"))
                    itemDao.insertItem(Item(name = "Cadena de Lujo", price = 12000.0, stock = 15, imageName = "cadenadelujo"))
                }
            }
         })
        .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideBetDao(database: AppDatabase): BetDao = database.betDao()

    @Provides
    @Singleton
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao, apiService: ApiService): UserRepository {
        return UserRepository(userDao, apiService)
    }

    @Provides
    @Singleton
    fun provideBetRepository(betDao: BetDao): BetRepository {
        return BetRepository(betDao)
    }

    @Provides
    @Singleton
    fun provideItemRepository(itemDao: ItemDao): ItemRepository {
        return ItemRepository(itemDao)
    }

    // --- API Remota ---
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://ninantmp.com/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
