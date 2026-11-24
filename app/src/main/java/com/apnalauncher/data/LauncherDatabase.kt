package com.apnalauncher.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    @Query("SELECT * FROM home_items ORDER BY page, y, x")
    fun homeItems(): Flow<List<HomeItem>>

    @Query("SELECT * FROM home_items ORDER BY page, y, x")
    suspend fun allHomeItems(): List<HomeItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHomeItem(item: HomeItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHomeItems(items: List<HomeItem>)

    @Query("DELETE FROM home_items WHERE id = :id")
    suspend fun deleteHomeItem(id: Long)

    @Query("DELETE FROM home_items WHERE page = :page")
    suspend fun clearPage(page: Int)

    @Query("DELETE FROM home_items")
    suspend fun clearHome()

    @Query("SELECT * FROM folders")
    fun folders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders")
    suspend fun allFolders(): List<Folder>

    @Query("SELECT * FROM folder_apps")
    suspend fun allFolderApps(): List<FolderApp>

    @Query("SELECT * FROM hidden_apps")
    suspend fun allHidden(): List<HiddenApp>

    @Query("SELECT * FROM locked_apps")
    suspend fun allLocked(): List<LockedApp>

    @Query("SELECT * FROM app_custom")
    suspend fun allCustoms(): List<AppCustom>

    @Upsert
    suspend fun saveFolder(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("SELECT * FROM folder_apps WHERE folderId = :folderId")
    fun folderApps(folderId: String): Flow<List<FolderApp>>

    @Upsert
    suspend fun saveFolderApp(item: FolderApp)

    @Query("DELETE FROM folder_apps WHERE folderId = :folderId AND key = :key")
    suspend fun deleteFolderApp(folderId: String, key: String)

    @Query("SELECT * FROM hidden_apps")
    fun hiddenApps(): Flow<List<HiddenApp>>

    @Upsert
    suspend fun hideApp(item: HiddenApp)

    @Query("DELETE FROM hidden_apps WHERE key = :key")
    suspend fun unhideApp(key: String)

    @Query("SELECT * FROM locked_apps")
    fun lockedApps(): Flow<List<LockedApp>>

    @Upsert
    suspend fun lockApp(item: LockedApp)

    @Query("DELETE FROM locked_apps WHERE key = :key")
    suspend fun unlockApp(key: String)

    @Query("SELECT * FROM app_custom")
    fun customs(): Flow<List<AppCustom>>

    @Upsert
    suspend fun saveCustom(custom: AppCustom)

    @Query("DELETE FROM app_custom WHERE key = :key")
    suspend fun deleteCustom(key: String)

    @Query("DELETE FROM home_items")
    suspend fun wipeLayout()
}

@Database(
    entities = [HomeItem::class, Folder::class, FolderApp::class, HiddenApp::class,
        LockedApp::class, AppCustom::class],
    version = 1,
    exportSchema = false,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun dao(): LauncherDao

    companion object {
        @Volatile
        private var instance: LauncherDatabase? = null

        fun get(context: Context): LauncherDatabase = instance ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, LauncherDatabase::class.java, "apna-launcher.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
