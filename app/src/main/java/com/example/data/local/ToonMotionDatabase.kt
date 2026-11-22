package com.example.data.local

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

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val updatedAtMs: Long,
    val sceneCount: Int,
    val backgroundName: String,
    val jsonPayload: String
)

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val styleName: String,
    val isTemplate: Boolean,
    val updatedAtMs: Long,
    val jsonPayload: String
)

@Entity(tableName = "scene_templates")
data class SceneTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val backgroundName: String,
    val updatedAtMs: Long,
    val jsonPayload: String
)

@Dao
interface StudioDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtMs DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("SELECT * FROM characters ORDER BY isTemplate ASC, updatedAtMs DESC")
    fun observeCharacters(): Flow<List<CharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCharacter(entity: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacter(id: String)

    @Query("SELECT * FROM scene_templates ORDER BY updatedAtMs DESC")
    fun observeSceneTemplates(): Flow<List<SceneTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSceneTemplate(entity: SceneTemplateEntity)

    @Query("DELETE FROM scene_templates WHERE id = :id")
    suspend fun deleteSceneTemplate(id: String)
}

@Database(
    entities = [ProjectEntity::class, CharacterEntity::class, SceneTemplateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ToonMotionDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: ToonMotionDatabase? = null

        fun getInstance(context: Context): ToonMotionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ToonMotionDatabase::class.java,
                    "toonmotion_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
