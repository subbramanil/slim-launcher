package com.sduduzog.slimlauncher.ui.main.model

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AppDao {


    @get:Query("SELECT * FROM apps WHERE package_name NOT IN (SELECT apps.package_name from apps JOIN home_apps ON home_apps.package_name=apps.package_name) ORDER BY app_name ASC")
    val apps: LiveData<List<App>>

    @get:Query("SELECT * FROM home_apps ORDER BY app_name ASC")
    val homeApps: LiveData<List<HomeApp>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(app: App)

    @Insert
    fun addHomeApp(app: HomeApp)

    @Query("DELETE FROM apps")
    fun deleteAll()

    @Delete
    fun delete(app: HomeApp)

}
