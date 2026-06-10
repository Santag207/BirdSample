package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Int): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdSync(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)
}

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites WHERE projectId = :projectId ORDER BY id DESC")
    fun getSitesForProject(projectId: Int): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE id = :id")
    fun getSiteById(id: Int): Flow<Site?>

    @Query("SELECT * FROM sites WHERE projectId = :projectId")
    suspend fun getSitesForProjectSync(projectId: Int): List<Site>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getSiteByIdSync(id: Int): Site?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: Site): Long

    @Delete
    suspend fun deleteSite(site: Site)
}

@Dao
interface SamplingDao {
    @Query("SELECT * FROM samplings WHERE siteId = :siteId ORDER BY date DESC")
    fun getSamplingsForSite(siteId: Int): Flow<List<Sampling>>

    @Query("SELECT * FROM samplings WHERE id = :id")
    fun getSamplingById(id: Int): Flow<Sampling?>

    @Query("SELECT * FROM samplings WHERE siteId = :siteId")
    suspend fun getSamplingsForSiteSync(siteId: Int): List<Sampling>

    @Query("SELECT * FROM samplings WHERE id = :id")
    suspend fun getSamplingByIdSync(id: Int): Sampling?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSampling(sampling: Sampling): Long

    @Update
    suspend fun updateSampling(sampling: Sampling)

    @Delete
    suspend fun deleteSampling(sampling: Sampling)
}

@Dao
interface ObservationDao {
    @Query("SELECT * FROM observations WHERE samplingId = :samplingId ORDER BY timestamp DESC")
    fun getObservationsForSampling(samplingId: Int): Flow<List<Observation>>

    @Query("SELECT * FROM observations WHERE samplingId = :samplingId ORDER BY timestamp DESC")
    suspend fun getObservationsForSamplingSync(samplingId: Int): List<Observation>

    @Query("SELECT * FROM observations WHERE id = :id")
    fun getObservationById(id: Int): Flow<Observation?>

    @Query("SELECT * FROM observations ORDER BY timestamp DESC")
    fun getAllObservations(): Flow<List<Observation>>

    @Query("SELECT * FROM observations ORDER BY timestamp DESC")
    suspend fun getAllObservationsSync(): List<Observation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: Observation): Long

    @Delete
    suspend fun deleteObservation(observation: Observation)
}

@Dao
interface FormTemplateDao {
    @Query("SELECT * FROM form_templates")
    fun getTemplates(): Flow<List<FormTemplate>>

    @Query("SELECT * FROM form_templates")
    suspend fun getTemplatesSync(): List<FormTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: FormTemplate): Long

    @Delete
    suspend fun deleteTemplate(template: FormTemplate)
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC")
    fun getSyncLogs(): Flow<List<SyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLog): Long
}

@Dao
interface BirdArticleDao {
    @Query("SELECT * FROM bird_articles ORDER BY pubDate DESC")
    fun getAllArticles(): Flow<List<BirdArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<BirdArticle>)

    @Query("DELETE FROM bird_articles")
    suspend fun deleteAllArticles()
}

@Dao
interface SamplingSessionDao {
    @Query("SELECT * FROM sampling_sessions ORDER BY createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<SamplingSession>>

    @Query("SELECT * FROM sampling_sessions ORDER BY createdAt DESC")
    suspend fun getAllSessions(): List<SamplingSession>

    @Query("SELECT * FROM sampling_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): SamplingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SamplingSession): Long

    @Query("DELETE FROM sampling_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)
}

