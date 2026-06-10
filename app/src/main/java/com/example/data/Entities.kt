package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val client: String,
    val contractNumber: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "Borrador" // Borrador, En Progreso, Sincronizado
)

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val name: String,
    val department: String,
    val municipality: String,
    val vereda: String,
    val ecosystem: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Double = 0.0
)

@Entity(tableName = "samplings")
data class Sampling(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val siteId: Int,
    val observer: String,
    val date: Long = System.currentTimeMillis(),
    val weather: String, // Soleado, Nublado, Lluvioso, Viento
    val temperature: Double,
    val humidity: Double,
    val methodology: String, // Redes de niebla, Observación directa, Estimación por puntos
    val customFieldsJson: String = "{}" // Dynamic key-values matching custom template fields
)

@Entity(tableName = "observations")
data class Observation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val samplingId: Int,
    val birdScientificName: String,
    val birdCommonName: String,
    val birdFamily: String,
    val quantity: Int,
    val sex: String, // Macho, Hembra, Desconocido
    val age: String, // Adulto, Juvenil, Desconocido
    val behavior: String, // Volando, Cantando, Alimentándose, Posado
    val photoPath: String? = null,
    val audioPath: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "form_templates")
data class FormTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val fieldsJson: String // Serialized array of CustomField definers
)

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val status: String, // EXITOSO, FALLIDO
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bird_articles")
data class BirdArticle(
    @PrimaryKey val link: String,
    val title: String,
    val description: String,
    val pubDate: String,
    val creator: String = "",
    val category: String = ""
)

@Entity(tableName = "sampling_sessions")
data class SamplingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val template: String = "aves_estandar",
    val projectName: String = "",
    val date: String = "",
    val author: String = "",
    val institution: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val ecosystem: String = "",
    val weather: String = "",
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    val methodology: String = "",
    val duration: Int = 0,
    val timeStart: String = "",
    val timeEnd: String = "",
    val observer: String = "",
    val notes: String = "",
    val speciesJson: String = "[]", // Serialized List<SessionSpecies>
    val generalPhotosJson: String = "[]", // Serialized List<SessionPhoto>
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

data class SessionSpecies(
    val id: String,
    val family: String,
    val name: String,
    val commonName: String = "",
    val count: Int = 1,
    val behavior: String = "",
    val sexAge: String = "",
    val notes: String = "",
    val photos: List<SessionPhoto> = emptyList(),
    val audioPath: String? = null
)

data class SessionPhoto(
    val path: String, // local filesystem path (or base64 if needed, but file path is safer for storage volume)
    val caption: String = "",
    val name: String = ""
)
