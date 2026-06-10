import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../providers/bird_provider.dart';
import '../../models/models.dart';
import 'project_detail_screen.dart';

class ProjectListScreen extends StatelessWidget {
  const ProjectListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();
    final projects = provider.projects;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Proyectos de Avifauna', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
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
        onTap: () {
          context.read<BirdProvider>().selectProject(project);
          Navigator.push(context, MaterialPageRoute(builder: (_) => const ProjectDetailScreen()));
        },
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
              const SizedBox(height: 8),
              Text(project.description, style: const TextStyle(fontSize: 12, fontStyle: FontStyle.italic), maxLines: 2, overflow: TextOverflow.ellipsis),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Creado: ${DateFormat('yyyy-MM-dd').format(DateTime.fromMillisecondsSinceEpoch(project.createdAt))}',
                    style: const TextStyle(fontSize: 10, color: Colors.grey)),
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
