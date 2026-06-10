package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class BirdRepository(private val db: AppDatabase) {

    // Sessions
    val allSessions: Flow<List<SamplingSession>> = db.samplingSessionDao().getAllSessionsFlow()
    suspend fun getSessionsSync(): List<SamplingSession> = db.samplingSessionDao().getAllSessions()
    suspend fun getSessionById(id: Int): SamplingSession? = db.samplingSessionDao().getSessionById(id)
    suspend fun saveSession(session: SamplingSession): Long = db.samplingSessionDao().insertSession(session)
    suspend fun deleteSessionById(id: Int) = db.samplingSessionDao().deleteSessionById(id)

    // Projects
    val allProjects: Flow<List<Project>> = db.projectDao().getAllProjects()
    
    fun getProject(id: Int): Flow<Project?> = db.projectDao().getProjectById(id)
    
    suspend fun getProjectSync(id: Int): Project? = db.projectDao().getProjectByIdSync(id)

    suspend fun saveProject(project: Project): Long = db.projectDao().insertProject(project)

    suspend fun updateProject(project: Project) = db.projectDao().updateProject(project)

    suspend fun deleteProject(project: Project) = db.projectDao().deleteProject(project)

    // Sites
    fun getSitesForProject(projectId: Int): Flow<List<Site>> = db.siteDao().getSitesForProject(projectId)

    fun getSiteById(id: Int): Flow<Site?> = db.siteDao().getSiteById(id)
    
    suspend fun getSitesForProjectSync(projectId: Int): List<Site> = db.siteDao().getSitesForProjectSync(projectId)

    suspend fun getSiteByIdSync(id: Int): Site? = db.siteDao().getSiteByIdSync(id)

    suspend fun saveSite(site: Site): Long = db.siteDao().insertSite(site)

    suspend fun deleteSite(site: Site) = db.siteDao().deleteSite(site)

    // Samplings
    fun getSamplingsForSite(siteId: Int): Flow<List<Sampling>> = db.samplingDao().getSamplingsForSite(siteId)

    fun getSamplingById(id: Int): Flow<Sampling?> = db.samplingDao().getSamplingById(id)

    suspend fun getSamplingsForSiteSync(siteId: Int): List<Sampling> = db.samplingDao().getSamplingsForSiteSync(siteId)

    suspend fun getSamplingByIdSync(id: Int): Sampling? = db.samplingDao().getSamplingByIdSync(id)

    suspend fun saveSampling(sampling: Sampling): Long = db.samplingDao().insertSampling(sampling)

    suspend fun updateSampling(sampling: Sampling) = db.samplingDao().updateSampling(sampling)

    suspend fun deleteSampling(sampling: Sampling) = db.samplingDao().deleteSampling(sampling)

    // Observations
    fun getObservationsForSampling(samplingId: Int): Flow<List<Observation>> = db.observationDao().getObservationsForSampling(samplingId)

    fun getObservationById(id: Int): Flow<Observation?> = db.observationDao().getObservationById(id)

    val allObservations: Flow<List<Observation>> = db.observationDao().getAllObservations()

    suspend fun getObservationsForSamplingSync(samplingId: Int): List<Observation> = db.observationDao().getObservationsForSamplingSync(samplingId)

    suspend fun getAllObservationsSync(): List<Observation> = db.observationDao().getAllObservationsSync()

    suspend fun saveObservation(observation: Observation): Long = db.observationDao().insertObservation(observation)

    suspend fun deleteObservation(observation: Observation) = db.observationDao().deleteObservation(observation)

    // Templates
    val allTemplates: Flow<List<FormTemplate>> = db.formTemplateDao().getTemplates()

    suspend fun saveTemplate(template: FormTemplate): Long = db.formTemplateDao().insertTemplate(template)

    suspend fun deleteTemplate(template: FormTemplate) = db.formTemplateDao().deleteTemplate(template)

    // Sync logs
    val allSyncLogs: Flow<List<SyncLog>> = db.syncLogDao().getSyncLogs()

    suspend fun insertSyncLog(log: SyncLog) = db.syncLogDao().insertSyncLog(log)

    // Articles & News from All About Birds
    val allArticles: Flow<List<BirdArticle>> = db.birdArticleDao().getAllArticles()

    suspend fun saveArticles(articles: List<BirdArticle>) = db.birdArticleDao().insertArticles(articles)

    suspend fun fetchAndCacheAllAboutBirdsArticles(): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url("https://www.allaboutbirds.org/news/feed/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Android BirdWatch/1.0")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string() ?: return@withContext false
                
                val articles = mutableListOf<BirdArticle>()
                val items = body.split("<item>")
                for (i in 1 until items.size) {
                    val itemContent = items[i].split("</item>")[0]
                    
                    val title = extractTagContent(itemContent, "title") ?: "Sin Título"
                    val link = extractTagContent(itemContent, "link") ?: "https://www.allaboutbirds.org/news/articles_${System.currentTimeMillis()}_$i"
                    val pubDate = extractTagContent(itemContent, "pubDate") ?: ""
                    val description = extractTagContent(itemContent, "description") ?: ""
                    val creator = extractTagContent(itemContent, "dc:creator") ?: "Cornell Lab"
                    val category = extractTagContent(itemContent, "category") ?: "Mundo Aves"
                    
                    val cleanDesc = description
                        .replace("<![CDATA[", "")
                        .replace("]]>", "")
                        .replace(Regex("<.*?>"), "") // Strip HTML tag syntax
                        .trim()
                    
                    val cleanTitle = title
                        .replace("<![CDATA[", "")
                        .replace("]]>", "")
                        .trim()

                    articles.add(
                        BirdArticle(
                            link = link,
                            title = cleanTitle,
                            description = if (cleanDesc.length > 200) cleanDesc.take(197) + "..." else cleanDesc,
                            pubDate = pubDate,
                            creator = creator,
                            category = category
                        )
                    )
                }

                if (articles.isNotEmpty()) {
                    db.birdArticleDao().insertArticles(articles)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun extractTagContent(xml: String, tagName: String): String? {
        val openTag = "<$tagName>"
        val closeTag = "</$tagName>"
        val startIndex = xml.indexOf(openTag)
        if (startIndex == -1) {
            val tagRegex = Regex("<$tagName>(.*?)</$tagName>", RegexOption.DOT_MATCHES_ALL)
            val match = tagRegex.find(xml)
            return match?.groupValues?.get(1)
        }
        val endIndex = xml.indexOf(closeTag, startIndex + openTag.length)
        if (endIndex == -1) return null
        return xml.substring(startIndex + openTag.length, endIndex)
    }

    // Check & Prepopulate templates if empty
    suspend fun prePopulateTemplates() {
        val current = db.formTemplateDao().getTemplatesSync()
        if (current.isEmpty()) {
            val template1 = FormTemplate(
                name = "Monitoreo Estándar de Avifauna",
                fieldsJson = """
                    [
                      {"key": "cloud_cover", "label": "Cobertura de Nubes (%)", "type": "NUMBER", "required": true},
                      {"key": "canopy_status", "label": "Estado del Dosel", "type": "DROPDOWN", "options": "Denso, Medio, Abierto, Ninguuno", "required": false},
                      {"key": "wind_speed", "label": "Velocidad del Viento (km/h)", "type": "NUMBER", "required": false},
                      {"key": "noise_level", "label": "Nivel de Ruido Ambiental", "type": "DROPDOWN", "options": "Bajo, Moderado, Alto", "required": true}
                    ]
                """.trimIndent()
            )
            val template2 = FormTemplate(
                name = "Ficha de Hábitat y Vegetación",
                fieldsJson = """
                    [
                      {"key": "canopy_height", "label": "Altura Promedio de Dosel (m)", "type": "NUMBER", "required": true},
                      {"key": "understory_density", "label": "Densidad de Sotobosque", "type": "DROPDOWN", "options": "Alta, Media, Baja, Nula", "required": true},
                      {"key": "human_impact", "label": "Evidencia de Impacto Humano", "type": "DROPDOWN", "options": "Grave, Moderado, Leve, Ninguno", "required": true}
                    ]
                """.trimIndent()
            )
            db.formTemplateDao().insertTemplate(template1)
            db.formTemplateDao().insertTemplate(template2)
        }
    }
}
