import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:share_plus/share_plus.dart';
import '../../providers/bird_provider.dart';
import '../../models/models.dart';
import '../../services/report_service.dart';
import '../../services/media_service.dart';
import '../../services/gemini_service.dart';
import '../../services/location_service.dart';
import '../../data/species_catalog.dart';

class SamplingWizardScreen extends StatefulWidget {
  const SamplingWizardScreen({super.key});

  @override
  State<SamplingWizardScreen> createState() => _SamplingWizardScreenState();
}

class _SamplingWizardScreenState extends State<SamplingWizardScreen> {
  int _currentStep = 0;
  SamplingSession? _activeSession;
  List<SessionSpecies> _speciesList = [];
  List<SessionPhoto> _generalPhotos = [];
  int? _editingSpeciesIndex;

  final _projectController = TextEditingController();
  final _authorController = TextEditingController();
  final _institutionController = TextEditingController();
  final _locationController = TextEditingController();
  final _ecosystemController = TextEditingController();
  final _observerController = TextEditingController();
  final _notesController = TextEditingController();
  final _weatherController = TextEditingController();
  final _tempController = TextEditingController();
  final _humController = TextEditingController();
  final _methodController = TextEditingController();

  final _spCommonNameController = TextEditingController();
  final _spScientificNameController = TextEditingController();
  final _spCountController = TextEditingController();
  final _spBehaviorController = TextEditingController();
  final _spSexAgeController = TextEditingController();
  final _spNotesController = TextEditingController();
  double _lat = 0, _lon = 0, _alt = 0;

  final MediaService _mediaService = MediaService();

  @override
  void dispose() {
    _mediaService.dispose();
    _projectController.dispose();
    _authorController.dispose();
    _institutionController.dispose();
    _locationController.dispose();
    _ecosystemController.dispose();
    _observerController.dispose();
    _notesController.dispose();
    _weatherController.dispose();
    _tempController.dispose();
    _humController.dispose();
    _methodController.dispose();
    _spCommonNameController.dispose();
    _spScientificNameController.dispose();
    _spCountController.dispose();
    _spBehaviorController.dispose();
    _spSexAgeController.dispose();
    _spNotesController.dispose();
    super.dispose();
  }

  void _loadSession(SamplingSession session) {
    setState(() {
      _activeSession = session;
      _currentStep = 0;
      _projectController.text = session.projectName;
      _authorController.text = session.author;
      _institutionController.text = session.institution;
      _locationController.text = session.location;
      _ecosystemController.text = session.ecosystem;
      _observerController.text = session.observer;
      _notesController.text = session.notes;
      _weatherController.text = session.weather;
      _tempController.text = session.temperature.toString();
      _humController.text = session.humidity.toString();
      _methodController.text = session.methodology;
      _lat = session.latitude;
      _lon = session.longitude;
      _alt = session.altitude;

      try {
        final List<dynamic> spMaps = jsonDecode(session.speciesJson);
        _speciesList = spMaps.map((m) => SessionSpecies.fromMap(m)).toList();
        final List<dynamic> phMaps = jsonDecode(session.generalPhotosJson);
        _generalPhotos = phMaps.map((m) => SessionPhoto.fromMap(m)).toList();
      } catch (e) {
        _speciesList = [];
        _generalPhotos = [];
      }
    });
  }

  SamplingSession _buildCompiledSession() {
    return (_activeSession ?? SamplingSession()).copyWith(
      projectName: _projectController.text,
      author: _authorController.text,
      institution: _institutionController.text,
      location: _locationController.text,
      ecosystem: _ecosystemController.text,
      observer: _observerController.text,
      notes: _notesController.text,
      weather: _weatherController.text,
      temperature: double.tryParse(_tempController.text) ?? 0.0,
      humidity: double.tryParse(_humController.text) ?? 0.0,
      methodology: _methodController.text,
      latitude: _lat,
      longitude: _lon,
      altitude: _alt,
      speciesJson: jsonEncode(_speciesList.map((s) => s.toMap()).toList()),
      generalPhotosJson: jsonEncode(_generalPhotos.map((p) => p.toMap()).toList()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();

    if (_activeSession == null) {
      return _buildHistoryView(context, provider);
    }

    return Scaffold(
      appBar: AppBar(
        title: Text('Paso ${_currentStep + 1} de 5'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => setState(() => _activeSession = null),
        ),
      ),
      body: Stepper(
        type: StepperType.vertical,
        currentStep: _currentStep,
      onStepContinue: () async {
          if (_currentStep < 4) {
            setState(() => _currentStep++);
          } else {
          await provider.saveSamplingSession(_buildCompiledSession());
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Sesión guardada exitosamente')));
            setState(() => _activeSession = null);
          }
          }
        },
        onStepCancel: () {
          if (_currentStep > 0) {
            setState(() => _currentStep--);
          }
        },
        steps: [
          Step(title: const Text('Dossier del Proyecto'), isActive: _currentStep >= 0, content: _buildDossierStep()),
          Step(title: const Text('Ubicación y Sitio'), isActive: _currentStep >= 1, content: _buildSiteStep()),
          Step(title: const Text('Inventario de Especies'), isActive: _currentStep >= 2, content: _buildSpeciesStep()),
          Step(title: const Text('Registro Fotográfico'), isActive: _currentStep >= 3, content: _buildGalleryStep()),
          Step(title: const Text('Finalizar y Compilar'), isActive: _currentStep >= 4, content: _buildReportStep(provider)),
        ],
      ),
    );
  }

  Widget _buildDossierStep() {
    return Column(
      children: [
        TextField(controller: _projectController, decoration: const InputDecoration(labelText: 'Nombre del Proyecto')),
        TextField(controller: _authorController, decoration: const InputDecoration(labelText: 'Investigador Principal')),
        TextField(controller: _institutionController, decoration: const InputDecoration(labelText: 'Institución')),
      ],
    );
  }

  Widget _buildSiteStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(controller: _locationController, decoration: const InputDecoration(labelText: 'Localidad')),
        TextField(controller: _ecosystemController, decoration: const InputDecoration(labelText: 'Ecosistema')),
        const SizedBox(height: 16),
        const Text('Georreferenciación', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
        Row(
          children: [
            Expanded(child: Text('GPS: ${_lat.toStringAsFixed(4)}, ${_lon.toStringAsFixed(4)} (${_alt.toInt()}m)', style: const TextStyle(fontSize: 11))),
            IconButton(
              icon: const Icon(Icons.my_location, color: Colors.green),
              onPressed: () async {
                final pos = await LocationService.getCurrentLocation();
                if (pos != null) {
                  setState(() {
                    _lat = pos.latitude;
                    _lon = pos.longitude;
                    _alt = pos.altitude;
                  });
                }
              },
            ),
          ],
        ),
        const Divider(),
        const Text('Variables Ambientales', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
        Row(
          children: [
            Expanded(child: TextField(controller: _weatherController, decoration: const InputDecoration(labelText: 'Clima'))),
            const SizedBox(width: 8),
            Expanded(child: TextField(controller: _tempController, decoration: const InputDecoration(labelText: 'Temp (°C)'), keyboardType: TextInputType.number)),
          ],
        ),
        Row(
          children: [
            Expanded(child: TextField(controller: _humController, decoration: const InputDecoration(labelText: 'Humedad (%)'), keyboardType: TextInputType.number)),
            const SizedBox(width: 8),
            Expanded(child: TextField(controller: _methodController, decoration: const InputDecoration(labelText: 'Metodología'))),
          ],
        ),
        const SizedBox(height: 12),
        TextField(controller: _observerController, decoration: const InputDecoration(labelText: 'Observador')),
        TextField(controller: _notesController, decoration: const InputDecoration(labelText: 'Notas Generales'), maxLines: 2),
      ],
    );
  }

  String? _editingFamily;
  void _startEditingSpecies(int index) {
    final sp = _speciesList[index];
    _spCommonNameController.text = sp.commonName;
    _spScientificNameController.text = sp.name;
    _spCountController.text = sp.count.toString();
    _spBehaviorController.text = sp.behavior;
    _spSexAgeController.text = sp.sexAge;
    _spNotesController.text = sp.notes;
    _editingFamily = sp.family;
    setState(() => _editingSpeciesIndex = index);
  }

  void _saveCurrentSpecies() {
    if (_editingSpeciesIndex == null) return;
    final index = _editingSpeciesIndex!;
    final sp = _speciesList[index];
    setState(() {
      _speciesList[index] = SessionSpecies(
        id: sp.id,
        family: _editingFamily ?? sp.family,
        name: _spScientificNameController.text,
        commonName: _spCommonNameController.text,
        count: int.tryParse(_spCountController.text) ?? 1,
        behavior: _spBehaviorController.text,
        sexAge: _spSexAgeController.text,
        notes: _spNotesController.text,
        photos: sp.photos,
        audioPath: sp.audioPath,
      );
      _editingSpeciesIndex = null;
    });
  }

  Widget _buildSpeciesStep() {
    if (_editingSpeciesIndex != null) {
      return _buildSpeciesForm(_editingSpeciesIndex!);
    }

    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Aves Registradas (${_speciesList.length})', style: const TextStyle(fontWeight: FontWeight.bold)),
            ElevatedButton.icon(
              onPressed: () {
                _speciesList.add(SessionSpecies(
                  id: DateTime.now().millisecondsSinceEpoch.toString(),
                  family: SpeciesCatalog.families.first,
                  name: "",
                ));
                _startEditingSpecies(_speciesList.length - 1);
              },
              icon: const Icon(Icons.add, size: 16),
              label: const Text('Agregar'),
            )
          ],
        ),
        const SizedBox(height: 12),
        if (_speciesList.isEmpty)
          const Padding(padding: EdgeInsets.all(20), child: Text('No hay registros', style: TextStyle(color: Colors.grey))),
        ..._speciesList.asMap().entries.map((entry) => _buildSpeciesCard(entry.key, entry.value)),
      ],
    );
  }

  Widget _buildSpeciesCard(int index, SessionSpecies sp) {
    return Card(
      child: ListTile(
        title: Text(sp.commonName.isEmpty ? 'Sin nombre' : sp.commonName),
        subtitle: Text('${sp.name} • ${sp.family}\nCant: ${sp.count} • ${sp.sexAge}', style: const TextStyle(fontSize: 11)),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(icon: const Icon(Icons.edit, size: 18), onPressed: () => _startEditingSpecies(index)),
            IconButton(icon: const Icon(Icons.delete, size: 18, color: Colors.red), onPressed: () => setState(() => _speciesList.removeAt(index))),
          ],
        ),
      ),
    );
  }

  Widget _buildSpeciesForm(int index) {
    final sp = _speciesList[index];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('Editar Especie', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.green)),
            TextButton(onPressed: () => setState(() => _editingSpeciesIndex = null), child: const Text('Cancelar', style: TextStyle(color: Colors.grey, fontSize: 12))),
          ],
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          value: _editingFamily,
          items: SpeciesCatalog.families.map((f) => DropdownMenuItem(value: f, child: Text(f, style: const TextStyle(fontSize: 12)))).toList(),
          onChanged: (v) {
            setState(() {
              _editingFamily = v;
            });
          },
          decoration: const InputDecoration(labelText: 'Familia'),
        ),
        const SizedBox(height: 12),
        _buildAiAssistantBox(index),
        const SizedBox(height: 12),
        TextField(
          decoration: const InputDecoration(labelText: 'Nombre Común'),
          controller: _spCommonNameController,
        ),
        TextField(
          decoration: const InputDecoration(labelText: 'Nombre Científico'),
          controller: _spScientificNameController,
        ),
        Row(
          children: [
            Expanded(child: TextField(decoration: const InputDecoration(labelText: 'Cantidad'), controller: _spCountController, keyboardType: TextInputType.number)),
            const SizedBox(width: 8),
            Expanded(child: TextField(decoration: const InputDecoration(labelText: 'Sexo/Edad'), controller: _spSexAgeController)),
          ],
        ),
        TextField(decoration: const InputDecoration(labelText: 'Comportamiento'), controller: _spBehaviorController),
        TextField(decoration: const InputDecoration(labelText: 'Notas'), controller: _spNotesController, maxLines: 2),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: FilledButton.icon(
                onPressed: () async {
                  final path = await _mediaService.takePhoto();
                  if (path != null) {
                    setState(() {
                      final updatedPhotos = List<SessionPhoto>.from(sp.photos);
                      updatedPhotos.add(SessionPhoto(path: path, name: path.split('/').last, caption: "Foto de ${sp.commonName}"));
                      _speciesList[index] = SessionSpecies(
                        id: sp.id, family: sp.family, name: sp.name, commonName: sp.commonName, count: sp.count, behavior: sp.behavior, photos: updatedPhotos, audioPath: sp.audioPath, notes: sp.notes, sexAge: sp.sexAge
                      );
                    });
                  }
                },
                icon: const Icon(Icons.camera_alt, size: 16),
                label: const Text('Foto'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: _saveCurrentSpecies,
                icon: const Icon(Icons.check, size: 16),
                label: const Text('Guardar'),
              ),
            )
          ],
        )
      ],
    );
  }

  Widget _buildAiAssistantBox(int index) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.green.withOpacity(0.05),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.green.withOpacity(0.2)),
      ),
      child: Column(
        children: [
          Row(
            children: const [
              Icon(Icons.auto_awesome, color: Colors.green, size: 16),
              SizedBox(width: 8),
              Text('Asistente IA', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.green)),
            ],
          ),
          const SizedBox(height: 8),
          const Text('Reconocimiento multimodal de foto y audio.', style: TextStyle(fontSize: 10)),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: SmallButton(
                  onPressed: () => _runAiIdentification(index),
                  label: 'Identificar con IA',
                ),
              ),
            ],
          )
        ],
      ),
    );
  }

  void _runAiIdentification(int index) async {
    final sp = _speciesList[index];
    if (sp.photos.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Toma una foto primero para identificar')));
      return;
    }

    final res = await GeminiService.askGeminiMultimodal(
      "Identifica esta ave de Colombia. Devuelve: Nombre Común|Nombre Científico|Familia|Razón",
      images: [File(sp.photos.first.path)],
    );

    if (res.contains('|')) {
      final parts = res.split('|');
      setState(() {
        final common = parts[0].trim();
        final scientific = parts.length > 1 ? parts[1].trim() : _spScientificNameController.text;
        final family = parts.length > 2 ? parts[2].trim() : _editingFamily;
        final notes = parts.length > 3 ? parts[3].trim() : _spNotesController.text;

        _spCommonNameController.text = common;
        _spScientificNameController.text = scientific;
        _spNotesController.text = notes;
        _editingFamily = family;
      });
    }
  }

  Widget _buildGalleryStep() {
    return Column(
      children: [
        const Text('Fotos Generales del Hábitat', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(child: ElevatedButton.icon(onPressed: () async {
              final path = await _mediaService.takePhoto();
              if (path != null) setState(() => _generalPhotos.add(SessionPhoto(path: path, name: path.split('/').last)));
            }, icon: const Icon(Icons.camera_alt), label: const Text('Cámara'))),
            const SizedBox(width: 12),
            Expanded(child: ElevatedButton.icon(onPressed: () async {
              final path = await _mediaService.pickImageFromGallery();
              if (path != null) setState(() => _generalPhotos.add(SessionPhoto(path: path, name: path.split('/').last)));
            }, icon: const Icon(Icons.photo_library), label: const Text('Galería'))),
          ],
        ),
        const SizedBox(height: 16),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, crossAxisSpacing: 8, mainAxisSpacing: 8),
          itemCount: _generalPhotos.length,
          itemBuilder: (context, idx) => Image.file(File(_generalPhotos[idx].path), fit: BoxFit.cover),
        )
      ],
    );
  }

  Widget _buildReportStep(BirdProvider provider) {
    return Column(
      children: [
        const Icon(Icons.check_circle, color: Colors.green, size: 64),
        const SizedBox(height: 16),
        const Text('Muestreo Listo para Compilar', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 24),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: () async {
              final compiled = _buildCompiledSession();
              await provider.saveSamplingSession(compiled);
              final file = await ReportService.generateSessionZip(
                session: compiled,
                speciesList: _speciesList,
                generalPhotos: _generalPhotos,
              );
              Share.shareXFiles([XFile(file.path)], text: 'Paquete de Muestreo');
            },
            icon: const Icon(Icons.download),
            label: const Text('Guardar y Generar PDF + ZIP'),
          ),
        )
      ],
    );
  }

  Widget _buildHistoryView(BuildContext context, BirdProvider provider) {
    final sessions = provider.sessions;
    return Scaffold(
      body: sessions.isEmpty
          ? const Center(child: Text('No hay sesiones registradas'))
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: sessions.length,
              itemBuilder: (context, index) => _buildSessionCard(context, provider, sessions[index]),
            ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          setState(() {
            _activeSession = SamplingSession(
              projectName: 'Muestreo ${DateFormat('MMM yyyy').format(DateTime.now())}',
              date: DateFormat('yyyy-MM-dd').format(DateTime.now()),
            );
            _currentStep = 0;
            _speciesList = [];
            _generalPhotos = [];
            _projectController.text = _activeSession!.projectName;
            _authorController.text = "";
            _institutionController.text = "";
            _locationController.text = "";
            _ecosystemController.text = "";
            _observerController.text = "";
            _notesController.text = "";
            _weatherController.text = "";
            _tempController.text = "0";
            _humController.text = "0";
            _methodController.text = "";
            _lat = 0;
            _lon = 0;
            _alt = 0;
          });
        },
        label: const Text('Nueva Sesión'),
        icon: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildSessionCard(BuildContext context, BirdProvider provider, SamplingSession session) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: () => _showSessionSummary(context, session),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(session.projectName, style: const TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Text('Fecha: ${session.date}\nObservador: ${session.observer}', style: const TextStyle(fontSize: 12)),
                trailing: const Icon(Icons.info_outline, color: Colors.green),
              ),
              const Divider(),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    onPressed: () => _showDownloadMenu(context, session),
                    icon: const Icon(Icons.download, size: 18),
                    label: const Text('Exportar', style: TextStyle(fontSize: 12)),
                  ),
                  TextButton.icon(
                    onPressed: () => _loadSession(session),
                    icon: const Icon(Icons.edit, size: 18),
                    label: const Text('Editar', style: TextStyle(fontSize: 12)),
                  ),
                  IconButton(
                    icon: const Icon(Icons.delete, color: Colors.red, size: 18),
                    onPressed: () => provider.deleteSamplingSession(session.id!),
                  ),
                ],
              )
            ],
          ),
        ),
      ),
    );
  }

  void _showSessionSummary(BuildContext context, SamplingSession session) {
    List<SessionSpecies> sps = [];
    try { sps = (jsonDecode(session.speciesJson) as List).map((e) => SessionSpecies.fromMap(e)).toList(); } catch (_) {}

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (context) => DraggableScrollableSheet(
        initialChildSize: 0.6,
        maxChildSize: 0.9,
        expand: false,
        builder: (_, scrollController) => ListView(
          controller: scrollController,
          padding: const EdgeInsets.all(20),
          children: [
            Center(child: Container(width: 40, height: 4, decoration: BoxDecoration(color: Colors.grey, borderRadius: BorderRadius.all(Radius.circular(2))))),
            const SizedBox(height: 20),
            Text(session.projectName, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            _summaryRow(Icons.person, 'Autor', session.author),
            _summaryRow(Icons.business, 'Institución', session.institution),
            _summaryRow(Icons.location_on, 'Ubicación', session.location),
            _summaryRow(Icons.eco, 'Ecosistema', session.ecosystem),
            const Divider(height: 30),
            Text('Especies Registradas (${sps.length})', style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            ...sps.map((s) => ListTile(
              dense: true,
              title: Text(s.commonName),
              subtitle: Text('${s.name} (${s.family})'),
              trailing: Text('Cant: ${s.count}'),
            )),
            const SizedBox(height: 20),
            Text('Notas: ${session.notes}', style: const TextStyle(fontSize: 12, fontStyle: FontStyle.italic)),
          ],
        ),
      ),
    );
  }

  Widget _summaryRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        children: [
          Icon(icon, size: 16, color: Colors.green),
          const SizedBox(width: 8),
          Text('$label: ', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
          Expanded(child: Text(value, style: const TextStyle(fontSize: 13))),
        ],
      ),
    );
  }

  void _showDownloadMenu(BuildContext context, SamplingSession session) {
    showModalBottomSheet(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const ListTile(title: Text('Exportar Datos de Sesión', style: TextStyle(fontWeight: FontWeight.bold))),
            ListTile(
              leading: const Icon(Icons.picture_as_pdf, color: Colors.red),
              title: const Text('Documento PDF'),
              onTap: () async {
                Navigator.pop(context);
                final sps = (jsonDecode(session.speciesJson) as List).map((e) => SessionSpecies.fromMap(e)).toList();
                final file = await ReportService.exportSessionToPdf(session, sps);
                Share.shareXFiles([XFile(file.path)], text: 'Reporte PDF');
              },
            ),
            ListTile(
              leading: const Icon(Icons.grid_on, color: Colors.green),
              title: const Text('Excel / CSV'),
              onTap: () async {
                Navigator.pop(context);
                final sps = (jsonDecode(session.speciesJson) as List).map((e) => SessionSpecies.fromMap(e)).toList();
                final file = await ReportService.exportSessionToCsv(session, sps);
                Share.shareXFiles([XFile(file.path)], text: 'Datos CSV');
              },
            ),
            ListTile(
              leading: const Icon(Icons.archive, color: Colors.blue),
              title: const Text('Paquete ZIP (Todo)'),
              onTap: () async {
                Navigator.pop(context);
                final sps = (jsonDecode(session.speciesJson) as List).map((e) => SessionSpecies.fromMap(e)).toList();
                final photos = (jsonDecode(session.generalPhotosJson) as List).map((e) => SessionPhoto.fromMap(e)).toList();
                final file = await ReportService.generateSessionZip(session: session, speciesList: sps, generalPhotos: photos);
                Share.shareXFiles([XFile(file.path)], text: 'Paquete Completo');
              },
            ),
          ],
        ),
      ),
    );
  }
}

class SmallButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String label;
  const SmallButton({super.key, required this.onPressed, required this.label});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 30,
      child: OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 8)),
        child: Text(label, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
      ),
    );
  }
}
