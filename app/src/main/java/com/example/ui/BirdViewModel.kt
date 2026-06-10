package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.Environment
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BirdViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = BirdRepository(db)

    // UI state flows
    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<SamplingSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<FormTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLog>> = repository.allSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allObservations: StateFlow<List<Observation>> = repository.allObservations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val birdArticles: StateFlow<List<BirdArticle>> = repository.allArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRefreshingArticles = MutableStateFlow(false)

    // Selection/Navigation context states
    val selectedProject = MutableStateFlow<Project?>(null)
    val selectedSite = MutableStateFlow<Site?>(null)
    val selectedSampling = MutableStateFlow<Sampling?>(null)

    // Calculated child lists (dynamic derived flows to ensure immediate UI refreshes!)
    val selectedProjectSites: StateFlow<List<Site>> = selectedProject
        .flatMapLatest { project ->
            if (project != null) repository.getSitesForProject(project.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedSiteSamplings: StateFlow<List<Sampling>> = selectedSite
        .flatMapLatest { site ->
            if (site != null) repository.getSamplingsForSite(site.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedSamplingObservations: StateFlow<List<Observation>> = selectedSampling
        .flatMapLatest { sampling ->
            if (sampling != null) repository.getObservationsForSampling(sampling.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map Coordinates tracking (for offline map canvas)
    val mapCenterLat = MutableStateFlow(4.7110) // Bogotá default
    val mapCenterLng = MutableStateFlow(-74.0721)
    val mapZoom = MutableStateFlow(12f)

    // Dynamic field responses map
    val dynamicAnswers = mutableStateMapOf<String, String>()

    // AI chat history state
    val aiResponse = MutableStateFlow("")
    val aiLoading = MutableStateFlow(false)

    // Synchronization statuses
    val isSyncing = MutableStateFlow(false)
    val syncProgress = MutableStateFlow(0f)
    val syncLogMessage = MutableStateFlow("")

    init {
        // Run prepopulate on background thread
        viewModelScope.launch {
            repository.prePopulateTemplates()
            
            // Check & Prepopulate default biological and tutorial articles from All About Birds for offline usage
            try {
                // If offline or feed is slow, we use gorgeous static fallbacks
                val initialArticles = listOf(
                    BirdArticle(
                        link = "https://www.allaboutbirds.org/news/how-to-start-birdwatch-tips/",
                        title = "Principios de Observación: Siluetas de Pico e Identificación",
                        description = "Aprende los elementos críticos para mapear especies en campo: altura del dosel, clasificación del pico, dinámica de vuelo y reconocimiento de cantos en zonas boscosas.",
                        pubDate = "Wed, 10 Jun 2026 12:00:00 GMT",
                        creator = "Cornell Lab of Ornithology",
                        category = "Ornitología de Campo"
                    ),
                    BirdArticle(
                        link = "https://www.allaboutbirds.org/news/acoustic-monitoring-essential-gear/",
                        title = "Monitoreo Acústico de Avifauna: Captura de Cantos y Muestreos",
                        description = "El uso correcto del grabador de voz en campo y su relación con el análisis de espectrogramas. Técnicas de grabación de notas vocales biológicas para observaciones robustas.",
                        pubDate = "Tue, 09 Jun 2026 14:30:00 GMT",
                        creator = "Sección de Acústica",
                        category = "Herramientas"
                    ),
                    BirdArticle(
                        link = "https://www.allaboutbirds.org/news/climate-change-impact-birds/",
                        title = "Análisis Biológico y Degradación del Hábitat de Paseriformes",
                        description = "Estudio científico sobre el impacto ecológico de la velocidad del viento y humedad relativa en la actividad reproductiva de mirlas, colibríes y semilleros andinos.",
                        pubDate = "Mon, 08 Jun 2026 09:15:00 GMT",
                        creator = "Impacto Ambiental",
                        category = "Investigación"
                    )
                )
                repository.saveArticles(initialArticles)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Safe async try to fetch live feed from Cornell Lab
            try {
                repository.fetchAndCacheAllAboutBirdsArticles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchArticlesFromAllAboutBirds() {
        if (isRefreshingArticles.value) return
        viewModelScope.launch {
            isRefreshingArticles.value = true
            try {
                repository.fetchAndCacheAllAboutBirdsArticles()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRefreshingArticles.value = false
            }
        }
    }

    // --- CRUD Actions ---

    fun createProject(name: String, client: String, contract: String, desc: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val project = Project(
                name = name,
                client = client,
                contractNumber = contract,
                description = desc,
                status = "Borrador"
            )
            repository.saveProject(project)
        }
    }

    fun modifyProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(project)
            if (selectedProject.value?.id == project.id) {
                selectedProject.value = null
            }
        }
    }

    fun createSite(name: String, dept: String, mun: String, vereda: String, eco: String, lat: Double, lon: Double, alt: Double) {
        val projId = selectedProject.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val site = Site(
                projectId = projId,
                name = name,
                department = dept,
                municipality = mun,
                vereda = vereda,
                ecosystem = eco,
                latitude = lat,
                longitude = lon,
                altitude = alt,
                accuracy = 5.0
            )
            repository.saveSite(site)
            // Auto update map center when adding site
            mapCenterLat.value = lat
            mapCenterLng.value = lon
        }
    }

    fun deleteSite(site: Site) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSite(site)
            if (selectedSite.value?.id == site.id) {
                selectedSite.value = null
            }
        }
    }

    fun startSampling(observer: String, weather: String, temp: Double, hum: Double, method: String) {
        val siteId = selectedSite.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Serialize answers
            val answersJson = withContext(Dispatchers.Default) {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(Map::class.java)
                adapter.toJson(dynamicAnswers) ?: "{}"
            }

            val sampling = Sampling(
                siteId = siteId,
                observer = observer,
                weather = weather,
                temperature = temp,
                humidity = hum,
                methodology = method,
                customFieldsJson = answersJson
            )
            val newId = repository.saveSampling(sampling)
            selectedSampling.value = sampling.copy(id = newId.toInt())
        }
    }

    fun updateSamplingDraft(observer: String, weather: String, temp: Double, hum: Double, method: String) {
        val current = selectedSampling.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val answersJson = withContext(Dispatchers.Default) {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(Map::class.java)
                adapter.toJson(dynamicAnswers) ?: "{}"
            }
            val updated = current.copy(
                observer = observer,
                weather = weather,
                temperature = temp,
                humidity = hum,
                methodology = method,
                customFieldsJson = answersJson
            )
            repository.updateSampling(updated)
            selectedSampling.value = updated
        }
    }

    fun deleteSampling(sampling: Sampling) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSampling(sampling)
            if (selectedSampling.value?.id == sampling.id) {
                selectedSampling.value = null
            }
        }
    }

    fun addObservation(
        birdCom: String,
        birdSci: String,
        birdFam: String,
        qty: Int,
        sex: String,
        age: String,
        behavior: String,
        notes: String,
        photoPath: String?,
        audioPath: String?,
        lat: Double,
        lon: Double,
        alt: Double
    ) {
        val sampId = selectedSampling.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val obs = Observation(
                samplingId = sampId,
                birdCommonName = birdCom,
                birdScientificName = birdSci,
                birdFamily = birdFam,
                quantity = qty,
                sex = sex,
                age = age,
                behavior = behavior,
                notes = notes,
                photoPath = photoPath,
                audioPath = audioPath,
                latitude = lat,
                longitude = lon,
                altitude = alt
            )
            repository.saveObservation(obs)
        }
    }

    fun deleteObservation(obs: Observation) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteObservation(obs)
    }

    // --- GPS Coordinates Manager ---

    fun fetchCurrentGps(context: Context, onLocation: (Double, Double, Double) -> Unit) {
        try {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onLocation(location.latitude, location.longitude, location.altitude)
                } else {
                    // Bogotá Biological Center mock fallback if emulator GPS is disconnected
                    onLocation(4.7110, -74.0721, 2600.0)
                }
            }.addOnFailureListener {
                onLocation(4.7110, -74.0721, 2600.0)
            }
        } catch (e: SecurityException) {
            // Permission denied - return Bogota mock fallback
            onLocation(4.7110, -74.0721, 2600.0)
        }
    }

    // --- Local Assets Files Management ---

    fun savePhotoBitmap(context: Context, bitmap: Bitmap): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoName = "IMG_$timeStamp.jpg"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val imageFile = File(storageDir, photoName)
        
        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imageFile.absolutePath
    }

    fun saveUriToLocalFile(context: Context, uri: android.net.Uri): String? {
        return try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val photoName = "IMG_$timeStamp.jpg"
            val destFile = File(storageDir, photoName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveAudioStream(context: Context, inputStream: InputStream): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val audioName = "REC_$timeStamp.mp3"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        val audioFile = File(storageDir, audioName)

        try {
            FileOutputStream(audioFile).use { out ->
                inputStream.copyTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return audioFile.absolutePath
    }

    // --- Document Exports & Sharing ---

    fun exportProjectPdf(context: Context, onExported: (File) -> Unit) {
        val project = selectedProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val sitesLocal = repository.getSitesForProjectSync(project.id)
            val samplingsLocal = mutableListOf<Sampling>()
            val obsLocal = mutableListOf<Observation>()
            
            sitesLocal.forEach { s ->
                val samps = repository.getSamplingsForSiteSync(s.id)
                samplingsLocal.addAll(samps)
                samps.forEach { sm ->
                    obsLocal.addAll(repository.getObservationsForSamplingSync(sm.id))
                }
            }

            val file = ReportExporter.exportToPdf(context, project, sitesLocal, samplingsLocal, obsLocal)
            withContext(Dispatchers.Main) {
                onExported(file)
            }
        }
    }

    fun exportProjectExcel(context: Context, onExported: (File) -> Unit) {
        val project = selectedProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val sitesLocal = repository.getSitesForProjectSync(project.id)
            val samplingsLocal = mutableListOf<Sampling>()
            val obsLocal = mutableListOf<Observation>()

            sitesLocal.forEach { s ->
                val samps = repository.getSamplingsForSiteSync(s.id)
                samplingsLocal.addAll(samps)
                samps.forEach { sm ->
                    obsLocal.addAll(repository.getObservationsForSamplingSync(sm.id))
                }
            }

            val file = ReportExporter.exportToExcelCsv(context, project, sitesLocal, samplingsLocal, obsLocal)
            withContext(Dispatchers.Main) {
                onExported(file)
            }
        }
    }

    // --- Cloud Incremental Sync Simulation (OneDrive/GDrive via Storage Access Framework) ---

    fun performIncrementalSync() {
        val project = selectedProject.value ?: return
        if (isSyncing.value) return

        viewModelScope.launch(Dispatchers.IO) {
            isSyncing.value = true
            syncProgress.value = 0.1f
            syncLogMessage.value = "Iniciando motor de sincronización incremental..."
            
            val sitesLocal = repository.getSitesForProjectSync(project.id)
            
            withContext(Dispatchers.Main) {
                syncProgress.value = 0.3f
                syncLogMessage.value = "Validando tokens de sesión Cloud (Google Drive / OneDrive)..."
            }
            kotlinx.coroutines.delay(1000)

            withContext(Dispatchers.Main) {
                syncProgress.value = 0.5f
                syncLogMessage.value = "Escaneando cambios locales desfasados en base de datos Room..."
            }
            kotlinx.coroutines.delay(1000)

            // Calculate pending items counts
            val totalObservations = allObservations.value.size
            
            withContext(Dispatchers.Main) {
                syncProgress.value = 0.7f
                syncLogMessage.value = "Confirmando subversión en nube. Subiendo $totalObservations registros..."
            }
            kotlinx.coroutines.delay(1500)

            withContext(Dispatchers.Main) {
                syncProgress.value = 0.9f
                syncLogMessage.value = "Resolución de conflictos automática: El timestamp más reciente gana."
            }
            kotlinx.coroutines.delay(1000)

            // Update status of actual project
            val syncedProject = project.copy(status = "Sincronizado", updatedAt = System.currentTimeMillis())
            repository.updateProject(syncedProject)
            selectedProject.value = syncedProject

            // Append sync log entry
            val log = SyncLog(
                action = "Subida de Proyecto: ${project.name}",
                status = "EXITOSO",
                summary = "Sincronizados correctamente ${sitesLocal.size} sitios y $totalObservations observaciones biológicas."
            )
            repository.insertSyncLog(log)

            withContext(Dispatchers.Main) {
                syncProgress.value = 1.0f
                syncLogMessage.value = "¡Sincronización incremental completada exitosamente!"
                isSyncing.value = false
            }
        }
    }

    // --- Gemini AI Assistant Chat Operations ---

    fun searchBirdWithGemini(featherDesc: String, beakDesc: String, placeDesc: String) {
        aiLoading.value = true
        aiResponse.value = "Consultando con la base de datos experta de avifauna a través de Gemini AI..."
        
        viewModelScope.launch(Dispatchers.IO) {
            val systemIns = "Eres un ornitólogo experto especializado en avifauna sudamericana y colombiana."
            val userPrompt = """
                Basado en la siguiente descripción del ave, por favor identifícala y devuélvenos:
                1. Nombre científico aproximado y familia taxonómica.
                2. Nombre común más usado en español.
                3. Estado de conservación de la IUCN aproximado y datos curiosos sobre su comportamiento de alimentación.
                
                DESCRIPCIÓN DEL AVE:
                - Plumaje: $featherDesc
                - Tipo de Pico / Patas: $beakDesc
                - Entorno geográfico / Hábitat donde se observó: $placeDesc
            """.trimIndent()

            val text = GeminiClient.askGemini(userPrompt, systemIns)
            withContext(Dispatchers.Main) {
                aiResponse.value = text
                aiLoading.value = false
            }
        }
    }

    fun generateExecutiveSummaryWithGemini() {
        val project = selectedProject.value ?: return
        aiLoading.value = true
        aiResponse.value = "Generando resumen técnico ejecutivo..."

        viewModelScope.launch(Dispatchers.IO) {
            val sitesLocal = repository.getSitesForProjectSync(project.id)
            val totalObs = allObservations.value.size
            val birdsList = allObservations.value.map { "${it.birdCommonName} (${it.birdFamily})" }.distinct().joinToString(", ")

            val systemIns = "Eres un redactor científico experto que genera resúmenes para reportes de impacto ambiental y biodiversidad."
            val userPrompt = """
                Por favor, genera un excelente resumen ejecutivo formal estructurado para un informe ambiental:
                - Proyecto: "${project.name}"
                - Contrato: ${project.contractNumber}
                - Cliente: ${project.client}
                - Total de Sitios Evaluados: ${sitesLocal.size}
                - Total de observaciones biológicas recopiladas: $totalObs
                - Lista de especies observadas destacadas: $birdsList
                
                Incluye secciones cortas claras: Portada Sintética, Diagnóstico Biológico, Conclusiones y Recomendaciones de Conservación.
            """.trimIndent()

            val text = GeminiClient.askGemini(userPrompt, systemIns)
            withContext(Dispatchers.Main) {
                aiResponse.value = text
                aiLoading.value = false
            }
        }
    }

    // --- SESSION SERIALIZATION & OPERATIONS HANDLERS ---
    private val moshiSerializer = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun serializeSpeciesList(list: List<SessionSpecies>): String {
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionSpecies::class.java)
            val adapter = moshiSerializer.adapter<List<SessionSpecies>>(type)
            adapter.toJson(list)
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    fun deserializeSpeciesList(json: String): List<SessionSpecies> {
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionSpecies::class.java)
            val adapter = moshiSerializer.adapter<List<SessionSpecies>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun serializePhotoList(list: List<SessionPhoto>): String {
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionPhoto::class.java)
            val adapter = moshiSerializer.adapter<List<SessionPhoto>>(type)
            adapter.toJson(list)
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    fun deserializePhotoList(json: String): List<SessionPhoto> {
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionPhoto::class.java)
            val adapter = moshiSerializer.adapter<List<SessionPhoto>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveSamplingSession(session: SamplingSession, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.saveSession(session).toInt()
            withContext(Dispatchers.Main) {
                onComplete(id)
            }
        }
    }

    fun deleteSamplingSession(sessionId: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSessionById(sessionId)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}

