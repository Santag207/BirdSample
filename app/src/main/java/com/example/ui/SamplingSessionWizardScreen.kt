package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import com.example.AudioRecorderController
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplingSessionWizardScreen(
    viewModel: BirdViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Active session editing state
    var activeSession by remember { mutableStateOf<SamplingSession?>(null) }
    var currentStep by remember { mutableStateOf(1) } // 1 to 5

    // In-memory working copies for active step items to guarantee fast Compose transitions
    var template by remember { mutableStateOf("aves_estandar") }
    var projectName by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }

    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(4.7110) }
    var longitude by remember { mutableStateOf(-74.0721) }
    var altitude by remember { mutableStateOf(2600.0) }
    var ecosystem by remember { mutableStateOf("Bosque Andino") }
    
    var weatherState by remember { mutableStateOf("Parcialmente nublado") }
    var tempC by remember { mutableStateOf(18.0) }
    var humidityP by remember { mutableStateOf(70.0) }
    var methodologyName by remember { mutableStateOf("Muestreo Visual Directo") }
    var durationMin by remember { mutableStateOf(60) }
    var timeStartStr by remember { mutableStateOf("06:00") }
    var timeEndStr by remember { mutableStateOf("08:00") }
    var observerName by remember { mutableStateOf("") }
    var generalNotes by remember { mutableStateOf("") }

    // Nested structures
    var speciesList by remember { mutableStateOf<List<SessionSpecies>>(emptyList()) }
    var generalPhotosList by remember { mutableStateOf<List<SessionPhoto>>(emptyList()) }
    var activeEditingSpeciesIndex by remember { mutableStateOf<Int?>(null) }

    // Dialog flags
    var showExportResultDialog by remember { mutableStateOf(false) }
    var exportedPdfFile by remember { mutableStateOf<File?>(null) }
    var exportedZipFile by remember { mutableStateOf<File?>(null) }
    var isGeneratingExports by remember { mutableStateOf(false) }

    // Audio recorder for bird call uploads
    val audioRecorder = remember { AudioRecorderController() }
    var isRecordingAudioAi by remember { mutableStateOf(false) }
    var recordedAudioPath by remember { mutableStateOf<String?>(null) }
    var isPlayingRecordedAudio by remember { mutableStateOf(false) }

    // AI search assistant states (AllAboutBirds)
    var showAiSearchDialog by remember { mutableStateOf(false) }
    var activeSpeciesIndexForAiSearch by remember { mutableStateOf(-1) }
    var aiSearchQuery by remember { mutableStateOf("") }
    var aiSearchResults by remember { mutableStateOf<List<AiBirdMatch>>(emptyList()) }
    var isAiSearching by remember { mutableStateOf(false) }
    var aiSearchError by remember { mutableStateOf("") }

    // AI multimodal helper (Photo/Audio id) states
    var showAiIdDialog by remember { mutableStateOf(false) }
    var activeSpeciesIndexForAiId by remember { mutableStateOf(-1) }
    var isAiIdentifying by remember { mutableStateOf(false) }
    var aiIdResults by remember { mutableStateOf<List<AiBirdMatch>>(emptyList()) }
    var aiIdError by remember { mutableStateOf("") }
    var selectedPhotoPathForId by remember { mutableStateOf<String?>(null) }

    // Temporary storage photo URI for camera captures
    var currentTargetSpeciesPhotoIndex by remember { mutableStateOf(-1) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = tempPhotoFile
            if (file != null && file.exists()) {
                val absolutePath = file.absolutePath
                val photoName = file.name
                val indexToTarget = currentTargetSpeciesPhotoIndex
                
                if (indexToTarget >= 0 && indexToTarget < speciesList.size) {
                    val updatedSp = speciesList.toMutableList()
                    val targetSp = updatedSp[indexToTarget]
                    val updatedPhotos = targetSp.photos.toMutableList()
                    updatedPhotos.add(SessionPhoto(path = absolutePath, caption = "Reg: " + targetSp.name, name = photoName))
                    updatedSp[indexToTarget] = targetSp.copy(photos = updatedPhotos)
                    speciesList = updatedSp
                } else {
                    // Added as a general photo
                    val updatedGenPhotos = generalPhotosList.toMutableList()
                    updatedGenPhotos.add(SessionPhoto(path = absolutePath, caption = "Hábitat general", name = photoName))
                    generalPhotosList = updatedGenPhotos
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = viewModel.saveUriToLocalFile(context, uri)
            if (path != null) {
                val file = File(path)
                val photoName = file.name
                val indexToTarget = -1 // Gallery picks defaults to general photos. Species selection has inline handled launcher details.
                
                val updatedGenPhotos = generalPhotosList.toMutableList()
                updatedGenPhotos.add(SessionPhoto(path = path, caption = "Hábitat general", name = photoName))
                generalPhotosList = updatedGenPhotos
            }
        }
    }

    // Capture Trigger details for species-specific camera
    val speciesGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = viewModel.saveUriToLocalFile(context, uri)
            val indexToTarget = currentTargetSpeciesPhotoIndex
            if (path != null && indexToTarget >= 0 && indexToTarget < speciesList.size) {
                val file = File(path)
                val photoName = file.name
                val updatedSp = speciesList.toMutableList()
                val targetSp = updatedSp[indexToTarget]
                val updatedPhotos = targetSp.photos.toMutableList()
                updatedPhotos.add(SessionPhoto(path = path, caption = "Muestra de species", name = photoName))
                updatedSp[indexToTarget] = targetSp.copy(photos = updatedPhotos)
                speciesList = updatedSp
            }
        }
    }

    // Helper to request camera intent natively
    fun triggerCameraFor(speciesIndex: Int) {
        try {
            currentTargetSpeciesPhotoIndex = speciesIndex
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val tempFile = File(storageDir, "CAMP_${timeStamp}.jpg")
            tempPhotoFile = tempFile

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
            }
            cameraLauncher.launch(cameraIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error iniciando cámara: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Helper functions to sync view states
    fun loadSessionToFields(session: SamplingSession) {
        activeSession = session
        template = session.template
        projectName = session.projectName
        dateString = session.date
        author = session.author
        institution = session.institution

        locationName = session.location
        latitude = session.latitude
        longitude = session.longitude
        altitude = session.altitude
        ecosystem = session.ecosystem

        weatherState = session.weather
        tempC = session.temperature
        humidityP = session.humidity
        methodologyName = session.methodology
        durationMin = session.duration
        timeStartStr = session.timeStart
        timeEndStr = session.timeEnd
        observerName = session.observer
        generalNotes = session.notes

        speciesList = viewModel.deserializeSpeciesList(session.speciesJson)
        generalPhotosList = viewModel.deserializePhotoList(session.generalPhotosJson)
        currentStep = 1
    }

    fun buildSessionFromFields(existingId: Int = 0): SamplingSession {
        return SamplingSession(
            id = existingId,
            template = template,
            projectName = projectName,
            date = dateString,
            author = author,
            institution = institution,
            location = locationName,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            ecosystem = ecosystem,
            weather = weatherState,
            temperature = tempC,
            humidity = humidityP,
            methodology = methodologyName,
            duration = durationMin,
            timeStart = timeStartStr,
            timeEnd = timeEndStr,
            observer = observerName,
            notes = generalNotes,
            speciesJson = viewModel.serializeSpeciesList(speciesList),
            generalPhotosJson = viewModel.serializePhotoList(generalPhotosList),
            createdAt = activeSession?.createdAt ?: System.currentTimeMillis()
        )
    }

    fun saveDraftAndContinue(targetStep: Int) {
        if (projectName.isBlank()) {
            Toast.makeText(context, "Por favor asigne un nombre de Proyecto/Campaña en el Paso 1", Toast.LENGTH_SHORT).show()
            currentStep = 1
            return
        }
        val currentId = activeSession?.id ?: 0
        val compiled = buildSessionFromFields(currentId)
        viewModel.saveSamplingSession(compiled) { savedId ->
            activeSession = compiled.copy(id = savedId)
            currentStep = targetStep
        }
    }

    if (activeSession == null) {
        // --- HISTORY PANEL VIEW ---
        Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Registro por Sesiones de Campo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Manejo robusto fuera de línea de censos lógicos. Exporta reportes PDF y paquetes LaTeX listos.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Historial de Muestreos (${sessions.size})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterVintage,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No se han registrado sesiones aún.", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                            Text("¡Usa el botón inferior para iniciar un censo ecológico guiado paso a paso!", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(sessions) { _, session ->
                            val sps = viewModel.deserializeSpeciesList(session.speciesJson)
                            val gens = viewModel.deserializePhotoList(session.generalPhotosJson)
                            val totalPhotos = sps.sumOf { it.photos.size } + gens.size

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = session.projectName.ifBlank { "Muestreo de Campo" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        // Template Badge
                                        val badgeColor = when(session.template) {
                                            "aves_estandar" -> Color(0xFF2E7D32)
                                            "biodiversidad" -> Color(0xFFC8780A)
                                            "registro_extendido" -> Color(0xFF1565C0)
                                            else -> Color.Gray
                                        }
                                        val badgeLbl = when(session.template) {
                                            "aves_estandar" -> "Estándar"
                                            "biodiversidad" -> "Biodiver."
                                            "registro_extendido" -> "Extendido"
                                            else -> "Censo"
                                        }

                                        Surface(
                                            color = badgeColor.copy(alpha = 0.15f),
                                            contentColor = badgeColor,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.padding(start = 6.dp)
                                        ) {
                                            Text(badgeLbl, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                                    ) {
                                        Text("📅 ${session.date}", fontSize = 11.sp, color = Color.Gray)
                                        Text("📍 ${session.location.ifBlank { "Sin ubicación" }}", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🕵️ Observador: ${session.observer.ifBlank { "No registrado" }}", fontSize = 11.sp, color = Color.Gray)

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.4f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Count Badges
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
                                                Text("🐦 ${sps.size} Especies", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                            }
                                            Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
                                                Text("📷 $totalPhotos Fotos", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                            }
                                        }

                                        // Actions Bottom
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // EDIT Button
                                            IconButton(
                                                onClick = { loadSessionToFields(session) },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                            }

                                            // EXPORT Button
                                            IconButton(
                                                onClick = {
                                                    isGeneratingExports = true
                                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                        try {
                                                            val pdf = SessionReportExporter.generateSessionPdf(context, session, sps, gens)
                                                            val zip = SessionReportExporter.generateSessionZip(context, session, sps, gens)
                                                            exportedPdfFile = pdf
                                                            exportedZipFile = zip
                                                            showExportResultDialog = true
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        } finally {
                                                            isGeneratingExports = false
                                                        }
                                                    }
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = "Descargar", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(15.dp))
                                            }

                                            // DELETE Button
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteSamplingSession(session.id)
                                                    Toast.makeText(context, "Sesión eliminada", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(70.dp)) // padding for FAB
            }

            // floating trigger
            ExtendedFloatingActionButton(
                onClick = {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val lastProj = sessions.firstOrNull()?.projectName ?: ""
                    val lastAuthor = sessions.firstOrNull()?.author ?: ""
                    val lastObs = sessions.firstOrNull()?.observer ?: ""
                    val lastInst = sessions.firstOrNull()?.institution ?: ""

                    activeSession = SamplingSession(
                        projectName = if(lastProj.isNotBlank()) lastProj else "Muestreo " + SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date()),
                        date = sdf.format(Date()),
                        author = lastAuthor,
                        observer = lastObs,
                        institution = lastInst
                    )
                    template = "aves_estandar"
                    projectName = activeSession?.projectName ?: ""
                    dateString = activeSession?.date ?: ""
                    author = activeSession?.author ?: ""
                    institution = activeSession?.institution ?: ""
                    locationName = ""
                    latitude = 4.7110
                    longitude = -74.0721
                    altitude = 2600.0
                    ecosystem = "Bosque Andino"
                    weatherState = "Parcialmente nublado"
                    tempC = 18.0
                    humidityP = 70.0
                    methodologyName = "Muestreo Visual Directo"
                    durationMin = 60
                    timeStartStr = "06:00"
                    timeEndStr = "08:00"
                    observerName = lastObs
                    generalNotes = ""
                    speciesList = emptyList()
                    generalPhotosList = emptyList()
                    currentStep = 1
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nueva Sesión", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // --- STEP-BY-STEP WORKFLOW ---
        Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
            // Header Top Bar inside Wizard Screen
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            // Prompt auto-save then dismiss
                            val currentId = activeSession?.id ?: 0
                            val compiled = buildSessionFromFields(currentId)
                            viewModel.saveSamplingSession(compiled) {
                                activeSession = null
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = projectName.ifBlank { "Nueva Campaña" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Draft Autoguardado · Paso $currentStep de 5",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // High-fidelity progress circles
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (s in 1..5) {
                    val isCompleted = s < currentStep
                    val isActive = s == currentStep
                    
                    val circleBg = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isCompleted -> MaterialTheme.colorScheme.secondary
                        else -> Color.DarkGray.copy(alpha = 0.1f)
                    }
                    val circleFg = when {
                        isActive || isCompleted -> Color.White
                        else -> Color.Gray
                    }
                    val stepLbl = when(s) {
                        1 -> "Dossier"
                        2 -> "Sitio"
                        3 -> "Especies"
                        4 -> "Galería"
                        5 -> "Reporte"
                        else -> ""
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(circleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            } else {
                                Text(s.toString(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = circleFg)
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(stepLbl, fontSize = 8.5.sp, fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal, color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray)
                    }

                    if (s < 5) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .padding(horizontal = 4.dp)
                                .background(if (s < currentStep) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body content scrolling area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "StepTransition"
                ) { targetStep ->
                    when (targetStep) {
                        1 -> {
                            // STEP 1: PLANTILLA
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Text("A. Selección del Protocolo Científico", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))

                                val templatesOptions = listOf(
                                    Triple("aves_estandar", "Muestreo Estándar", "Censos visuales sencillos aptos para principiantes y biólogos locales en campo."),
                                    Triple("biodiversidad", "Monitoreo Biodiversidad de Riqueza", "Apto para el registro detallado con índices abundancia relativa y coberturas."),
                                    Triple("registro_extendido", "Censo Científico de Campo Extendido", "Recopila altísima especificidad biológica de taxonomía compleja en campo.")
                                )

                                templatesOptions.forEach { (optionId, title, desc) ->
                                    val isSelected = template == optionId
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { template = optionId },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = isSelected, onClick = { template = optionId })
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(desc, fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text("B. Metadatos de Dossier", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = projectName,
                                    onValueChange = { projectName = it },
                                    label = { Text("Nombre del Proyecto / Campaña (Ej: Reserva El Vínculo)") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = dateString,
                                    onValueChange = { dateString = it },
                                    label = { Text("Fecha de Monitoreo (AAAA-MM-DD)") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = author,
                                        onValueChange = { author = it },
                                        label = { Text("Autor Principal") },
                                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = institution,
                                        onValueChange = { institution = it },
                                        label = { Text("Institución") },
                                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                        2 -> {
                            // STEP 2: SITIO & METODOLOGIA
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Text("A. Geolocalización y Nicho Ecológico", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = locationName,
                                    onValueChange = { locationName = it },
                                    label = { Text("Nombre de la Localidad / Ubicación") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Coordenadas Satelitales (GPS)", fontWeight = FontWeight.Bold, fontSize = 10.2.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = latitude.toString(),
                                                onValueChange = { latitude = it.toDoubleOrNull() ?: latitude },
                                                label = { Text("Latitud") },
                                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                            OutlinedTextField(
                                                value = longitude.toString(),
                                                onValueChange = { longitude = it.toDoubleOrNull() ?: longitude },
                                                label = { Text("Longitud") },
                                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                            OutlinedTextField(
                                                value = altitude.toInt().toString(),
                                                onValueChange = { altitude = it.toDoubleOrNull() ?: altitude },
                                                label = { Text("Altitud (m)") },
                                                modifier = Modifier.weight(0.9f).padding(start = 4.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                viewModel.fetchCurrentGps(context) { lat, lon, alt ->
                                                    latitude = lat
                                                    longitude = lon
                                                    altitude = alt
                                                    Toast.makeText(context, "GPS prellenado de forma exitosa", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Obtener Posición Satelital GPS")
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = ecosystem,
                                    onValueChange = { ecosystem = it },
                                    label = { Text("Tipo de Ecosistema / Bioma (Ej: Humedal de Borde)") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("B. Climatología y Técnica de Conteo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = weatherState,
                                    onValueChange = { weatherState = it },
                                    label = { Text("Nubosidad / Estado del Tiempo (Despejado, Lluvioso, Niebla)") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = tempC.toString(),
                                        onValueChange = { tempC = it.toDoubleOrNull() ?: tempC },
                                        label = { Text("Temp (°C)") },
                                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = humidityP.toString(),
                                        onValueChange = { humidityP = it.toDoubleOrNull() ?: humidityP },
                                        label = { Text("Humedad (%)") },
                                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }

                                OutlinedTextField(
                                    value = methodologyName,
                                    onValueChange = { methodologyName = it },
                                    label = { Text("Metodología de Campo (Ej: Redes de Niebla, Conteo Directo)") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = durationMin.toString(),
                                        onValueChange = { durationMin = it.toIntOrNull() ?: durationMin },
                                        label = { Text("Duración (min)") },
                                        modifier = Modifier.weight(0.9f).padding(end = 4.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = timeStartStr,
                                        onValueChange = { timeStartStr = it },
                                        label = { Text("Hora Inicio") },
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                    )
                                    OutlinedTextField(
                                        value = timeEndStr,
                                        onValueChange = { timeEndStr = it },
                                        label = { Text("Hora Fin") },
                                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = observerName,
                                    onValueChange = { observerName = it },
                                    label = { Text("Observador Científico Responsable") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = generalNotes,
                                    onValueChange = { generalNotes = it },
                                    label = { Text("Notas Ambientales del Hábitat") },
                                    modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 4.dp)
                                )
                            }
                        }
                        3 -> {
                            // STEP 3: REGISTRO DE ESPECIES (SEQUENTIAL FLOW)
                            if (activeEditingSpeciesIndex == null) {
                                // SCREEN A: MENU OF ALL SAMPLED BIRDS IN THIS SESSION
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Aves Muestreadas (${speciesList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        
                                        Button(
                                            onClick = {
                                                val updated = speciesList.toMutableList()
                                                val newBird = SessionSpecies(
                                                    id = UUID.randomUUID().toString(),
                                                    family = "Passerellidae (Gorriones)",
                                                    name = "Copetón común",
                                                    commonName = "Copetón común",
                                                    count = 1,
                                                    behavior = "Forrajeando",
                                                    sexAge = "Adulto"
                                                )
                                                updated.add(newBird)
                                                speciesList = updated
                                                activeEditingSpeciesIndex = updated.size - 1
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Registrar Nueva Ave", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (speciesList.isEmpty()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                                Icon(Icons.Outlined.Eco, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("No hay aves registradas aún en este muestreo.", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text("Haz clic en \"Registrar Nueva Ave\" para registrar tu primer avistamiento.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            itemsIndexed(speciesList) { idx, sp ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "${idx + 1}. ${sp.commonName}",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                text = "${sp.name} • ${sp.family}",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                                                fontSize = 11.sp,
                                                                color = Color.Gray
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                SuggestionChip(
                                                                    onClick = {},
                                                                    label = { Text("Cant: ${sp.count}", fontSize = 10.sp) },
                                                                    modifier = Modifier.height(24.dp)
                                                                )
                                                                if (sp.behavior.isNotBlank()) {
                                                                    SuggestionChip(
                                                                        onClick = {},
                                                                        label = { Text(sp.behavior, fontSize = 10.sp, maxLines = 1) },
                                                                        modifier = Modifier.height(24.dp)
                                                                    )
                                                                }
                                                                if (sp.photos.isNotEmpty()) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CameraAlt,
                                                                        contentDescription = null,
                                                                        tint = MaterialTheme.colorScheme.secondary,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                    Text("${sp.photos.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                                                }
                                                                if (sp.audioPath != null) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Mic,
                                                                        contentDescription = null,
                                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                    Text("Canto", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                                                                }
                                                            }
                                                        }

                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            IconButton(
                                                                onClick = {
                                                                    activeEditingSpeciesIndex = idx
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Editar",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    val updated = speciesList.toMutableList()
                                                                    updated.removeAt(idx)
                                                                    speciesList = updated
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Eliminar",
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // SCREEN B: DETAILED FORM FOR EDITING / REGISTERING THE BIRD AT activeEditingSpeciesIndex
                                val idx = activeEditingSpeciesIndex!!
                                val sp = speciesList.getOrNull(idx)
                                if (sp == null) {
                                    activeEditingSpeciesIndex = null
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Completar Datos del Ejemplar #${idx + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            TextButton(
                                                onClick = { activeEditingSpeciesIndex = null }
                                            ) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Lista")
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                // Family Dropdown
                                                Text("Familia Biológica", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                                var isFamExpanded by remember { mutableStateOf(false) }
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    OutlinedButton(
                                                        onClick = { isFamExpanded = true },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(sp.family, color = Color.Black, fontSize = 13.sp)
                                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                                        }
                                                    }
                                                    DropdownMenu(
                                                        expanded = isFamExpanded,
                                                        onDismissRequest = { isFamExpanded = false }
                                                    ) {
                                                        SpeciesCatalog.families.forEach { fam ->
                                                            DropdownMenuItem(
                                                                text = { Text(fam, fontSize = 13.sp) },
                                                                onClick = {
                                                                    isFamExpanded = false
                                                                    val filteredSp = SpeciesCatalog.getSpeciesByFamily(fam)
                                                                    val defaultSp = filteredSp.firstOrNull()
                                                                    val updated = speciesList.toMutableList()
                                                                    updated[idx] = sp.copy(
                                                                        family = fam,
                                                                        name = defaultSp?.scientificName ?: "",
                                                                        commonName = defaultSp?.commonName ?: ""
                                                                    )
                                                                    speciesList = updated
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Species Selection Dropdown
                                                Text("Especie Taxón", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                                var isSpExpanded by remember { mutableStateOf(false) }
                                                val familySpeciesList = SpeciesCatalog.getSpeciesByFamily(sp.family)

                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    OutlinedButton(
                                                        onClick = { isSpExpanded = true },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("${sp.commonName} (${sp.name})", color = Color.Black, fontSize = 13.sp)
                                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                                        }
                                                    }
                                                    DropdownMenu(
                                                        expanded = isSpExpanded,
                                                        onDismissRequest = { isSpExpanded = false }
                                                    ) {
                                                        familySpeciesList.forEach { fsp ->
                                                            DropdownMenuItem(
                                                                text = { Text("${fsp.commonName} (${fsp.scientificName})", fontSize = 13.sp) },
                                                                onClick = {
                                                                    isSpExpanded = false
                                                                    val updated = speciesList.toMutableList()
                                                                    updated[idx] = sp.copy(
                                                                        name = fsp.scientificName,
                                                                        commonName = fsp.commonName
                                                                    )
                                                                    speciesList = updated
                                                                }
                                                            )
                                                        }
                                                        DropdownMenuItem(
                                                            text = { Text("✏ [Ingresar especie manualmente]", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                                            onClick = {
                                                                isSpExpanded = false
                                                                val updated = speciesList.toMutableList()
                                                                updated[idx] = sp.copy(
                                                                    name = "[Ingresar manual]",
                                                                    commonName = "Especie Manual"
                                                                )
                                                                speciesList = updated
                                                            }
                                                        )
                                                    }
                                                }

                                                if (sp.name == "[Ingresar manual]") {
                                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                        OutlinedTextField(
                                                            value = sp.commonName,
                                                            onValueChange = { txt ->
                                                                val updated = speciesList.toMutableList()
                                                                updated[idx] = sp.copy(commonName = txt)
                                                                speciesList = updated
                                                            },
                                                            label = { Text("Nombre Común") },
                                                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                                                            singleLine = true
                                                        )
                                                        OutlinedTextField(
                                                            value = sp.notes, // custom scientific name
                                                            onValueChange = { txt ->
                                                                val updated = speciesList.toMutableList()
                                                                updated[idx] = sp.copy(notes = txt)
                                                                speciesList = updated
                                                            },
                                                            label = { Text("Nombre Científico") },
                                                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                                                            singleLine = true
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                    OutlinedTextField(
                                                        value = sp.count.toString(),
                                                        onValueChange = { txt ->
                                                            val updated = speciesList.toMutableList()
                                                            updated[idx] = sp.copy(count = txt.toIntOrNull() ?: sp.count)
                                                            speciesList = updated
                                                        },
                                                        label = { Text("Individuos") },
                                                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                    )
                                                    OutlinedTextField(
                                                        value = sp.behavior,
                                                        onValueChange = { txt ->
                                                            val updated = speciesList.toMutableList()
                                                            updated[idx] = sp.copy(behavior = txt)
                                                            speciesList = updated
                                                        },
                                                        label = { Text("Comportamiento") },
                                                        modifier = Modifier.weight(1.5f).padding(horizontal = 4.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = sp.sexAge,
                                                        onValueChange = { txt ->
                                                            val updated = speciesList.toMutableList()
                                                            updated[idx] = sp.copy(sexAge = txt)
                                                            speciesList = updated
                                                        },
                                                        label = { Text("Sexo / Edad") },
                                                        modifier = Modifier.weight(1.5f).padding(start = 4.dp)
                                                    )
                                                }

                                                if (sp.name != "[Ingresar manual]") {
                                                    OutlinedTextField(
                                                        value = sp.notes,
                                                        onValueChange = { txt ->
                                                            val updated = speciesList.toMutableList()
                                                            updated[idx] = sp.copy(notes = txt)
                                                            speciesList = updated
                                                        },
                                                        label = { Text("Notas taxonómicas / del avistamiento") },
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                                    )
                                                }

                                                // --- Species photos gallery ---
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text("Fotografías de Muestra", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                                
                                                Row(
                                                    modifier = Modifier.padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { triggerCameraFor(idx) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Capturar", fontSize = 11.sp)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            currentTargetSpeciesPhotoIndex = idx
                                                            speciesGalleryLauncher.launch("image/*")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Galería", fontSize = 11.sp)
                                                    }
                                                }

                                                // Render attached thumbnails
                                                if (sp.photos.isNotEmpty()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState())
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        sp.photos.forEachIndexed { pIdx, photo: SessionPhoto ->
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                                                Box(modifier = Modifier.size(60.dp)) {
                                                                    AsyncImage(
                                                                        model = photo.path,
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                                                                    )
                                                                    // Remove photo
                                                                    IconButton(
                                                                        onClick = {
                                                                            val updatedSp = speciesList.toMutableList()
                                                                            val modSp = updatedSp[idx]
                                                                            val modPhotos = modSp.photos.toMutableList()
                                                                            modPhotos.removeAt(pIdx)
                                                                            updatedSp[idx] = modSp.copy(photos = modPhotos)
                                                                            speciesList = updatedSp
                                                                        },
                                                                        modifier = Modifier
                                                                            .size(18.dp)
                                                                            .align(Alignment.TopEnd)
                                                                            .background(Color.White, CircleShape)
                                                                    ) {
                                                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(11.dp))
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                
                                                                // Editable Caption Text directly below
                                                                BasicTextField(
                                                                    value = photo.caption,
                                                                    onValueChange = { cap ->
                                                                        val updatedSp = speciesList.toMutableList()
                                                                        val modSp = updatedSp[idx]
                                                                        val modPhotos = modSp.photos.toMutableList()
                                                                        modPhotos[pIdx] = photo.copy(caption = cap)
                                                                        updatedSp[idx] = modSp.copy(photos = modPhotos)
                                                                        speciesList = updatedSp
                                                                    },
                                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 8.5.sp, color = Color.DarkGray, fontStyle = FontStyle.Italic),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                        .padding(2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // --- Gemini AI Assistances ---
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.AutoAwesome,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Asistente de Campo IA",
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        Text(
                                                            text = "Sugerencias y reconocimiento biológico de avifauna basado en All About Birds y Gemini AI.",
                                                            fontSize = 9.5.sp,
                                                            color = Color.DarkGray,
                                                            modifier = Modifier.padding(vertical = 4.dp)
                                                        )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            // Suggestions Search Trigger
                                                            OutlinedButton(
                                                                onClick = {
                                                                    activeSpeciesIndexForAiSearch = idx
                                                                    aiSearchQuery = if (sp.name == "[Ingresar manual]") sp.commonName else sp.name
                                                                    aiSearchResults = emptyList()
                                                                    aiSearchError = ""
                                                                    showAiSearchDialog = true
                                                                },
                                                                modifier = Modifier.weight(1f).height(32.dp),
                                                                shape = RoundedCornerShape(6.dp),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                                            ) {
                                                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Sugerir Nombre", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                            }

                                                            // Multimodal Identification Trigger
                                                            Button(
                                                                onClick = {
                                                                    activeSpeciesIndexForAiId = idx
                                                                    aiIdResults = emptyList()
                                                                    aiIdError = ""
                                                                    recordedAudioPath = null
                                                                    selectedPhotoPathForId = sp.photos.firstOrNull()?.path
                                                                    showAiIdDialog = true
                                                                },
                                                                modifier = Modifier.weight(1.2f).height(32.dp),
                                                                shape = RoundedCornerShape(6.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Reconocer Foto/Audio", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                Button(
                                                    onClick = { activeEditingSpeciesIndex = null },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Guardar y Volver a la Lista")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
                            // STEP 4: FOTOS GENERALES
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Text("A. Evicencias del Entorno Biológico", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Captura o importa imágenes generales del dosel, sotobosque, cobertura o hábitat circundante.", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { triggerCameraFor(-1) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tomar Foto")
                                    }

                                    Button(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Importar")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Fotografías Adjuntadas (${generalPhotosList.size})", fontWeight = FontWeight.Bold, fontSize = 11.2.sp)

                                if (generalPhotosList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(140.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No se han adjuntado fotos generales de contexto.", color = Color.Gray, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                                    }
                                } else {
                                    // Grid of general photos
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        generalPhotosList.forEachIndexed { pIdx, photo ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model = photo.path,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(65.dp).clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Foto de Paisaje / Hábitat #${pIdx + 1}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        OutlinedTextField(
                                                            value = photo.caption,
                                                            onValueChange = { txt ->
                                                                val updated = generalPhotosList.toMutableList()
                                                                updated[pIdx] = photo.copy(caption = txt)
                                                                generalPhotosList = updated
                                                            },
                                                            placeholder = { Text("Subtítulo de campo para reporte PDF", fontSize = 10.sp) },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.5.sp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val updated = generalPhotosList.toMutableList()
                                                            updated.removeAt(pIdx)
                                                            generalPhotosList = updated
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        5 -> {
                            // STEP 5: PREVISUALIZACION Y EXPORTACION
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Text("A. Compilación del Reporte Científico", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Resumen Dossier para Exportación", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val checks = listOf(
                                            "Protocolo Activo" to when(template) {
                                                "aves_estandar" -> "Estándar"
                                                "biodiversidad" -> "Biodiversidad"
                                                else -> "Censo Extendido"
                                            },
                                            "Campaña Proyecto:" to projectName,
                                            "Lote Investigador:" to author,
                                            "Geolocalización:" to locationName,
                                            "Registros Especies:" to "${speciesList.size} taxones",
                                            "Total Individuos:" to "${speciesList.sumOf { it.count }} ejemplares",
                                            "Adjuntos Visuales:" to "${speciesList.sumOf { it.photos.size } + generalPhotosList.size} fotos"
                                        )

                                        checks.forEach { (lbl, valStr) ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(lbl, fontSize = 11.sp, color = Color.DarkGray)
                                                Text(valStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (isGeneratingExports) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Generando PDF, fuentes LaTeX y consolidando compresión general...", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isGeneratingExports = true
                                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val compiled = buildSessionFromFields(activeSession?.id ?: 0)
                                                    viewModel.saveSamplingSession(compiled)
                                                    
                                                    val pdf = SessionReportExporter.generateSessionPdf(context, compiled, speciesList, generalPhotosList)
                                                    val zip = SessionReportExporter.generateSessionZip(context, compiled, speciesList, generalPhotosList)
                                                    
                                                    exportedPdfFile = pdf
                                                    exportedZipFile = zip
                                                    showExportResultDialog = true
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                } finally {
                                                    isGeneratingExports = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generar Documentación Pro (PDF+ZIP)", fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Generará un informe impreso PDF estructurado de gran calidad, fuentes completas de LaTeX .tex compilables para reportes y carpetas estructuradas de fotos.", fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer actions navbar (Back / Next)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStep > 1) {
                            saveDraftAndContinue(currentStep - 1)
                        }
                    },
                    enabled = currentStep > 1,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Atrás")
                }

                Button(
                    onClick = {
                        if (currentStep < 5) {
                            saveDraftAndContinue(currentStep + 1)
                        } else {
                            // Final save draft, exit edit mode and navigate back to list
                            val currentId = activeSession?.id ?: 0
                            val compiled = buildSessionFromFields(currentId)
                            viewModel.saveSamplingSession(compiled) {
                                activeSession = null
                                Toast.makeText(context, "Muestreo finalizado y guardado con éxito", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text(if (currentStep == 5) "Finalizar" else "Continuar")
                    Spacer(modifier = Modifier.width(4.dp))
                    if (currentStep < 5) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
        }
    }

    // --- REPORT EXPORT SHARING MODAL DIALOG ---
    if (showExportResultDialog) {
        val pdf = exportedPdfFile
        val zip = exportedZipFile
        
        Dialog(onDismissRequest = { showExportResultDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("¡Exportación Exitosa!", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Los reportes se han generado sin conexión en la caché de la aplicación de manera segura.", textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    if (pdf != null && pdf.exists()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Informe Técnico PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Proyectado para imprenta", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                            Button(
                                onClick = {
                                    try {
                                        val authority = "${context.packageName}.fileprovider"
                                        val uri = FileProvider.getUriForFile(context, authority, pdf)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Ver PDF"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No hay lector PDF instalado. Ruta: ${pdf.absolutePath}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Compartir", fontSize = 10.sp)
                            }
                        }
                    }

                    if (zip != null && zip.exists()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color(0xFFC8780A), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Paquete Completo LATEX", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("ZIP con .tex, JSON, imágenes", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                            Button(
                                onClick = {
                                    try {
                                        val authority = "${context.packageName}.fileprovider"
                                        val uri = FileProvider.getUriForFile(context, authority, zip)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Enviar ZIP"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error abriendo compartir: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Enviar", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showExportResultDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    // --- DIÁLOGO DE BÚSQUEDA INTELIGENTE DE ESPECIES ---
    if (showAiSearchDialog) {
        Dialog(onDismissRequest = { showAiSearchDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Búsqueda IA (AllAboutBirds)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { showAiSearchDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Text(
                        "Deduce taxonomía científica basada en rasgos morfológicos, cantos o comportamientos usando la biblioteca experta de Cornell Lab a través de Gemini AI.",
                        fontSize = 10.5.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = aiSearchQuery,
                        onValueChange = { aiSearchQuery = it },
                        label = { Text("Describir hallazgo (p. ej., 'pájaro amarillo con cabeza negra')") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isAiSearching = true
                            aiSearchError = ""
                            aiSearchResults = emptyList()
                            coroutineScope.launch {
                                try {
                                    val prompt = """
                                        El usuario está buscando un ave en campo con la siguiente descripción de rasgos físicos, comportamiento o hábitat:
                                        "$aiSearchQuery"
                                        
                                        Usa el conocimiento y los recursos oficiales de Cornell Lab (www.allaboutbirds.org) para encontrar las 3 especies que coinciden mejor.
                                        
                                        Formato de salida estricto: Devuelve exactamente 3 líneas (una por cada especie sugerida). Cada línea debe tener la información separada exactamente por la barra vertical pipe (|) con los siguientes campos:
                                        Nombre Común en Español|Nombre Científico (Género especie)|Familia Biológica|Razón breve del hallazgo según los recursos de Cornell de All About Birds.
                                        
                                        Ejemplo de salida de 3 líneas:
                                        Colibrí rutilante|Colibri coruscans|Trochilidae (Colibríes)|Su garganta azul-violeta brillante y comportamiento territorial descritos por All About Birds.
                                        Pinnchón real|Diglossa albilatera|Thraupidae (Tángaras)|El pico modificado en forma de gancho para perforar flores registrado en Cornell.
                                        Copetón común|Zonotrichia capensis|Passerellidae (Gorriones)|Pájaro pequeño con copete gris y marcas faciales negras características según guías de All About Birds.
                                        
                                        Por favor, no incluyas ningún texto de bienvenida, ni bloques de código de markdown con tildes invertidas, ni explicaciones adicionales. Solo las 3 líneas con el pipe (|).
                                    """.trimIndent()
                                    
                                    val systemInstruction = "Eres un ornitólogo experto del Cornell Lab of Ornithology encargado de identificar avifauna en campo utilizando como referencia www.allaboutbirds.org."
                                    val responseText = GeminiClient.askGemini(prompt, systemInstruction)
                                    
                                    val matches = mutableListOf<AiBirdMatch>()
                                    if (responseText.contains("|")) {
                                        val lines = responseText.split("\n")
                                        lines.forEach { line ->
                                            if (line.isNotBlank() && line.contains("|")) {
                                                val parts = line.split("|")
                                                if (parts.size >= 4) {
                                                    matches.add(
                                                        AiBirdMatch(
                                                            commonName = parts[0].trim(),
                                                            scientificName = parts[1].trim(),
                                                            family = parts[2].trim(),
                                                            reason = parts[3].trim()
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (matches.isEmpty()) {
                                        aiSearchError = "No se estructuraron correctamente los resultados de la IA. Respuesta cruda:\n$responseText"
                                    } else {
                                        aiSearchResults = matches
                                    }
                                } catch (e: Exception) {
                                    aiSearchError = "Error en la consulta: ${e.message}"
                                } finally {
                                    isAiSearching = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAiSearching && aiSearchQuery.isNotBlank()
                    ) {
                        if (isAiSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscando en la nube con IA...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consultar All About Birds + Gemini")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (aiSearchError.isNotBlank()) {
                        Text(aiSearchError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(aiSearchResults) { bIdx, match ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(match.commonName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("${match.scientificName} • ${match.family}", fontStyle = FontStyle.Italic, fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Button(
                                            onClick = {
                                                val index = activeSpeciesIndexForAiSearch
                                                if (index >= 0 && index < speciesList.size) {
                                                    val updated = speciesList.toMutableList()
                                                    updated[index] = updated[index].copy(
                                                        family = match.family,
                                                        name = match.scientificName,
                                                        commonName = "${match.commonName} (IA)",
                                                        notes = "Coincidencia: ${match.reason} (IA)\n" + updated[index].notes
                                                    )
                                                    speciesList = updated
                                                }
                                                showAiSearchDialog = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Aplicar", fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("¿Por qué coincide?: ${match.reason}", fontSize = 10.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO DE RECONOCIMIENTO MULTIMODAL IA (FOTO/AUDIO) ---
    if (showAiIdDialog) {
        val activeSp = if (activeSpeciesIndexForAiId >= 0 && activeSpeciesIndexForAiId < speciesList.size) {
            speciesList[activeSpeciesIndexForAiId]
        } else null

        Dialog(onDismissRequest = {
            if (isRecordingAudioAi) {
                audioRecorder.stopRecording()
            }
            showAiIdDialog = false
        }) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reconocedor Multimodal IA", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = {
                            if (isRecordingAudioAi) {
                                audioRecorder.stopRecording()
                            }
                            showAiIdDialog = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Text(
                        "Combina imágenes capturadas con grabaciones de audio en tiempo real para predecir la especie de ave exacta utilizando el clasificador de Gemini AI.",
                        fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Seccion 1: Imagenes de la especie
                    Text("1. Seleccionar Foto para Análisis", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    if (activeSp != null && activeSp.photos.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeSp.photos.forEach { p ->
                                val isSelected = selectedPhotoPathForId == p.path
                                Box(
                                    modifier = Modifier
                                        .size(65.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedPhotoPathForId = p.path }
                                ) {
                                    AsyncImage(
                                        model = p.path,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("Foto activa seleccionada.", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("• Toca para cambiar.", fontSize = 9.5.sp, color = Color.Gray)
                        }
                    } else {
                        Text("No has adjuntado fotos de campo a esta especie aún. Puedes continuar solo con notas o grabar audio.", fontSize = 10.sp, color = Color.Red.copy(alpha = 0.7f), fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Seccion 2: Grabador de audio
                    Text("2. Capturar Canto de Campo (Audio)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isRecordingAudioAi) {
                                    Button(
                                        onClick = {
                                            val ok = audioRecorder.startRecording(context)
                                            if (ok) {
                                                isRecordingAudioAi = true
                                                recordedAudioPath = null
                                            } else {
                                                Toast.makeText(context, "Grabando audio simulado en caché.", Toast.LENGTH_SHORT).show()
                                                isRecordingAudioAi = true
                                                recordedAudioPath = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Grabar Canto", fontSize = 11.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            val savedPath = audioRecorder.stopRecording()
                                            recordedAudioPath = savedPath ?: (context.cacheDir.absolutePath + "/BIRD_CALL_MOCK.3gp")
                                            isRecordingAudioAi = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Red)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Detener", fontSize = 11.sp)
                                    }
                                }

                                if (recordedAudioPath != null) {
                                    Button(
                                        onClick = {
                                            isPlayingRecordedAudio = true
                                            audioRecorder.playAudio(recordedAudioPath!!) {
                                                isPlayingRecordedAudio = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        enabled = !isPlayingRecordedAudio
                                    ) {
                                        Icon(Icons.Default.Hearing, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isPlayingRecordedAudio) "Reproduciendo..." else "Escuchar", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            if (isRecordingAudioAi) {
                                Text("🎙️ Grabando audio de canto en tiempo real...", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            } else if (recordedAudioPath != null) {
                                Text("✅ Canto de ave guardado en caché.", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                            } else {
                                Text("Sin espectrograma de audio cargado.", fontSize = 9.5.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Seccion 3: Analizar
                    Button(
                        onClick = {
                            isAiIdentifying = true
                            aiIdError = ""
                            aiIdResults = emptyList()
                            coroutineScope.launch {
                                try {
                                    val prompt = """
                                        Identifica la especie de este ejemplar de ave observada. 
                                        Estudia los parámetros proporcionados de manera integral comparando patrones científicos contra los registros taxonómicos de Cornell Lab (www.allaboutbirds.org).
                                        
                                        Información del contexto de observación:
                                        - Ecosistema: $ecosystem
                                        - Notas descriptivas adicionales ingresadas: ${activeSp?.notes ?: "Ningunas"}
                                        
                                        Formato de salida estricto: Devuelve exactamente 3 líneas (una por cada especie sugerida ordenada por probabilidad). Cada línea debe tener la información separada exactamente por la barra vertical pipe (|) con los siguientes campos:
                                        Nombre Común en Español|Nombre Científico (Género especie)|Familia Biológica|Razón científica del diagnóstico de All About Birds.
                                        
                                        Por favor, no incluyas ningún texto introductorio, ni bloques markdown. Solo las 3 líneas con pipe (|). Si hay imagen, analiza el plumaje y pico. Si hay audio, analiza el tempo y estilo rítmico.
                                    """.trimIndent()

                                    val inlineDatas = mutableListOf<GeminiInlineData>()
                                    selectedPhotoPathForId?.let { path ->
                                        extractBase64FromImagePath(path)?.let { b64 ->
                                            inlineDatas.add(GeminiInlineData(mimeType = "image/jpeg", data = b64))
                                        }
                                    }
                                    recordedAudioPath?.let { path ->
                                        extractBase64FromAudioPath(path)?.let { b64 ->
                                            inlineDatas.add(GeminiInlineData(mimeType = "audio/3gpp", data = b64))
                                        }
                                    }

                                    val systemInstruction = "Eres un ornitólogo de campo de élite encargado de clasificar especímenes utilizando fotos de muestras o grabaciones de llamadas de audio de Cornell o hábitats locales."
                                    val responseText = GeminiClient.askGeminiMultimodal(prompt, systemInstruction, inlineDatas)

                                    val matches = mutableListOf<AiBirdMatch>()
                                    if (responseText.contains("|")) {
                                        val lines = responseText.split("\n")
                                        lines.forEach { line ->
                                            if (line.isNotBlank() && line.contains("|")) {
                                                val parts = line.split("|")
                                                if (parts.size >= 4) {
                                                    matches.add(
                                                        AiBirdMatch(
                                                            commonName = parts[0].trim(),
                                                            scientificName = parts[1].trim(),
                                                            family = parts[2].trim(),
                                                            reason = parts[3].trim()
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (matches.isEmpty()) {
                                        aiIdError = "No se pudieron estructurar los resultados de reconocimiento IA. Respuesta cruda:\n$responseText"
                                    } else {
                                        aiIdResults = matches
                                    }
                                } catch (e: Exception) {
                                    aiIdError = "Error al clasificar: ${e.message}"
                                } finally {
                                    isAiIdentifying = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAiIdentifying && (selectedPhotoPathForId != null || recordedAudioPath != null || (activeSp?.notes?.isNotBlank() == true))
                    ) {
                        if (isAiIdentifying) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clasificando...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clasificar Ejemplar con Gemini")
                        }
                    }

                    if (aiIdError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(aiIdError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    aiIdResults.forEachIndexed { bIdx, match ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(match.commonName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text("${match.scientificName} • ${match.family}", fontStyle = FontStyle.Italic, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Button(
                                        onClick = {
                                            val index = activeSpeciesIndexForAiId
                                            if (index >= 0 && index < speciesList.size) {
                                                val updated = speciesList.toMutableList()
                                                updated[index] = updated[index].copy(
                                                    family = match.family,
                                                    name = match.scientificName,
                                                    commonName = "${match.commonName} (IA)",
                                                    notes = "Identificación: ${match.reason} (IA)\n" + updated[index].notes,
                                                    audioPath = recordedAudioPath ?: updated[index].audioPath
                                                )
                                                speciesList = updated
                                            }
                                            showAiIdDialog = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Seleccionar", fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Evidencias del hallazgo: ${match.reason}", fontSize = 10.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DATA CLASSES Y UTILIDADES AUXILIARES ---
data class AiBirdMatch(
    val commonName: String,
    val scientificName: String,
    val family: String,
    val reason: String
)

fun extractBase64FromImagePath(path: String): String? {
    return try {
        val file = File(path)
        if (!file.exists()) return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun extractBase64FromAudioPath(path: String): String? {
    return try {
        val file = File(path)
        if (!file.exists()) return null
        val bytes = file.readBytes()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

