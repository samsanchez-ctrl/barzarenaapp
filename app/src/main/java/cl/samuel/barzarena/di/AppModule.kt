package cl.samuel.barzarena.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.samuel.barzarena.data.local.AppDatabase
import cl.samuel.barzarena.data.local.SessionManager
import cl.samuel.barzarena.data.local.dao.BetDao
import cl.samuel.barzarena.data.local.dao.ItemDao
import cl.samuel.barzarena.data.local.dao.UserDao
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- GESTOR DE SESIÓN ---
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    // --- Base de Datos Local ---
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "barzarena_database"
        ).fallbackToDestructiveMigration()
         .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Use un executor para insertar los datos iniciales en la base de datos.
                Executors.newSingleThreadExecutor().execute {
                    db.execSQL("INSERT INTO items (name, price, stock, imageName) VALUES ('Microfono de Oro', 15000.0, 10, 'microfonodeoro')")
                    db.execSQL("INSERT INTO items (name, price, stock, imageName) VALUES ('Pulsera de Lujo', 8000.0, 20, 'pulseradelujo')")
                    db.execSQL("INSERT INTO items (name, price, stock, imageName) VALUES ('Cadena de Lujo', 12000.0, 15, 'cadenadelujo')")
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
