package com.soccertips.predcompose.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soccertips.predcompose.data.local.entities.FavoriteItem

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoriteItem: FavoriteItem)

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteItem>

    @Query("DELETE FROM favorites WHERE fixtureId = :fixtureId")
    suspend fun delete(fixtureId: String)
}
