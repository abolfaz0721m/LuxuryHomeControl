package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey val name: String,
    val version: String,
    val summary: String,
    val installDate: Long = System.currentTimeMillis(),
    val isCustom: Boolean = false
)

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages ORDER BY name ASC")
    fun getAllPackages(): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages ORDER BY name ASC")
    suspend fun getAllPackagesSync(): List<PackageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: PackageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(pkgs: List<PackageEntity>)

    @Query("DELETE FROM packages WHERE name = :name")
    suspend fun deletePackage(name: String)

    @Query("SELECT COUNT(*) FROM packages")
    suspend fun count(): Int
}

@Database(entities = [PackageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packageDao(): PackageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "highrise_bot_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
