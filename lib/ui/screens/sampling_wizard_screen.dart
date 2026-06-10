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

  // Controllers
  final _projectController = TextEditingController();
  final _authorController = TextEditingController();
  final _institutionController = TextEditingController();
  final _locationController = TextEditingController();
  final _ecosystemController = TextEditingController();
  final _observerController = TextEditingController();
  final _notesController = TextEditingController();

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
    return _activeSession!.copyWith(
      projectName: _projectController.text,
      author: _authorController.text,
      institution: _institutionController.text,
      location: _locationController.text,
      ecosystem: _ecosystemController.text,
      observer: _observerController.text,
      notes: _notesController.text,
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
        type: StepperType.horizontal,
        currentStep: _currentStep,
        onStepContinue: () {
          if (_currentStep < 4) {
            setState(() => _currentStep++);
          } else {
            provider.saveSamplingSession(_buildCompiledSession());
            setState(() => _activeSession = null);
          }
        },
        onStepCancel: () {
          if (_currentStep > 0) {
            setState(() => _currentStep--);
          }
        },
        steps: [
          Step(title: const Text('Dossier'), isActive: _currentStep >= 0, content: _buildDossierStep()),
          Step(title: const Text('Sitio'), isActive: _currentStep >= 1, content: _buildSiteStep()),
          Step(title: const Text('Especies'), isActive: _currentStep >= 2, content: _buildSpeciesStep()),
          Step(title: const Text('Galería'), isActive: _currentStep >= 3, content: _buildGalleryStep()),
          Step(title: const Text('Reporte'), isActive: _currentStep >= 4, content: _buildReportStep()),
        ],
      ),
    );
  }

  // --- UI STEPS ---

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
      children: [
        TextField(controller: _locationController, decoration: const InputDecoration(labelText: 'Localidad')),
        TextField(controller: _ecosystemController, decoration: const InputDecoration(labelText: 'Ecosistema')),
        const SizedBox(height: 12),
        TextField(controller: _observerController, decoration: const InputDecoration(labelText: 'Observador')),
        TextField(controller: _notesController, decoration: const InputDecoration(labelText: 'Notas Generales'), maxLines: 2),
      ],
    );
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
                setState(() {
                  _speciesList.add(SessionSpecies(
                    id: DateTime.now().millisecondsSinceEpoch.toString(),
                    family: SpeciesCatalog.families.first,
                    name: "",
                  ));
                  _editingSpeciesIndex = _speciesList.length - 1;
                });
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
        subtitle: Text('${sp.name} • ${sp.family}', style: const TextStyle(fontSize: 11)),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(icon: const Icon(Icons.edit, size: 18), onPressed: () => setState(() => _editingSpeciesIndex = index)),
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
        const Text('Editar Especie', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.green)),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          value: sp.family,
          items: SpeciesCatalog.families.map((f) => DropdownMenuItem(value: f, child: Text(f, style: const TextStyle(fontSize: 12)))).toList(),
          onChanged: (v) => setState(() => _speciesList[index] = SessionSpecies(
            id: sp.id, family: v!, name: "", commonName: "", count: sp.count, behavior: sp.behavior, photos: sp.photos, audioPath: sp.audioPath, notes: sp.notes, sexAge: sp.sexAge
          )),
          decoration: const InputDecoration(labelText: 'Familia'),
        ),
        const SizedBox(height: 12),
        _buildAiAssistantBox(index),
        const SizedBox(height: 12),
        TextField(
          decoration: const InputDecoration(labelText: 'Nombre Común'),
          onChanged: (v) => _speciesList[index] = SessionSpecies(
            id: sp.id, family: sp.family, name: sp.name, commonName: v, count: sp.count, behavior: sp.behavior, photos: sp.photos, audioPath: sp.audioPath, notes: sp.notes, sexAge: sp.sexAge
          ),
          controller: TextEditingController(text: sp.commonName)..selection = TextSelection.collapsed(offset: sp.commonName.length),
        ),
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
                onPressed: () => setState(() => _editingSpeciesIndex = null),
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

    // Simulate AI Call
    final res = await GeminiService.askGeminiMultimodal(
      "Identifica esta ave de Colombia. Devuelve: Nombre Común|Nombre Científico|Familia|Razón",
      images: [File(sp.photos.first.path)],
    );

    if (res.contains('|')) {
      final parts = res.split('|');
      setState(() {
        _speciesList[index] = SessionSpecies(
          id: sp.id,
          family: parts.length > 2 ? parts[2].trim() : sp.family,
          name: parts.length > 1 ? parts[1].trim() : sp.name,
          commonName: parts[0].trim(),
          notes: parts.length > 3 ? parts[3].trim() : sp.notes,
          count: sp.count, behavior: sp.behavior, photos: sp.photos, audioPath: sp.audioPath, sexAge: sp.sexAge
        );
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

  Widget _buildReportStep() {
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
              final file = await ReportService.generateSessionZip(
                session: compiled,
                speciesList: _speciesList,
                generalPhotos: _generalPhotos,
              );
              Share.shareXFiles([XFile(file.path)], text: 'Paquete de Muestreo');
            },
            icon: const Icon(Icons.download),
            label: const Text('Generar PDF + ZIP'),
          ),
        )
      ],
    );
  }

  Widget _buildHistoryView(BuildContext context, BirdProvider provider) {
    final sessions = provider.sessions;
    return Scaffold(
      appBar: AppBar(title: const Text('Sesiones de Campo')),
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
      child: ListTile(
        title: Text(session.projectName),
        subtitle: Text('Fecha: ${session.date}\nObservador: ${session.observer}'),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(icon: const Icon(Icons.delete, color: Colors.red, size: 18), onPressed: () => provider.deleteSamplingSession(session.id!)),
            const Icon(Icons.chevron_right),
          ],
        ),
        onTap: () => _loadSession(session),
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
