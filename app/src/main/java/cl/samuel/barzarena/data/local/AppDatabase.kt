package cl.samuel.barzarena.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cl.samuel.barzarena.data.local.converters.Converters
import cl.samuel.barzarena.data.local.dao.BetDao
import cl.samuel.barzarena.data.local.dao.ItemDao
import cl.samuel.barzarena.data.local.dao.UserDao
import cl.samuel.barzarena.data.local.model.Bet
import cl.samuel.barzarena.data.local.model.Item
import cl.samuel.barzarena.data.local.model.User

@Database(entities = [User::class, Bet::class, Item::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun betDao(): BetDao
    abstract fun itemDao(): ItemDao

}
