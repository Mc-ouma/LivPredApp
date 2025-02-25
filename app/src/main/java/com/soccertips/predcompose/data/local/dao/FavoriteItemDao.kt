package com.soccertips.predcompose.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteItem(favoriteItem: FavoriteItem)

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteItem>

    @Query("DELETE FROM favorites WHERE fixtureId = :fixtureId")
    suspend fun deleteFavoriteItem(fixtureId: String)

    @Query("SELECT COUNT(*) FROM favorites")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT * FROM favorites WHERE mTime = :mTime AND mDate = :mDate")
    suspend fun getDueItem(mTime: String, mDate: String): List<FavoriteItem>
}
