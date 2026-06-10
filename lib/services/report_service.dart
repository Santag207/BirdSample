import 'dart:io';
import 'dart:convert';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:path_provider/path_provider.dart';
import 'package:intl/intl.dart';
import 'package:archive/archive.dart';
import '../models/models.dart';

class ReportService {

  // PDF Export for Project
  static Future<File> exportProjectToPdf({
    required Project project,
    required List<Site> sites,
    required List<Sampling> samplings,
    required List<Observation> observations,
  }) async {
    final pdf = pw.Document();
    final dateStr = DateFormat('dd/MM/yyyy HH:mm').format(DateTime.now());

    pdf.addPage(
      pw.Page(
        pageFormat: PdfPageFormat.a4,
        build: (pw.Context context) {
          return pw.Column(
            cross: pw.CrossAxisAlignment.start,
            children: [
              pw.Text('BIRD SAMPLE ENGINE', style: pw.TextStyle(fontSize: 14, color: PdfColors.grey600)),
              pw.SizedBox(height: 20),
              pw.Text('Informe Técnico de Avifauna', style: pw.TextStyle(fontSize: 28, fontWeight: pw.FontWeight.bold, color: PdfColors.green900)),
              pw.SizedBox(height: 40),
              pw.Container(
                padding: const pw.EdgeInsets.all(10),
                decoration: pw.BoxDecoration(
                  color: PdfColors.green50,
                  border: pw.Border.all(color: PdfColors.green100, width: 2),
                ),
                child: pw.Column(
                  cross: pw.CrossAxisAlignment.start,
                  children: [
                    pw.Text('PROYECTO: ${project.name}', style: pw.TextStyle(fontWeight: pw.FontWeight.bold, color: PdfColors.green900)),
                    pw.Text('Cliente: ${project.client}'),
                    pw.Text('Contrato: ${project.contractNumber}'),
                  ],
                ),
              ),
              pw.SizedBox(height: 40),
              pw.Text('Resumen de Monitoreo', style: pw.TextStyle(fontSize: 18, fontWeight: pw.FontWeight.bold, color: PdfColors.green800)),
              pw.SizedBox(height: 10),
              pw.Text('Fecha de Reporte: $dateStr'),
              pw.Text('Sitios Evaluados: ${sites.size}'),
              pw.Text('Campañas de Muestreo: ${samplings.length}'),
              pw.Text('Total de Observaciones: ${observations.length}'),
              pw.Text('Total de Individuos: ${observations.fold(0, (sum, item) => sum + item.quantity)}'),
            ],
          );
        },
      ),
    );

    // Save
    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/Reporte_${project.name.replaceAll(' ', '_')}.pdf');
    await file.writeAsBytes(await pdf.save());
    return file;
  }

  // CSV Export for Project
  static Future<File> exportProjectToCsv({
    required Project project,
    required List<Site> sites,
    required List<Sampling> samplings,
    required List<Observation> observations,
  }) async {
    final StringBuffer buffer = StringBuffer();
    // BOM for Excel
    buffer.write('\uFEFF');
    buffer.writeln('REPORTE BIRD SAMPLE - PROYECTO: ${project.name}');
    buffer.writeln('Cliente,${project.client}');
    buffer.writeln('Contrato,${project.contractNumber}');
    buffer.writeln('');

    buffer.writeln('Sitios Registrados');
    buffer.writeln('ID,Nombre,Departamento,Municipio,Vereda,Ecosistema,Latitud,Longitud,Altitud');
    for (var s in sites) {
      buffer.writeln('${s.id},"${s.name}","${s.department}","${s.municipality}","${s.vereda}","${s.ecosystem}",${s.latitude},${s.longitude},${s.altitude}');
    }
    buffer.writeln('');

    buffer.writeln('Campañas de Muestreo');
    buffer.writeln('ID,SitioID,Observador,Fecha,Clima,Temp,Hum,Metodologia');
    for (var s in samplings) {
      final date = DateFormat('yyyy-MM-dd HH:mm').format(DateTime.fromMillisecondsSinceEpoch(s.date));
      buffer.writeln('${s.id},${s.siteId},"${s.observer}","$date","${s.weather}",${s.temperature},${s.humidity},"${s.methodology}"');
    }
    buffer.writeln('');

    buffer.writeln('Inventario de Especies');
    buffer.writeln('ID,MuestreoID,Nombre Comun,Nombre Cientifico,Familia,Cantidad,Sexo,Edad,Comportamiento,Latitud,Longitud,Altitud,Notas');
    for (var o in observations) {
      buffer.writeln('${o.id},${o.samplingId},"${o.birdCommonName}","${o.birdScientificName}","${o.birdFamily}",${o.quantity},"${o.sex}","${o.age}","${o.behavior}",${o.latitude},${o.longitude},${o.altitude},"${o.notes.replaceAll('"', '""')}"');
    }

    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/Exportacion_${project.name.replaceAll(' ', '_')}.csv');
    await file.writeAsString(buffer.toString());
    return file;
  }

  // Session ZIP Export (PDF + LaTeX + JSON + Photos)
  static Future<File> generateSessionZip({
    required SamplingSession session,
    required List<SessionSpecies> speciesList,
    required List<SessionPhoto> generalPhotos,
  }) async {
    final archive = Archive();

    // 1. PDF (Simplified for this step)
    final pdfFile = await _generateSessionPdf(session, speciesList, generalPhotos);
    archive.addFile(ArchiveFile('reporte.pdf', pdfFile.lengthSync(), pdfFile.readAsBytesSync()));

    // 2. LaTeX
    final latex = _generateSessionLatex(session, speciesList, generalPhotos);
    archive.addFile(ArchiveFile('fuentes.tex', latex.length, utf8.encode(latex)));

    // 3. JSON
    final jsonData = jsonEncode(session.toMap());
    archive.addFile(ArchiveFile('datos.json', jsonData.length, utf8.encode(jsonData)));

    // 4. Photos
    for (var sp in speciesList) {
      for (var photo in sp.photos) {
        final f = File(photo.path);
        if (await f.exists()) {
          archive.addFile(ArchiveFile('imagenes/${photo.name}', f.lengthSync(), f.readAsBytesSync()));
        }
      }
    }
    for (var photo in generalPhotos) {
      final f = File(photo.path);
      if (await f.exists()) {
        archive.addFile(ArchiveFile('imagenes/${photo.name}', f.lengthSync(), f.readAsBytesSync()));
      }
    }

    final zipData = ZipEncoder().encode(archive);
    final directory = await getApplicationDocumentsDirectory();
    final file = File('${directory.path}/Session_${session.id}_Export.zip');
    await file.writeAsBytes(zipData!);
    return file;
  }

  static Future<File> _generateSessionPdf(SamplingSession session, List<SessionSpecies> speciesList, List<SessionPhoto> generalPhotos) async {
    final pdf = pw.Document();
    pdf.addPage(pw.Page(build: (c) => pw.Text('Reporte de Sesion: ${session.projectName}')));
    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/temp_session.pdf');
    await file.writeAsBytes(await pdf.save());
    return file;
  }

  static String _generateSessionLatex(SamplingSession session, List<SessionSpecies> speciesList, List<SessionPhoto> generalPhotos) {
    return "% LaTeX Template for Bird Session\n\\documentclass{article}\n\\begin{document}\nSesion: ${session.projectName}\n\\end{document}";
  }
}

extension ListSize on List {
  int get size => length;
}
