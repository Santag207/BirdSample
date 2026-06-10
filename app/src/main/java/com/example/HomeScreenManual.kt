package com.example

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BirdArticle
import com.example.ui.BirdViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenManual(
    viewModel: BirdViewModel,
    onNavigateToMuestreos: () -> Unit
) {
    val context = LocalContext.current
    val articles by viewModel.birdArticles.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingArticles.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Search states for AllAboutBirds + Gemini edge classification
    var searchQuery by remember { mutableStateOf("") }
    var searchResponse by remember { mutableStateOf("") }
    var searchLoading by remember { mutableStateOf(false) }

    var selectedManualTab by remember { mutableStateOf("guia") } // guia, funciones, ideas

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
            .padding(horizontal = 16.dp)
    ) {
        // Hero Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "OrnithoCache Pro",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Monitoreo offline & clasificador en el borde",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Bienvenido a la plataforma líder para biólogos, guardabosques y ornitólogos. Registra campañas ecológicas en campo de forma 100% offline y sincronízalas automáticamente cuando recuperes cobertura.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onNavigateToMuestreos,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Comenzar Registro en Campo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Feature & Manual Selectors
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Manual del Operador Científico",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            TabRow(
                selectedTabIndex = when (selectedManualTab) {
                    "guia" -> 0
                    "funciones" -> 1
                    else -> 2
                },
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedManualTab == "guia",
                    onClick = { selectedManualTab = "guia" },
                    text = { Text("Guía de Uso", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedManualTab == "funciones",
                    onClick = { selectedManualTab = "funciones" },
                    text = { Text("Funciones", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedManualTab == "ideas",
                    onClick = { selectedManualTab = "ideas" },
                    text = { Text("Casos prácticos", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Dynamic guide text display based on current tab
        item {
            AnimatedContent(
                targetState = selectedManualTab,
                label = "ManualCollapseTransition"
            ) { condition ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (condition) {
                            "guia" -> {
                                Text("Paso 1: Crear el Proyecto", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Accede a la pestaña 'Muestreos', crea un proyecto indicando el contrato e impacto a evaluar.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Paso 2: Registrar Sitios Georreferenciados", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Añade puntos de muestreo georreferenciados con altitud y coordenadas GPS obtenidas del microprocesador móvil.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Paso 3: Campañas Ambientales y Plantillas", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Selecciona un método de muestreo estándar (Redes de Niebla, Conteo Directo, Estimación por Puntos) y completa los campos personalizados de cobertura forestal.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Paso 4: Ingresar Avistamientos", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Por cada ave detectada, ingresa la cantidad, comportamiento, sexo y captura evidencias (memos de audio y fotografías de plumajes).", fontSize = 12.sp, color = Color.Gray)
                            }
                            "funciones" -> {
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Caché Persistente Fuera de Red", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Todo se guarda de forma transaccional en la memoria flash de SQLite a nivel binario. Cero dependencias de red activa.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(0.3f))
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Biometría Acústica Integrada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Usa el controlador MIC para registrar vocalizaciones directamente y escucharlas offline de inmediato.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(0.3f))
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Generador de Reportes XLS & PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Genera matrices de biodiversidad oficiales listas para exportar a Excel o informes formales firmados en formato PDF.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                            "ideas" -> {
                                Text("💡 Evaluaciones de Impacto Forestal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Define proyectos de monitoreo antes y después de podas de infraestructura para medir el desplazamiento de avifauna local.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("🔍 Inventarios Comunitarios Participativos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Organiza salidas locales con agricultores veredales y documenta con registros fotográficos las especies polinizadoras endémicas.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("🦜 Seguimiento de Especies Amenazadas (IUCN)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Focaliza muestreos en el Loro Orejiamarillo o el Cóndor Andino. Sincroniza logs para validación de ministerios biológicos.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // SEARCH & DIRECT TAXONOMICAL LOOKUP IN THE EDGE WITH GEMINI & CORNELL LAB INFO
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Clasificación Científica & Datos en el Borde",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Ingresa características físicas (color, plumaje, forma) o el nombre de un ave. Buscaremos su ficha detallada de Cornell Lab (All About Birds) con ayuda de Gemini AI.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ej: Colibrí Picoespada con plumas brillantes o Aulacorhynchus", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 13.sp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                searchLoading = true
                                searchResponse = ""
                                val userPrompt = """
                                    Encuentra la taxonomía, perfil y datos basándote en la base de datos de de Cornell Lab (allaboutbirds.org) para la especie o descripción científica de: "$searchQuery". 
                                    Por favor responde de forma resumida en español bajo las siguientes secciones clave en texto limpio:
                                    - **Ficha Taxonómica**: Especie, Familia y nombre científico.
                                    - **Datos Cornell Lab**: Hábitos de alimentación, hábitat preferente y cantos notables.
                                    - **Enlace de Estudio Sugerido**: Genera un link textual al perfil de búsqueda de All About Birds para lectura directa (escribiendo el link exacto como: https://www.allaboutbirds.org/guide/Nombre_Especie de forma amigable).
                                    - **Conservación**: Rango IUCN.
                                """.trimIndent()
                                
                                scope.launch {
                                    try {
                                        val systemIns = "Eres un catalogador bio-ornitológico que sintetiza información exacta basándose en Cornell Lab of Ornithology (allaboutbirds.org)."
                                        val res = com.example.data.GeminiClient.askGemini(userPrompt, systemIns)
                                        searchResponse = res
                                    } catch (e: Exception) {
                                        searchResponse = "Hubo un problema consultando el clasificador: ${e.message}"
                                    } finally {
                                        searchLoading = false
                                    }
                                }
                            }
                        },
                        enabled = searchQuery.isNotBlank() && !searchLoading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (searchLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.YoutubeSearchedFor, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consultar Base Cornell & IA", fontSize = 12.sp)
                        }
                    }

                    if (searchResponse.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Resultado en el Borde IA:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.height(6.dp))
                                SelectionContainer {
                                    Text(
                                        searchResponse,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ALL ABOUT BIRDS NEWS FEED SECTION (CACHED OFFLINE IN ROOM)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Noticias de Conservación y Aves",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Feed de noticias oficial de allaboutbirds.org",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                IconButton(
                    onClick = { viewModel.fetchArticlesFromAllAboutBirds() },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sincronizar feed All About Birds",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Articles item rendering
        if (articles.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Presiona el botón de recarga arriba para conectar online con All About Birds y obtener noticias.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(articles) { article ->
                NewsArticleItem(article)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun NewsArticleItem(article: BirdArticle) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Suppress or fallback search
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (article.category.isNotBlank()) article.category else "Cornell Lab",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Text(
                    text = article.pubDate.take(16),
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = article.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = article.description,
                fontSize = 11.sp,
                color = Color.DarkGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Autor: " + article.creator,
                    fontSize = 9.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Leer artículo",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.ArrowOutward,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
