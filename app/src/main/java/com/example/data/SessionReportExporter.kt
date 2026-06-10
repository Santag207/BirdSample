package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SessionReportExporter {

    fun generateSessionPdf(
        context: Context,
        session: SamplingSession,
        speciesList: List<SessionSpecies>,
        generalPhotos: List<SessionPhoto>
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points

        val cDarkGreen = Color.parseColor("#1B5E20")  // Forest green
        val cMediumGreen = Color.parseColor("#2E7D32")
        val cPrimaryLight = Color.parseColor("#E8F5E9") // Tint back
        val cAmber = Color.parseColor("#C8780A")

        val paintText = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isAntiAlias = true
        }
        val paintTitle = Paint().apply {
            color = cDarkGreen
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintHeading = Paint().apply {
            color = cMediumGreen
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintBold = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintBox = Paint().apply {
            color = cPrimaryLight
            style = Paint.Style.FILL
        }
        val paintStroke = Paint().apply {
            color = Color.parseColor("#C8E6C9")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        // --- PAGE 1: COVER ---
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Decorative side stripe
        canvas.drawRect(0f, 0f, 25f, pageHeight.toFloat(), Paint().apply { color = cDarkGreen })

        var y = 140f
        canvas.drawText("PLATAFORMA ORNITHO_CACHE PRO", 55f, y, paintBold.apply {
            textSize = 12f
            color = Color.GRAY
        })
        y += 35f
        
        // Split title lines
        val rawProjName = if (session.projectName.isBlank()) "Proyecto de Campo" else session.projectName
        canvas.drawText(rawProjName, 55f, y, paintTitle)
        
        y += 30f
        canvas.drawText("Informe de Monitoreo Biológico de Avifauna", 55f, y, paintText.apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        })

        y += 60f
        // Metadata Box
        canvas.drawRect(55f, y, pageWidth.toFloat() - 35f, y + 130f, paintBox)
        canvas.drawRect(55f, y, pageWidth.toFloat() - 35f, y + 130f, paintStroke)

        val metadataList = listOf(
            "Metodología:" to when(session.template) {
                "waves_estandar" -> "Muestreo Estándar de Avifauna"
                "biodiversidad" -> "Evaluación de Biodiversidad y Riqueza"
                "registro_extendido" -> "Registro Técnico Científico Extendido"
                else -> "Muestreo Estándar"
            },
            "Investigador(es):" to if (session.author.isBlank()) "No especificado" else session.author,
            "Institución:" to if (session.institution.isBlank()) "No especificada" else session.institution,
            "Localización:" to if (session.location.isBlank()) "No registrada" else session.location,
            "Campaña Fecha:" to session.date
        )

        var metaY = y + 25f
        metadataList.forEach { (label, valStr) ->
            canvas.drawText(label, 70f, metaY, paintBold.apply { textSize = 10.5f; color = cDarkGreen })
            canvas.drawText(valStr, 190f, metaY, paintText.apply { textSize = 10.5f; color = Color.BLACK; typeface = Typeface.DEFAULT })
            metaY += 21f
        }

        y += 180f
        canvas.drawText("Estadísticas Generales", 55f, y, paintHeading.apply { textSize = 14f })
        y += 25f

        val totalInd = speciesList.sumOf { it.count }
        val statsList = listOf(
            "Especies Registradas: ${speciesList.size}",
            "Individuos Visualizados: $totalInd",
            "Muestreo Coordenadas: ${session.latitude}, ${session.longitude}",
            "Altitud del Terreno: ${session.altitude} m.s.n.m.",
            "Ecosistema Hábitat: ${session.ecosystem}",
            "Fotografías Anexas: ${speciesList.sumOf { it.photos.size } + generalPhotos.size} fotos"
        )

        statsList.forEach { valStr ->
            canvas.drawText("• $valStr", 65f, y, paintText.apply { textSize = 10.5f; color = Color.DKGRAY })
            y += 18f
        }

        // Draw footer page cover
        canvas.drawText("Página 1", pageWidth / 2f, pageHeight - 35f, paintText.apply { textSize = 9f })
        pdfDocument.finishPage(page)

        // --- PAGE 2: CONDICIONES, METODOLOGÍA & TABLA DE REGISTROS ---
        pageNumber = 2
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        canvas.drawRect(0f, 0f, 25f, pageHeight.toFloat(), Paint().apply { color = cDarkGreen })

        y = 60f
        canvas.drawText("1. Metodología e Información Ambiental", 55f, y, paintHeading)
        y += 20f

        val surveyRows = listOf(
            "Clima / Estado:" to session.weather,
            "Temperatura Ambiente:" to "${session.temperature} °C",
            "Humedad Relativa:" to "${session.humidity} %",
            "Metodología Aplicada:" to session.methodology,
            "Duración Campaña:" to "${session.duration} minutos",
            "Horario:" to "${session.timeStart} – ${session.timeEnd}",
            "Observador Responsable:" to session.observer
        )

        surveyRows.forEach { (lbl, valStr) ->
            canvas.drawText(lbl, 65f, y, paintBold.apply { textSize = 10f; color = Color.BLACK })
            canvas.drawText(valStr, 220f, y, paintText.apply { textSize = 10f })
            canvas.drawLine(55f, y + 4f, pageWidth.toFloat() - 35f, y + 4f, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
            y += 22f
        }

        y += 20f
        canvas.drawText("2. Tabla Sistemática de Avistamiento", 55f, y, paintHeading)
        y += 25f

        // Table Header
        canvas.drawRect(55f, y - 15f, pageWidth.toFloat() - 35f, y + 5f, paintBox)
        canvas.drawText("Nombre / Especie", 60f, y - 3f, paintBold.apply { textSize = 9f; color = cDarkGreen })
        canvas.drawText("Familia", 240f, y - 3f, paintBold)
        canvas.drawText("N", 370f, y - 3f, paintBold)
        canvas.drawText("Comportamiento", 400f, y - 3f, paintBold)
        canvas.drawText("Sexo/Edad", 490f, y - 3f, paintBold)

        y += 18f
        if (speciesList.isEmpty()) {
            canvas.drawText("Sin especies observadas registradas en esta campaña.", 70f, y, paintText.apply { textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) })
        } else {
            speciesList.take(15).forEach { sp ->
                val shortName = if (sp.name.length > 25) sp.name.take(22) + "..." else sp.name
                val shortFam = if (sp.family.length > 22) sp.family.take(19) + "..." else sp.family
                val shortBehavior = if (sp.behavior.length > 16) sp.behavior.take(14) + "..." else sp.behavior
                val shortSex = if (sp.sexAge.length > 16) sp.sexAge.take(14) + "..." else sp.sexAge

                canvas.drawText(shortName, 60f, y, paintText.apply { textSize = 8.5f; color = Color.BLACK; typeface = Typeface.DEFAULT })
                canvas.drawText(shortFam, 240f, y, paintText)
                canvas.drawText(sp.count.toString(), 370f, y, paintText)
                canvas.drawText(shortBehavior, 400f, y, paintText)
                canvas.drawText(shortSex, 490f, y, paintText)

                canvas.drawLine(55f, y + 4f, pageWidth.toFloat() - 35f, y + 4f, Paint().apply { color = Color.parseColor("#F0F0F0"); strokeWidth = 0.5f })
                y += 18f
            }
        }

        canvas.drawText("Página 2", pageWidth / 2f, pageHeight - 35f, paintText.apply { textSize = 9f })
        pdfDocument.finishPage(page)

        // --- PAGE 3+: FOTOGRAFÍAS DE ESPECIES Y ANEXOS ---
        val allPhotosToRender = mutableListOf<Pair<String, String>>() // Path to Caption
        speciesList.forEach { sp ->
            sp.photos.forEach { ph ->
                allPhotosToRender.add(ph.path to "Especie: ${sp.name} — ${ph.caption}")
            }
        }
        generalPhotos.forEach { ph ->
            allPhotosToRender.add(ph.path to "Foto de Contexto: ${ph.caption}")
        }

        if (allPhotosToRender.isNotEmpty()) {
            var photoIdx = 0
            while (photoIdx < allPhotosToRender.size) {
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                canvas.drawRect(0f, 0f, 25f, pageHeight.toFloat(), Paint().apply { color = cDarkGreen })

                y = 60f
                canvas.drawText("Anexo Fotográfico de Campo (Pág. ${pageNumber - 2})", 55f, y, paintHeading)
                y += 30f

                // Render up to 2 photos per page to keep high clarity and format
                for (pInPage in 0 until 2) {
                    if (photoIdx >= allPhotosToRender.size) break
                    val (path, caption) = allPhotosToRender[photoIdx]
                    
                    try {
                        val file = File(path)
                        if (file.exists()) {
                            val originalBitmap = BitmapFactory.decodeFile(path)
                            if (originalBitmap != null) {
                                // Scale bitmap proportionally to fit 180dp height
                                val maxW = pageWidth - 100f
                                val maxH = 260f
                                var scale = maxW / originalBitmap.width
                                if (originalBitmap.height * scale > maxH) {
                                    scale = maxH / originalBitmap.height
                                }
                                val dW = (originalBitmap.width * scale).toInt()
                                val dH = (originalBitmap.height * scale).toInt()
                                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, dW, dH, true)

                                val drawX = 55f + ((pageWidth - 90f) - dW) / 2f
                                canvas.drawBitmap(scaledBitmap, drawX, y, null)
                                
                                y += dH + 15f
                                // Caption Text wrapped
                                canvas.drawText(caption, 55f, y, paintText.apply {
                                    textSize = 9.5f
                                    color = Color.BLACK
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                                })
                                y += 30f
                            }
                        }
                    } catch (e: Exception) {
                        canvas.drawText("[Error cargando imagen: ${e.message}]", 65f, y, paintText.apply { color = Color.RED })
                        y += 40f
                    }
                    photoIdx++
                }

                canvas.drawText("Página $pageNumber", pageWidth / 2f, pageHeight - 35f, paintText.apply { textSize = 9f })
                pdfDocument.finishPage(page)
            }
        }

        // Write outputs
        val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outputFile = File(storageDir, "reporte_sesion_${session.id}.pdf")
        try {
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
        return outputFile
    }

    fun generateSessionLatex(
        session: SamplingSession,
        speciesList: List<SessionSpecies>,
        generalPhotos: List<SessionPhoto>
    ): String {
        val dateObj = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(session.date) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val dateStr = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "CO")).format(dateObj)

        val coords = "${session.latitude}, ${session.longitude}"
        val tplLabel = when (session.template) {
            "aves_estandar" -> "Muestreo estándar de avifauna"
            "biodiversidad" -> "Monitoreo de biodiversidad molecular y riqueza"
            "registro_extendido" -> "Registro de campo científico extendido"
            else -> "Muestreo estándar"
        }

        val speciesRows = if (speciesList.isNotEmpty()) {
            speciesList.joinToString("\n  \\hline\n") { sp ->
                "  ${escapeLatex(sp.name)} & ${sp.count} & ${escapeLatex(sp.behavior)} & ${escapeLatex(sp.sexAge)} & ${escapeLatex(sp.notes)} \\\\"
            }
        } else {
            "  \\textit{Sin registros} & — & — & — & — \\\\"
        }

        val allPhotos = mutableListOf<Pair<String, String>>() // fileName down to caption
        speciesList.forEach { sp ->
            sp.photos.forEach { ph ->
                allPhotos.add(ph.name to "Muestra de ${sp.name}: ${ph.caption}")
            }
        }
        generalPhotos.forEach { ph ->
            allPhotos.add(ph.name to "Hábitat: ${ph.caption}")
        }

        val photoIncludes = if (allPhotos.isNotEmpty()) {
            allPhotos.joinToString("\n\n") { (name, caption) ->
                """\begin{figure}[h!]
\centering
\includegraphics[width=0.75\textwidth]{imagenes/$name}
\caption{${escapeLatex(caption)}}
\end{figure}"""
            }
        } else {
            "% No se adjuntaron fotografías para esta campaña"
        }

        return """\documentclass[spanish,10pt,letterpaper]{article}
\usepackage[utf8]{inputenc}
\usepackage[spanish]{babel}
\usepackage{graphicx}
\usepackage{geometry}
\usepackage{booktabs}
\usepackage{fancyhdr}
\usepackage{xcolor}
\usepackage{hyperref}

\geometry{verbose,tmargin=2.5cm,bmargin=2.5cm,lmargin=3cm,rmargin=2.5cm}

\definecolor{ForestGreen}{HTML}{1B5E20}
\definecolor{PrimaryLight}{HTML}{E8F5E9}
\definecolor{Charcoal}{HTML}{263238}

\pagestyle{fancy}
\fancyhf{}
\fancyhead[L]{\textbf{AveSampler Pro — Reporte Científico}}
\fancyhead[R]{${escapeLatex(session.projectName)} · ${session.date}}
\fancyfoot[C]{\thepage}

\begin{document}

% --- PORTADA ---
\begin{titlepage}
  \centering
  \vspace*{2cm}
  {\large\scshape\color{gray} Plataforma Integrada de Monitoreo Biológico}\\[0.5cm]
  {\Huge\bfseries\color{ForestGreen} ${escapeLatex(session.projectName)}}\\[0.4cm]
  {\large\itshape $tplLabel}\\[1.5cm]

  \begin{minipage}{0.8\textwidth}
    \centering
    \begin{tabular}{ll}
      \textbf{Fecha de Registro:} & $dateStr \\
      \textbf{Investigador:} & ${escapeLatex(session.author)} \\
      \textbf{Institución:} & ${escapeLatex(session.institution)} \\
      \textbf{Localidad:} & ${escapeLatex(session.location)} \\
      \textbf{Coordenadas:} & $coords \\
      \textbf{Altitud:} & ${session.altitude} m.s.n.m. \\
      \textbf{Ecosistema:} & ${escapeLatex(session.ecosystem)} \\
    \end{tabular}
  \end{minipage}

  \vfill
  {\large Generado bajo AveSampler Pro Offline Engine}\\[0.8cm]
  {\large \today}
\end{titlepage}

\newpage

\section{Información General y Condiciones de Campo}
Los siguientes datos resumen las condiciones metodológicas e inherentes tomadas en la campaña biológica sobre el ecosistema forestal.

\begin{table}[h!]
\centering
\color{Charcoal}
\begin{tabular}{l|l}
\toprule
\textbf{Parámetro Ambiental} & \textbf{Valor Observado} \\
\midrule
Clima / Nubosidad & ${escapeLatex(session.weather)} \\
Temperatura del Aire & ${session.temperature} °C \\
Humedad Relativa & ${session.humidity} \% \\
Metodología de Campo & ${escapeLatex(session.methodology)} \\
Duración de Conteo & ${session.duration} minutos \\
Hora de Inicio -- Fin & ${session.timeStart} -- ${session.timeEnd} \\
Registro / Observador & ${escapeLatex(session.observer)} \\
\bottomrule
\end{tabular}
\caption{Condiciones ecológicas muestreadas.}
\end{table}

\subsection{Observaciones Generales}
\begin{quote}
${escapeLatex(session.notes.ifBlank { "Sin observaciones adicionales reportadas en este muestreo." })}
\end{quote}

\newpage

\section{Inventario Registrado de Avifauna}
Tabla completa de observaciones de campo:

\begin{table}[h!]
\centering
\scriptsize
\begin{tabular}{p{4cm}|p{3.5cm}|c|p{3.5cm}|p{3cm}}
\toprule
\textbf{Especie Registrada} & \textbf{Familia Taxonómica} & \textbf{N} & \textbf{Comportamiento} & \textbf{Sexo/Edad} \\
\midrule
$speciesRows
\bottomrule
\end{tabular}
\caption{Listado de taxones observados de forma directa o acústica.}
\end{table}

\newpage

\section{Anexo Fotográfico Georreferenciado}
A continuación, se adjunta la colección de evidencias fotográficas capturadas directamente en el borde con su correspondiente descripción caption.

$photoIncludes

\end{document}
"""
    }

    private fun escapeLatex(str: String): String {
        return str.replace("\\", "\\textbackslash{}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("~", "\\textasciitilde{}")
            .replace("^", "\\textasciicircum{}")
    }

    fun generateSessionZip(
        context: Context,
        session: SamplingSession,
        speciesList: List<SessionSpecies>,
        generalPhotos: List<SessionPhoto>
    ): File {
        val rootDir = context.getExternalFilesDir(null) ?: context.filesDir
        val sanitizedProjectName = session.projectName.replace("\\s+".toRegex(), "_").ifBlank { "Proyecto" }
        val baseName = "${sanitizedProjectName}_${session.date}"
        val zipFile = File(rootDir, "$baseName.zip")

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                // 1. Add PDF Report
                val pdfFile = generateSessionPdf(context, session, speciesList, generalPhotos)
                if (pdfFile.exists()) {
                    zos.putNextEntry(ZipEntry("$baseName/$baseName.pdf"))
                    pdfFile.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }

                // 2. Add LaTeX Sources
                val texContent = generateSessionLatex(session, speciesList, generalPhotos)
                zos.putNextEntry(ZipEntry("$baseName/$baseName.tex"))
                zos.write(texContent.toByteArray())
                zos.closeEntry()

                // 3. Add Raw JSON metadata backup file
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val sessionAdapter = moshi.adapter(SamplingSession::class.java)
                val jsonContent = sessionAdapter.toJson(session)
                zos.putNextEntry(ZipEntry("$baseName/${baseName}_datos.json"))
                zos.write(jsonContent.toByteArray())
                zos.closeEntry()

                // 4. Add README
                val readmeText = """AveSampler Pro — Reporte Técnico de Campo
==================================================
Proyecto: ${session.projectName}
Campaña Fecha: ${session.date}
Investigador: ${session.author}
Generado en: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}

ESTRUCTURA DEL PAQUETE ZIP:
--------------------------------------------------
- $baseName.pdf            -> Informe final PDF formal listo para impresión.
- $baseName.tex            -> Código LaTeX editable para Overleaf o compilador local.
- ${baseName}_datos.json   -> Respaldo íntegro JSON offline para bases de datos relacionales.
- imagenes/                 -> Carpeta contenedora de capturas biológicas de campo.

AveSampler Pro Sync Engine © 2026. Todos los derechos reservados.
"""
                zos.putNextEntry(ZipEntry("$baseName/README.txt"))
                zos.write(readmeText.toByteArray())
                zos.closeEntry()

                // 5. Pack All photos in directory (species + general)
                val allPhotosToPack = mutableListOf<SessionPhoto>()
                speciesList.forEach { sp -> allPhotosToPack.addAll(sp.photos) }
                allPhotosToPack.addAll(generalPhotos)

                allPhotosToPack.distinctBy { it.path }.forEach { ph ->
                    val file = File(ph.path)
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry("$baseName/imagenes/${ph.name}"))
                        file.inputStream().use { input ->
                            input.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return zipFile
    }
}
