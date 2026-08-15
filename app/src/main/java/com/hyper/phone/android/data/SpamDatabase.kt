package com.hyper.phone.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "spam_numbers")
data class SpamNumber(
    @PrimaryKey val number: String,
    val type: String // "exact", "prefix", "pattern"
)

@Dao
interface SpamDao {
    @Query("SELECT * FROM spam_numbers")
    fun getAllSpam(): Flow<List<SpamNumber>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spamNumber: SpamNumber)

    @Delete
    suspend fun delete(spamNumber: SpamNumber)
}

@Database(entities = [SpamNumber::class], version = 1, exportSchema = false)
abstract class SpamDatabase : RoomDatabase() {
    abstract fun spamDao(): SpamDao
}
