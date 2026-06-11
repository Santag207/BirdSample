import 'dart:io';
import 'dart:convert';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:path_provider/path_provider.dart';
import 'package:intl/intl.dart';
import 'package:archive/archive.dart';
import '../models/models.dart';

class ReportService {

  // Export Project to PDF
  static Future<File> exportProjectToPdf({
    required Project project,
    required List<Site> sites,
    required List<Sampling> samplings,
    required List<Observation> observations,
  }) async {
    final pdf = pw.Document();
    pdf.addPage(
      pw.Page(
        pageFormat: PdfPageFormat.a4,
        build: (pw.Context context) => _buildPdfContent(project, sites, samplings, observations),
      ),
    );
    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/Reporte_${project.name.replaceAll(' ', '_')}.pdf');
    await file.writeAsBytes(await pdf.save());
    return file;
  }

  // Export Session to PDF (Public)
  static Future<File> exportSessionToPdf(SamplingSession session, List<SessionSpecies> speciesList) async {
    final pdf = pw.Document();
    pdf.addPage(pw.Page(
      build: (c) => pw.Column(
        crossAxisAlignment: pw.CrossAxisAlignment.start,
        children: [
          pw.Text('REPORTE DE SESIÓN: ${session.projectName}', style: pw.TextStyle(fontSize: 20, fontWeight: pw.FontWeight.bold)),
          pw.SizedBox(height: 10),
          pw.Text('Autor: ${session.author}'),
          pw.Text('Localidad: ${session.location}'),
          pw.Divider(),
          pw.Text('Especies Registradas:', style: pw.TextStyle(fontWeight: pw.FontWeight.bold)),
          ...speciesList.map((s) => pw.Text('- ${s.commonName} (${s.name})')),
        ]
      )
    ));
    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/Sesion_${session.projectName.replaceAll(' ', '_')}.pdf');
    await file.writeAsBytes(await pdf.save());
    return file;
  }

  // Export Session to CSV (Excel compatible)
  static Future<File> exportSessionToCsv(SamplingSession session, List<SessionSpecies> speciesList) async {
    final StringBuffer buffer = StringBuffer();
    buffer.write('\uFEFF'); // BOM for Excel
    buffer.writeln('PROYECTO,AUTOR,LOCALIDAD,FECHA');
    buffer.writeln('"${session.projectName}","${session.author}","${session.location}","${session.date}"');
    buffer.writeln('');
    buffer.writeln('FAMILIA,NOMBRE_CIENTIFICO,NOMBRE_COMUN,CANTIDAD');
    for (var s in speciesList) {
      buffer.writeln('"${s.family}","${s.name}","${s.commonName}",${s.count}');
    }
    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/Datos_${session.projectName.replaceAll(' ', '_')}.csv');
    await file.writeAsString(buffer.toString());
    return file;
  }

  static pw.Widget _buildPdfContent(Project project, List<Site> sites, List<Sampling> samplings, List<Observation> observations) {
    return pw.Column(
      crossAxisAlignment: pw.CrossAxisAlignment.start,
      children: [
        pw.Text('BIRD SAMPLE ENGINE - PROYECTO', style: pw.TextStyle(color: PdfColors.grey600)),
        pw.SizedBox(height: 10),
        pw.Text(project.name, style: pw.TextStyle(fontSize: 24, fontWeight: pw.FontWeight.bold)),
        pw.Text('Cliente: ${project.client}'),
        pw.Divider(),
        pw.Text('Resumen: ${sites.length} sitios, ${observations.length} observaciones.'),
      ]
    );
  }

  // Session ZIP Export (Full package)
  static Future<File> generateSessionZip({
    required SamplingSession session,
    required List<SessionSpecies> speciesList,
    required List<SessionPhoto> generalPhotos,
  }) async {
    final archive = Archive();
    final pdfFile = await exportSessionToPdf(session, speciesList);
    archive.addFile(ArchiveFile('reporte.pdf', pdfFile.lengthSync(), pdfFile.readAsBytesSync()));

    final jsonData = jsonEncode(session.toMap());
    archive.addFile(ArchiveFile('datos.json', jsonData.length, utf8.encode(jsonData)));

    for (var sp in speciesList) {
      for (var photo in sp.photos) {
        final f = File(photo.path);
        if (await f.exists()) archive.addFile(ArchiveFile('imagenes/${photo.name}', f.lengthSync(), f.readAsBytesSync()));
      }
    }

    final zipData = ZipEncoder().encode(archive);
    final directory = await getApplicationDocumentsDirectory();
    final file = File('${directory.path}/Session_${session.id}_Export.zip');
    if (zipData != null) await file.writeAsBytes(zipData);
    return file;
  }

  static Future<File> exportProjectToCsv({
    required Project project,
    required List<Site> sites,
    required List<Sampling> samplings,
    required List<Observation> observations,
  }) async {
    final StringBuffer buffer = StringBuffer();
    buffer.write('\uFEFF');
    buffer.writeln('PROYECTO: ${project.name}');
    buffer.writeln('NOMBRE_COMUN,NOMBRE_CIENTIFICO,CANTIDAD');
    for (var o in observations) {
      buffer.writeln('"${o.birdCommonName}","${o.birdScientificName}",${o.quantity}');
    }
    final directory = await getTemporaryDirectory();
    final file = File('${directory.path}/Exportacion_${project.name.replaceAll(' ', '_')}.csv');
    await file.writeAsString(buffer.toString());
    return file;
  }
}
