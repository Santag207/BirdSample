import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';
import '../models/models.dart';
import '../services/bird_repository.dart';
import '../services/gemini_service.dart';
import '../services/google_drive_service.dart';
import '../services/report_service.dart';

class BirdProvider with ChangeNotifier {
  final BirdRepository _repository = BirdRepository();

  List<Project> _projects = [];
  List<SamplingSession> _sessions = [];
  List<FormTemplate> _templates = [];
  List<SyncLog> _syncLogs = [];
  List<Observation> _allObservations = [];
  List<BirdArticle> _birdArticles = [];

  List<Project> get projects => _projects;
  List<SamplingSession> get sessions => _sessions;
  List<FormTemplate> get templates => _templates;
  List<SyncLog> get syncLogs => _syncLogs;
  List<Observation> get allObservations => _allObservations;
  List<BirdArticle> get birdArticles => _birdArticles;

  bool _isRefreshingArticles = false;
  bool get isRefreshingArticles => _isRefreshingArticles;

  Project? _selectedProject;
  Site? _selectedSite;
  Sampling? _selectedSampling;

  Project? get selectedProject => _selectedProject;
  Site? get selectedSite => _selectedSite;
  Sampling? get selectedSampling => _selectedSampling;

  List<Site> _selectedProjectSites = [];
  List<Sampling> _selectedSiteSamplings = [];
  List<Observation> _selectedSamplingObservations = [];

  List<Site> get selectedProjectSites => _selectedProjectSites;
  List<Sampling> get selectedSiteSamplings => _selectedSiteSamplings;
  List<Observation> get selectedSamplingObservations => _selectedSamplingObservations;

  final Map<String, String> dynamicAnswers = {};
  String aiResponse = "";
  bool aiLoading = false;
  bool isSyncing = false;
  double syncProgress = 0.0;
  String syncLogMessage = "";

  GoogleSignInAccount? _googleUser;
  GoogleSignInAccount? get googleUser => _googleUser;

  BirdProvider() {
    _init();
  }

  Future<void> _init() async {
    await _repository.prePopulateTemplates();
    await loadAllData();
    fetchArticlesFromAllAboutBirds();
    _checkGoogleLogin();
  }

  Future<void> _checkGoogleLogin() async {
    _googleUser = await GoogleDriveService.signInSilently();
    notifyListeners();
  }

  Future<void> loginWithGoogle() async {
    _googleUser = await GoogleDriveService.signIn();
    notifyListeners();
  }

  Future<void> logoutFromGoogle() async {
    await GoogleDriveService.signOut();
    _googleUser = null;
    notifyListeners();
  }

  Future<void> loadAllData() async {
    _projects = await _repository.getAllProjects();
    _sessions = await _repository.getAllSessions();
    _templates = await _repository.getAllTemplates();
    _syncLogs = await _repository.getAllSyncLogs();
    _allObservations = await _repository.getAllObservations();
    _birdArticles = await _repository.getAllArticles();
    notifyListeners();
  }

  // Helper to gather all data for a report
  Future<Map<String, dynamic>> getProjectFullData(int projectId) async {
    final sites = await _repository.getSitesForProject(projectId);
    List<Sampling> allSamplings = [];
    List<Observation> allObservations = [];
    
    for (var site in sites) {
      final samplings = await _repository.getSamplingsForSite(site.id!);
      allSamplings.addAll(samplings);
      for (var sampling in samplings) {
        final observations = await _repository.getObservationsForSampling(sampling.id!);
        allObservations.addAll(observations);
      }
    }
    
    return {
      'sites': sites,
      'samplings': allSamplings,
      'observations': allObservations,
    };
  }

  void selectProject(Project? project) async {
    _selectedProject = project;
    if (project != null) {
      _selectedProjectSites = await _repository.getSitesForProject(project.id!);
    } else {
      _selectedProjectSites = [];
    }
    notifyListeners();
  }

  void selectSite(Site? site) async {
    _selectedSite = site;
    if (site != null) {
      _selectedSiteSamplings = await _repository.getSamplingsForSite(site.id!);
    } else {
      _selectedSiteSamplings = [];
    }
    notifyListeners();
  }

  void selectSampling(Sampling? sampling) async {
    _selectedSampling = sampling;
    if (sampling != null) {
      _selectedSamplingObservations = await _repository.getObservationsForSampling(sampling.id!);
      dynamicAnswers.clear();
      try {
        final Map<String, dynamic> decoded = jsonDecode(sampling.customFieldsJson);
        decoded.forEach((k, v) => dynamicAnswers[k] = v.toString());
      } catch (e) {}
    } else {
      _selectedSamplingObservations = [];
    }
    notifyListeners();
  }

  Future<void> createProject(String name, String client, String contract, String desc) async {
    final project = Project(name: name, client: client, contractNumber: contract, description: desc);
    await _repository.saveProject(project);
    await loadAllData();
  }

  Future<void> deleteProject(int id) async {
    await _repository.deleteProject(id);
    if (_selectedProject?.id == id) _selectedProject = null;
    await loadAllData();
  }

  Future<void> createSite(String name, String dept, String mun, String vereda, String eco, double lat, double lon, double alt) async {
    if (_selectedProject == null) return;
    final site = Site(
      projectId: _selectedProject!.id!,
      name: name,
      department: dept,
      municipality: mun,
      vereda: vereda,
      ecosystem: eco,
      latitude: lat,
      longitude: lon,
      altitude: alt,
    );
    await _repository.saveSite(site);
    _selectedProjectSites = await _repository.getSitesForProject(_selectedProject!.id!);
    notifyListeners();
  }

  Future<void> deleteSite(int id) async {
    await _repository.deleteSite(id);
    if (_selectedSite?.id == id) _selectedSite = null;
    if (_selectedProject != null) {
      _selectedProjectSites = await _repository.getSitesForProject(_selectedProject!.id!);
    }
    notifyListeners();
  }

  Future<void> startSampling(String observer, String weather, double temp, double hum, String method) async {
    if (_selectedSite == null) return;
    final sampling = Sampling(
      siteId: _selectedSite!.id!,
      observer: observer,
      weather: weather,
      temperature: temp,
      humidity: hum,
      methodology: method,
      customFieldsJson: jsonEncode(dynamicAnswers),
    );
    final id = await _repository.saveSampling(sampling);
    _selectedSampling = sampling.copyWith(id: id);
    _selectedSiteSamplings = await _repository.getSamplingsForSite(_selectedSite!.id!);
    notifyListeners();
  }

  Future<void> updateSamplingDraft(String observer, String weather, double temp, double hum, String method) async {
    if (_selectedSampling == null) return;
    final updated = _selectedSampling!.copyWith(
      observer: observer,
      weather: weather,
      temperature: temp,
      humidity: hum,
      methodology: method,
      customFieldsJson: jsonEncode(dynamicAnswers),
    );
    await _repository.updateSampling(updated);
    _selectedSampling = updated;
    notifyListeners();
  }

  Future<void> deleteSampling(int id) async {
    await _repository.deleteSampling(id);
    if (_selectedSampling?.id == id) _selectedSampling = null;
    if (_selectedSite != null) {
      _selectedSiteSamplings = await _repository.getSamplingsForSite(_selectedSite!.id!);
    }
    notifyListeners();
  }

  Future<void> addObservation(Observation obs) async {
    await _repository.saveObservation(obs);
    if (_selectedSampling != null) {
      _selectedSamplingObservations = await _repository.getObservationsForSampling(_selectedSampling!.id!);
    }
    _allObservations = await _repository.getAllObservations();
    notifyListeners();
  }

  Future<void> deleteObservation(int id) async {
    await _repository.deleteObservation(id);
    if (_selectedSampling != null) {
      _selectedSamplingObservations = await _repository.getObservationsForSampling(_selectedSampling!.id!);
    }
    _allObservations = await _repository.getAllObservations();
    notifyListeners();
  }

  Future<void> saveSamplingSession(SamplingSession session) async {
    await _repository.saveSession(session);
    await loadAllData();
  }

  Future<void> deleteSamplingSession(int id) async {
    await _repository.deleteSession(id);
    await loadAllData();
  }

  Future<void> searchBirdWithGemini(String feathers, String beak, String habitat) async {
    aiLoading = true;
    aiResponse = "Consultando con la base de datos experta de avifauna...";
    notifyListeners();

    const systemIns = "Eres un ornitólogo experto especializado en avifauna sudamericana y colombiana.";
    final userPrompt = """
      Basado en la siguiente descripción del ave, por favor identifícala y devuélvenos:
      1. Nombre científico aproximado y familia taxonómica.
      2. Nombre común más usado en español.
      3. Estado de conservación de la IUCN aproximado y datos curiosos sobre su comportamiento de alimentación.

      DESCRIPCIÓN DEL AVE:
      - Plumaje: $feathers
      - Tipo de Pico / Patas: $beak
      - Entorno geográfico / Hábitat donde se observó: $habitat
    """;

    aiResponse = await GeminiService.askGemini(userPrompt, systemInstruction: systemIns);
    aiLoading = false;
    notifyListeners();
  }

  Future<void> fetchArticlesFromAllAboutBirds() async {
    _isRefreshingArticles = true;
    notifyListeners();
    await _repository.fetchAndCacheAllAboutBirdsArticles();
    _birdArticles = await _repository.getAllArticles();
    _isRefreshingArticles = false;
    notifyListeners();
  }

  Future<void> performIncrementalSync() async {
    if (_selectedProject == null || isSyncing) return;

    isSyncing = true;
    syncProgress = 0.1;
    syncLogMessage = "Iniciando motor de sincronización incremental...";
    notifyListeners();

    await Future.delayed(const Duration(seconds: 1));
    syncProgress = 0.3;
    syncLogMessage = "Validando tokens de sesión Cloud...";

    if (_googleUser == null) {
      isSyncing = false;
      syncLogMessage = "Error: No has iniciado sesión con Google.";
      await _repository.saveSyncLog(SyncLog(
        action: "Sincronización Cloud",
        status: "FALLIDO",
        summary: "Usuario no autenticado en Google.",
      ));
      notifyListeners();
      return;
    }
    notifyListeners();

    await Future.delayed(const Duration(seconds: 1));
    syncProgress = 0.5;
    syncLogMessage = "Generando reportes para exportación...";

    final projectData = await getProjectFullData(_selectedProject!.id!);
    final pdfFile = await ReportService.exportProjectToPdf(
      project: _selectedProject!,
      sites: projectData['sites'],
      samplings: projectData['samplings'],
      observations: projectData['observations'],
    );

    final csvFile = await ReportService.exportProjectToCsv(
      project: _selectedProject!,
      sites: projectData['sites'],
      samplings: projectData['samplings'],
      observations: projectData['observations'],
    );

    notifyListeners();

    await Future.delayed(const Duration(seconds: 1));
    syncProgress = 0.7;
    syncLogMessage = "Subiendo archivos a Google Drive...";

    final pdfSuccess = await GoogleDriveService.uploadFileToDrive(pdfFile, 'Reporte_${_selectedProject!.name}.pdf', mimeType: 'application/pdf');
    final csvSuccess = await GoogleDriveService.uploadFileToDrive(csvFile, 'Datos_${_selectedProject!.name}.csv', mimeType: 'text/csv');

    if (!pdfSuccess || !csvSuccess) {
       isSyncing = false;
       syncLogMessage = "Error al subir archivos a Drive.";
       await _repository.saveSyncLog(SyncLog(
         action: "Subida a Drive: ${_selectedProject!.name}",
         status: "FALLIDO",
         summary: "Falla en la comunicación con el API de Drive.",
       ));
       notifyListeners();
       return;
    }

    notifyListeners();

    await Future.delayed(const Duration(seconds: 1));

    final updatedProject = _selectedProject!.copyWith(status: "Sincronizado", updatedAt: DateTime.now().millisecondsSinceEpoch);
    await _repository.updateProject(updatedProject);
    _selectedProject = updatedProject;

    await _repository.saveSyncLog(SyncLog(
      action: "Subida de Proyecto: ${updatedProject.name}",
      status: "EXITOSO",
      summary: "Sincronizados correctamente los datos en Google Drive.",
    ));

    syncProgress = 1.0;
    syncLogMessage = "¡Sincronización completada!";
    isSyncing = false;
    await loadAllData();
    notifyListeners();
  }
}
