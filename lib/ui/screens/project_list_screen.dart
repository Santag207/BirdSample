import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:share_plus/share_plus.dart';
import '../../providers/bird_provider.dart';
import '../../models/models.dart';
import '../../services/report_service.dart';
import 'project_detail_screen.dart';

class ProjectListScreen extends StatelessWidget {
  const ProjectListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();
    final projects = provider.projects;

    return Scaffold(
      body: projects.isEmpty
          ? _buildEmptyState()
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: projects.length,
              itemBuilder: (context, index) => _buildProjectCard(context, projects[index]),
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddProjectDialog(context, provider),
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildEmptyState() {
    return const Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.folder_open, size: 72, color: Colors.grey),
          SizedBox(height: 16),
          Text('No hay proyectos registrados', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 24),
            child: Text('Crea un proyecto para empezar a recolectar datos.',
              textAlign: TextAlign.center, style: TextStyle(color: Colors.grey, fontSize: 12)),
          ),
        ],
      ),
    );
  }

  Widget _buildProjectCard(BuildContext context, Project project) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: () => _showProjectSummary(context, project),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(project.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16), maxLines: 1, overflow: TextOverflow.ellipsis),
                  ),
                  _buildStatusChip(project.status),
                ],
              ),
              const SizedBox(height: 4),
              Text('Cliente: ${project.client}', style: const TextStyle(fontSize: 12, color: Colors.black87)),
              Text('Contrato: ${project.contractNumber}', style: const TextStyle(fontSize: 11, color: Colors.grey)),
              const Divider(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    onPressed: () => _showExportMenu(context, project),
                    icon: const Icon(Icons.download, size: 18),
                    label: const Text('Exportar', style: TextStyle(fontSize: 12)),
                  ),
                  TextButton.icon(
                    onPressed: () {
                      context.read<BirdProvider>().selectProject(project);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => const ProjectDetailScreen()));
                    },
                    icon: const Icon(Icons.edit, size: 18),
                    label: const Text('Editar', style: TextStyle(fontSize: 12)),
                  ),
                  IconButton(
                    icon: const Icon(Icons.delete, color: Colors.red, size: 18),
                    onPressed: () => context.read<BirdProvider>().deleteProject(project.id!),
                  )
                ],
              )
            ],
          ),
        ),
      ),
    );
  }

  void _showProjectSummary(BuildContext context, Project project) async {
    final provider = context.read<BirdProvider>();
    final data = await provider.getProjectFullData(project.id!);
    final List<Site> sites = data['sites'];
    final List<Observation> obs = data['observations'];

    if (!context.mounted) return;

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
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text(project.name, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            _summaryRow(Icons.person, 'Cliente', project.client),
            _summaryRow(Icons.description, 'Contrato', project.contractNumber),
            _summaryRow(Icons.calendar_today, 'Creado', DateFormat('yyyy-MM-dd').format(DateTime.fromMillisecondsSinceEpoch(project.createdAt))),
            _summaryRow(Icons.place, 'Sitios', sites.length.toString()),
            _summaryRow(Icons.visibility, 'Observaciones', obs.length.toString()),
            const Divider(height: 30),
            const Text('Descripción', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Text(project.description.isEmpty ? "Sin descripción" : project.description, style: const TextStyle(fontSize: 13)),
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

  void _showExportMenu(BuildContext context, Project project) {
    showModalBottomSheet(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const ListTile(title: Text('Exportar Datos del Proyecto', style: TextStyle(fontWeight: FontWeight.bold))),
            ListTile(
              leading: const Icon(Icons.picture_as_pdf, color: Colors.red),
              title: const Text('Documento PDF'),
              onTap: () async {
                Navigator.pop(context);
                final provider = context.read<BirdProvider>();
                final data = await provider.getProjectFullData(project.id!);
                final file = await ReportService.exportProjectToPdf(
                  project: project,
                  sites: data['sites'],
                  samplings: data['samplings'],
                  observations: data['observations'],
                );
                Share.shareXFiles([XFile(file.path)], text: 'Informe PDF del Proyecto');
              },
            ),
            ListTile(
              leading: const Icon(Icons.grid_on, color: Colors.green),
              title: const Text('Excel / CSV'),
              onTap: () async {
                Navigator.pop(context);
                final provider = context.read<BirdProvider>();
                final data = await provider.getProjectFullData(project.id!);
                final file = await ReportService.exportProjectToCsv(
                  project: project,
                  sites: data['sites'],
                  samplings: data['samplings'],
                  observations: data['observations'],
                );
                Share.shareXFiles([XFile(file.path)], text: 'Datos CSV del Proyecto');
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusChip(String status) {
    final color = status == "Sincronizado" ? Colors.green : Colors.orange;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(status, style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: color)),
    );
  }

  void _showAddProjectDialog(BuildContext context, BirdProvider provider) {
    final nameController = TextEditingController();
    final clientController = TextEditingController();
    final contractController = TextEditingController();
    final descController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Nuevo Proyecto'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: nameController, decoration: const InputDecoration(labelText: 'Nombre')),
              TextField(controller: clientController, decoration: const InputDecoration(labelText: 'Cliente')),
              TextField(controller: contractController, decoration: const InputDecoration(labelText: 'Contrato')),
              TextField(controller: descController, decoration: const InputDecoration(labelText: 'Descripción')),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(
            onPressed: () {
              if (nameController.text.isNotEmpty) {
                provider.createProject(nameController.text, clientController.text, contractController.text, descController.text);
                Navigator.pop(context);
              }
            },
            child: const Text('Crear')
          ),
        ],
      ),
    );
  }
}
