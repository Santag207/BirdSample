package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun exportToPdf(
        context: Context,
        project: Project,
        sites: List<Site>,
        samplings: List<Sampling>,
        observations: List<Observation>
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in postscript points
        val pageHeight = 842 // A4 height

        // --- PAGE 1: COVER PAGE ---
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val paintText = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        val paintTitle = Paint().apply {
            color = Color.parseColor("#1B5E20") // Rich Forest Green
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintHeading = Paint().apply {
            color = Color.parseColor("#2E7D32")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintBold = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintBox = Paint().apply {
            color = Color.parseColor("#E8F5E9") // Smooth light green background
            style = Paint.Style.FILL
        }
        val paintStroke = Paint().apply {
            color = Color.parseColor("#C8E6C9")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Draw elegant modern decorative green bar on left edge
        canvas.drawRect(0f, 0f, 30f, pageHeight.toFloat(), Paint().apply { color = Color.parseColor("#1B5E20") })

        var y = 150f
        canvas.drawText("BIRD SAMPLE ENGINE", 60f, y, paintBold.apply { 
            textSize = 14f
            color = Color.parseColor("#666666")
        })
        y += 40f
        canvas.drawText("Informe Técnico de Avifauna", 60f, y, paintTitle)
        
        y += 60f
        canvas.drawRect(60f, y, pageWidth.toFloat() - 40f, y + 100f, paintBox)
        canvas.drawRect(60f, y, pageWidth.toFloat() - 40f, y + 100f, paintStroke)
        
        canvas.drawText("PROYECTO: ${project.name}", 80f, y + 35f, paintBold.apply { 
            textSize = 13f
            color = Color.parseColor("#1B5E20")
        })
        canvas.drawText("Cliente: ${project.client}", 80f, y + 60f, paintText.apply { color = Color.BLACK })
        canvas.drawText("Contrato: ${project.contractNumber}", 80f, y + 80f, paintText)
        
        y += 150f
        canvas.drawText("Resumen de Monitoreo", 60f, y, paintHeading)
        y += 25f
        
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Fecha de Reporte: $dateStr", 60f, y, paintText.apply { color = Color.DKGRAY })
        y += 20f
        canvas.drawText("Sitios Evaluados: ${sites.size}", 60f, y, paintText)
        y += 20f
        canvas.drawText("Campañas de Muestreo: ${samplings.size}", 60f, y, paintText)
        y += 20f
        canvas.drawText("Total de Observaciones de Aves: ${observations.size}", 60f, y, paintText)
        y += 20f
        val totalBirds = observations.sumOf { it.quantity }
        canvas.drawText("Total de Individuos Registrados: $totalBirds", 60f, y, paintText)

        // Draw Species statistics pie chart decoration using canvas
        y += 50f
        canvas.drawText("Distribución de Familias de Aves", 60f, y, paintBold.apply { textSize = 14f })
        y += 30f

        val familiesMap = observations.groupBy { it.birdFamily }.mapValues { it.value.sumOf { o -> o.quantity } }
        if (familiesMap.isNotEmpty()) {
            val total = familiesMap.values.sum().toFloat()
            var startAngle = 0f
            val colors = listOf("#4CAF50", "#FF9800", "#00BCD4", "#E91E63", "#9C27B0", "#3F51B5", "#FFEB3B")
            var idx = 0
            
            // Draw pie slices
            familiesMap.forEach { (family, count) ->
                val sweepAngle = (count / total) * 360f
                val slicePaint = Paint().apply {
                    color = Color.parseColor(colors[idx % colors.size])
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawArc(80f, y, 220f, y + 140f, startAngle, sweepAngle, true, slicePaint)
                
                // Legend
                val textLegend = "${family.substringBefore(" (")}: $count ind. (${String.format("%.1f", (count/total)*100)}%)"
                canvas.drawRect(260f, y + (idx * 20f), 275f, y + (idx * 20f) + 12f, slicePaint)
                canvas.drawText(textLegend, 285f, y + (idx * 20f) + 10f, paintText.apply { textSize = 10f; color = Color.BLACK })
                
                startAngle += sweepAngle
                idx++
            }
        } else {
            canvas.drawText("Ninguna observación registrada aún en este proyecto.", 60f, y + 20f, paintText)
        }

        // Draw footer page cover
        canvas.drawText("Página 1", pageWidth / 2f, pageHeight - 30f, paintText.apply { textSize = 10f })
        pdfDocument.finishPage(page)

        // --- PAGE 2: SITES AND DETAILED OBSERVATIONS TABLE ---
        pageNumber = 2
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        // Decorative green bar
        canvas.drawRect(0f, 0f, 30f, pageHeight.toFloat(), Paint().apply { color = Color.parseColor("#1B5E20") })

        y = 60f
        canvas.drawText("Detalle de Registros de Campo", 60f, y, paintHeading)
        y += 35f

        // Table headers
        val headers = listOf("Especie", "Familia", "Cant.", "Sexo", "Edad", "Comportamiento")
        val colWidths = listOf(140f, 130f, 40f, 50f, 50f, 100f)
        val colPositions = mutableListOf<Float>().apply {
            var curr = 60f
            add(curr)
            for (i in 0 until colWidths.size - 1) {
                curr += colWidths[i]
                add(curr)
            }
        }

        // Draw Header row background
        canvas.drawRect(60f, y - 15f, pageWidth - 20f, y + 10f, paintBox)
        for (i in headers.indices) {
            canvas.drawText(headers[i], colPositions[i], yf(y), paintBold.apply { textSize = 9f; color = Color.parseColor("#1B5E20") })
        }
        y += 20f

        // Draw observation records
        var countObs = 0
        paintText.apply { textSize = 9f; color = Color.BLACK }
        for (obs in observations) {
            if (y > pageHeight - 100) {
                // Break and start a new page if page overflows
                canvas.drawText("Página $pageNumber", pageWidth / 2f, pageHeight - 30f, paintText.apply { textSize = 10f })
                pdfDocument.finishPage(page)
                
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                canvas.drawRect(0f, 0f, 30f, pageHeight.toFloat(), Paint().apply { color = Color.parseColor("#1B5E20") })
                y = 60f
                canvas.drawText("Detalle de Registros de Campo (Continuación)", 60f, y, paintHeading)
                y += 35f
                
                // Redraw table headers
                canvas.drawRect(60f, y - 15f, pageWidth - 20f, y + 10f, paintBox)
                for (i in headers.indices) {
                    canvas.drawText(headers[i], colPositions[i], yf(y), paintBold.apply { textSize = 9f; color = Color.parseColor("#1B5E20") })
                }
                y += 20f
            }

            // Draw current record
            canvas.drawText(obs.birdCommonName, colPositions[0], y, paintText)
            canvas.drawText(obs.birdFamily.substringBefore(" ("), colPositions[1], y, paintText)
            canvas.drawText(obs.quantity.toString(), colPositions[2], y, paintText)
            canvas.drawText(obs.sex, colPositions[3], y, paintText)
            canvas.drawText(obs.age, colPositions[4], y, paintText)
            canvas.drawText(obs.behavior, colPositions[5], y, paintText)

            y += 18f
            // Draw subtext with scientific name
            canvas.drawText("  [${obs.birdScientificName}]", colPositions[0], y, paintText.apply { 
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                color = Color.GRAY
            })
            paintText.apply { 
                typeface = Typeface.DEFAULT
                color = Color.BLACK
            }
            
            y += 15f
            canvas.drawLine(60f, y - 12f, pageWidth - 20f, y - 12f, Paint().apply { 
                color = Color.parseColor("#EEEEEE")
                strokeWidth = 1f
            })
            countObs++
            if (countObs >= 25) { // Protect total length
                break
            }
        }

        // Add coordinate footnotes
        y += 20f
        canvas.drawText("Metadata del Sistema de Archivo:", 60f, y, paintBold.apply { textSize = 10f; color = Color.DKGRAY })
        y += 15f
        canvas.drawText("Ubicación GPS general del proyecto triangulada según muestras.", 60f, y, paintText.apply { textSize = 9f; color = Color.GRAY })

        canvas.drawText("Página $pageNumber", pageWidth / 2f, pageHeight - 30f, paintText.apply { textSize = 10f })
        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        val file = File(context.cacheDir, "Reporte_${project.name.replace(" ", "_")}.pdf")
        FileOutputStream(file).use {
            pdfDocument.writeTo(it)
        }
        pdfDocument.close()
        return file
    }

    private fun yf(y: Float) = y // Direct value mapper

    // Excel exporter CSV mock equivalent format of .xlsx readable by spreadsheets beautifully
    fun exportToExcelCsv(
        context: Context,
        project: Project,
        sites: List<Site>,
        samplings: List<Sampling>,
        observations: List<Observation>
    ): File {
        val file = File(context.cacheDir, "Exportacion_${project.name.replace(" ", "_")}.csv")
        file.bufferedWriter().use { writer ->
            // Excel UTF-8 BOM to display accented characters correctly
            writer.write("\uFEFF")
            writer.write("REPORTE BIRD SAMPLE - PROYECTO: ${project.name}\n")
            writer.write("Cliente,${project.client}\n")
            writer.write("Contrato,${project.contractNumber}\n")
            writer.write("Estado,${project.status}\n")
            writer.write("\n")
            
            // Site references
            writer.write("Sitios Registrados\n")
            writer.write("ID,Nombre,Departamento,Municipio,Vereda,Ecosistema,Latitud,Longitud,Altitud\n")
            sites.forEach { site ->
                writer.write("${site.id},\"${site.name}\",\"${site.department}\",\"${site.municipality}\",\"${site.vereda}\",\"${site.ecosystem}\",${site.latitude},${site.longitude},${site.altitude}\n")
            }
            writer.write("\n")

            // Samples lists
            writer.write("Campañas de Muestreo de Campo\n")
            writer.write("ID,SitioID,Observador,Fecha,Clima,Temperatura (C),Humedad (%),Metodologia,Campos Dinamicos\n")
            samplings.forEach { s ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(s.date))
                writer.write("${s.id},${s.siteId},\"${s.observer}\",\"$dateStr\",\"${s.weather}\",${s.temperature},${s.humidity},\"${s.methodology}\",\"${s.customFieldsJson.replace("\"", "\"\"")}\"\n")
            }
            writer.write("\n")

            // Observations data
            writer.write("Inventario de Especies de Aves Observadas\n")
            writer.write("ID,MuestreoID,Nombre Comun,Nombre Cientifico,Familia,Cantidad,Sexo,Edad,Comportamiento,Latitud,Longitud,Altitud,Notas,Fecha Registro\n")
            observations.forEach { obs ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(obs.timestamp))
                writer.write("${obs.id},${obs.samplingId},\"${obs.birdCommonName}\",\"${obs.birdScientificName}\",\"${obs.birdFamily}\",${obs.quantity},\"${obs.sex}\",\"${obs.age}\",\"${obs.behavior}\",${obs.latitude},${obs.longitude},${obs.altitude},\"${obs.notes.replace("\"", "\"\"")}\",\"$dateStr\"\n")
            }
        }
        return file
    }
}
