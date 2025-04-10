package com.soccertips.predictx.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.data.local.entities.FavoriteItem

@Database(entities = [FavoriteItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No changes needed for this migration
            }
        }


        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 1: Create a new table with the updated schema
                database.execSQL(
                    """
            CREATE TABLE favorites_new (
                fixtureId TEXT PRIMARY KEY,
                homeTeam TEXT,
                awayTeam TEXT,
                league TEXT,
                mDate TEXT,
                mTime TEXT,
                mStatus TEXT,
                outcome TEXT,
                pick TEXT,
                color INTEGER,
                hLogoPath TEXT,
                aLogoPath TEXT,
                leagueLogo TEXT
            )
            """
                )

                // Step 2: Copy data from the old table to the new table
                // Convert fixtureId from Int to String
                database.execSQL(
                    """
            INSERT INTO favorites_new (
                fixtureId, homeTeam, awayTeam, league, mDate, mTime, mStatus, outcome, pick, color, hLogoPath, aLogoPath, leagueLogo
            )
            SELECT
                CAST(fixtureId AS TEXT), homeTeam, awayTeam, league, mDate, mTime, mStatus, outcome, pick, color, hLogoPath, aLogoPath, leagueLogo
            FROM favorites
            """
                )

                // Step 3: Drop the old table
                database.execSQL("DROP TABLE favorites")

                // Step 4: Rename the new table to the original table name
                database.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                   .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    //.fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}