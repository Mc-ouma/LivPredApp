package com.soccertips.predictx.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.soccertips.predictx.data.local.entities.FavoriteItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteItem(favoriteItem: FavoriteItem)

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteItem>

    @Query("SELECT * FROM favorites")
    fun getAllFavoritesFlow(): Flow<List<FavoriteItem>>

    @Query("DELETE FROM favorites WHERE fixtureId = :fixtureId")
    suspend fun deleteFavoriteItem(fixtureId: String)

    @Query("SELECT COUNT(*) FROM favorites")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT * FROM favorites WHERE mTime = :mTime AND mDate = :mDate")
    suspend fun getDueItem(mTime: String, mDate: String): List<FavoriteItem>

    @Update
    suspend fun updateFavoriteItem(favoriteItem: FavoriteItem)

    @Query("SELECT * FROM favorites WHERE fixtureId = :fixtureId")
    fun getFavoriteItemByFixtureId(fixtureId: String): FavoriteItem
}
