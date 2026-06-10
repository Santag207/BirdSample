import 'package:http/http.dart' as http;
import 'package:xml/xml.dart';
import 'package:sqflite/sqflite.dart' as sql;
import '../models/models.dart';
import 'database_service.dart';

class BirdRepository {
  final DatabaseService _dbService = DatabaseService.instance;

  // Projects
  Future<List<Project>> getAllProjects() async {
    final db = await _dbService.database;
    final maps = await db.query('projects', orderBy: 'updatedAt DESC');
    return maps.map((m) => Project.fromMap(m)).toList();
  }

  Future<Project?> getProjectById(int id) async {
    final db = await _dbService.database;
    final maps = await db.query('projects', where: 'id = ?', whereArgs: [id]);
    if (maps.isNotEmpty) return Project.fromMap(maps.first);
    return null;
  }

  Future<int> saveProject(Project project) async {
    final db = await _dbService.database;
    return await db.insert('projects', project.toMap());
  }

  Future<int> updateProject(Project project) async {
    final db = await _dbService.database;
    return await db.update('projects', project.toMap(),
        where: 'id = ?', whereArgs: [project.id]);
  }

  Future<int> deleteProject(int id) async {
    final db = await _dbService.database;
    return await db.delete('projects', where: 'id = ?', whereArgs: [id]);
  }

  // Sites
  Future<List<Site>> getSitesForProject(int projectId) async {
    final db = await _dbService.database;
    final maps = await db.query('sites',
        where: 'projectId = ?', whereArgs: [projectId], orderBy: 'id DESC');
    return maps.map((m) => Site.fromMap(m)).toList();
  }

  Future<int> saveSite(Site site) async {
    final db = await _dbService.database;
    return await db.insert('sites', site.toMap());
  }

  Future<int> deleteSite(int id) async {
    final db = await _dbService.database;
    return await db.delete('sites', where: 'id = ?', whereArgs: [id]);
  }

  // Samplings
  Future<List<Sampling>> getSamplingsForSite(int siteId) async {
    final db = await _dbService.database;
    final maps = await db.query('samplings',
        where: 'siteId = ?', whereArgs: [siteId], orderBy: 'date DESC');
    return maps.map((m) => Sampling.fromMap(m)).toList();
  }

  Future<int> saveSampling(Sampling sampling) async {
    final db = await _dbService.database;
    return await db.insert('samplings', sampling.toMap());
  }

  Future<int> updateSampling(Sampling sampling) async {
    final db = await _dbService.database;
    return await db.update('samplings', sampling.toMap(),
        where: 'id = ?', whereArgs: [sampling.id]);
  }

  Future<int> deleteSampling(int id) async {
    final db = await _dbService.database;
    return await db.delete('samplings', where: 'id = ?', whereArgs: [id]);
  }

  // Observations
  Future<List<Observation>> getObservationsForSampling(int samplingId) async {
    final db = await _dbService.database;
    final maps = await db.query('observations',
        where: 'samplingId = ?', whereArgs: [samplingId], orderBy: 'timestamp DESC');
    return maps.map((m) => Observation.fromMap(m)).toList();
  }

  Future<List<Observation>> getAllObservations() async {
    final db = await _dbService.database;
    final maps = await db.query('observations', orderBy: 'timestamp DESC');
    return maps.map((m) => Observation.fromMap(m)).toList();
  }

  Future<int> saveObservation(Observation observation) async {
    final db = await _dbService.database;
    return await db.insert('observations', observation.toMap());
  }

  Future<int> deleteObservation(int id) async {
    final db = await _dbService.database;
    return await db.delete('observations', where: 'id = ?', whereArgs: [id]);
  }

  // Sessions
  Future<List<SamplingSession>> getAllSessions() async {
    final db = await _dbService.database;
    final maps = await db.query('sampling_sessions', orderBy: 'createdAt DESC');
    return maps.map((m) => SamplingSession.fromMap(m)).toList();
  }

  Future<int> saveSession(SamplingSession session) async {
    final db = await _dbService.database;
    if (session.id != null && session.id! > 0) {
      await db.update('sampling_sessions', session.toMap(),
          where: 'id = ?', whereArgs: [session.id]);
      return session.id!;
    }
    return await db.insert('sampling_sessions', session.toMap());
  }

  Future<int> deleteSession(int id) async {
    final db = await _dbService.database;
    return await db.delete('sampling_sessions', where: 'id = ?', whereArgs: [id]);
  }

  // Sync Logs
  Future<List<SyncLog>> getAllSyncLogs() async {
    final db = await _dbService.database;
    final maps = await db.query('sync_logs', orderBy: 'timestamp DESC');
    return maps.map((m) => SyncLog.fromMap(m)).toList();
  }

  Future<int> saveSyncLog(SyncLog log) async {
    final db = await _dbService.database;
    return await db.insert('sync_logs', log.toMap());
  }

  // Articles
  Future<List<BirdArticle>> getAllArticles() async {
    final db = await _dbService.database;
    final maps = await db.query('bird_articles', orderBy: 'pubDate DESC');
    return maps.map((m) => BirdArticle.fromMap(m)).toList();
  }

  Future<void> saveArticles(List<BirdArticle> articles) async {
    final db = await _dbService.database;
    final batch = db.batch();
    for (var article in articles) {
      batch.insert('bird_articles', article.toMap(),
          conflictAlgorithm: sql.ConflictAlgorithm.replace);
    }
    await batch.commit(noResult: true);
  }

  Future<bool> fetchAndCacheAllAboutBirdsArticles() async {
    try {
      final response = await http.get(Uri.parse('https://www.allaboutbirds.org/news/feed/'));
      if (response.statusCode == 200) {
        final document = XmlDocument.parse(response.body);
        final items = document.findAllElements('item');
        final articles = <BirdArticle>[];

        for (var item in items) {
          final title = item.findElements('title').firstOrNull?.innerText ?? 'Sin Título';
          final link = item.findElements('link').firstOrNull?.innerText ?? '';
          final pubDate = item.findElements('pubDate').firstOrNull?.innerText ?? '';
          final description = item.findElements('description').firstOrNull?.innerText ?? '';
          final creator = item.findElements('dc:creator').firstOrNull?.innerText ?? 'Cornell Lab';
          final category = item.findElements('category').firstOrNull?.innerText ?? 'Mundo Aves';

          final cleanDesc = description
              .replaceAll('<![CDATA[', '')
              .replaceAll(']]>', '')
              .replaceAll(RegExp(r'<[^>]*>|&[^;]+;'), ' ')
              .trim();

          articles.add(BirdArticle(
            link: link,
            title: title.replaceAll('<![CDATA[', '').replaceAll(']]>', '').trim(),
            description: cleanDesc.length > 200 ? '${cleanDesc.substring(0, 197)}...' : cleanDesc,
            pubDate: pubDate,
            creator: creator,
            category: category,
          ));
        }

        if (articles.isNotEmpty) {
          await saveArticles(articles);
          return true;
        }
      }
    } catch (e) {
      print('Error fetching articles: $e');
    }
    return false;
  }

  // Templates
  Future<List<FormTemplate>> getAllTemplates() async {
    final db = await _dbService.database;
    final maps = await db.query('form_templates');
    return maps.map((m) => FormTemplate.fromMap(m)).toList();
  }

  Future<void> prePopulateTemplates() async {
    final templates = await getAllTemplates();
    if (templates.isEmpty) {
      final db = await _dbService.database;
      await db.insert('form_templates', FormTemplate(
        name: "Monitoreo Estándar de Avifauna",
        fieldsJson: '[{"key": "cloud_cover", "label": "Cobertura de Nubes (%)", "type": "NUMBER", "required": true}, {"key": "canopy_status", "label": "Estado del Dosel", "type": "DROPDOWN", "options": "Denso, Medio, Abierto, Ninguuno", "required": false}, {"key": "wind_speed", "label": "Velocidad del Viento (km/h)", "type": "NUMBER", "required": false}, {"key": "noise_level", "label": "Nivel de Ruido Ambiental", "type": "DROPDOWN", "options": "Bajo, Moderado, Alto", "required": true}]'
      ).toMap());
      await db.insert('form_templates', FormTemplate(
        name: "Ficha de Hábitat y Vegetación",
        fieldsJson: '[{"key": "canopy_height", "label": "Altura Promedio de Dosel (m)", "type": "NUMBER", "required": true}, {"key": "understory_density", "label": "Densidad de Sotobosque", "type": "DROPDOWN", "options": "Alta, Media, Baja, Nula", "required": true}, {"key": "human_impact", "label": "Evidencia de Impacto Humano", "type": "DROPDOWN", "options": "Grave, Moderado, Leve, Ninguno", "required": true}]'
      ).toMap());
    }
  }
}
