package com.ytmusicdl.app.data.db

import android.content.Context
import androidx.room.*

@Entity(tableName = "download_history_cache")
data class DownloadHistoryCacheEntity(
    @PrimaryKey val filePath: String,
    val title: String,
    val album: String,
    val duration: String,
    val coverBytes: ByteArray?,
    val lastModified: Long,
)

@Dao
interface DownloadHistoryCacheDao {
    @Query("SELECT * FROM download_history_cache ORDER BY lastModified DESC")
    suspend fun getAll(): List<DownloadHistoryCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DownloadHistoryCacheEntity>)

    @Query("DELETE FROM download_history_cache WHERE filePath NOT IN (:existingPaths)")
    suspend fun deleteMissing(existingPaths: List<String>)
}

@Database(entities = [DownloadHistoryCacheEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyCacheDao(): DownloadHistoryCacheDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ytmusicdl.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
