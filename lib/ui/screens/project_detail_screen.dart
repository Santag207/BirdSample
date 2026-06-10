import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';
import '../../providers/bird_provider.dart';
import '../../models/models.dart';
import '../../services/report_service.dart';
import '../../services/location_service.dart';
import 'site_detail_screen.dart';

class ProjectDetailScreen extends StatelessWidget {
  const ProjectDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();
    final project = provider.selectedProject;
    final sites = provider.selectedProjectSites;

    if (project == null) return const Scaffold(body: Center(child: Text('No hay proyecto seleccionado')));

    return Scaffold(
      appBar: AppBar(
        title: Text(project.name),
        actions: [
          IconButton(
            icon: const Icon(Icons.share),
            onPressed: () => _showExportMenu(context, provider),
          )
        ],
      ),
      body: Column(
        children: [
          _buildProjectHeader(context, project),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Sitios de Muestreo', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                Text('${sites.length} sitios', style: const TextStyle(fontSize: 12, color: Colors.grey)),
              ],
            ),
          ),
          Expanded(
            child: sites.isEmpty
                ? _buildEmptySites()
                : ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: sites.length,
                    itemBuilder: (context, index) => _buildSiteCard(context, provider, sites[index]),
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddSiteDialog(context, provider),
        child: const Icon(Icons.add_location),
      ),
    );
  }

  Widget _buildProjectHeader(BuildContext context, Project project) {
    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(project.name.toUpperCase(), style: TextStyle(fontWeight: FontWeight.bold, color: Theme.of(context).colorScheme.primary)),
            const SizedBox(height: 8),
            Text('Cliente: ${project.client}', style: const TextStyle(fontSize: 12)),
            Text(project.description, style: const TextStyle(fontSize: 12), maxLines: 3),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptySites() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: const [
          Icon(Icons.add_location, size: 54, color: Colors.grey),
          SizedBox(height: 10),
          Text('No hay sitios biológicos', style: TextStyle(fontWeight: FontWeight.bold)),
          Text('Añade coordenadas de un sector de muestreo.', style: TextStyle(color: Colors.grey, fontSize: 11)),
        ],
      ),
    );
  }

  Widget _buildSiteCard(BuildContext context, BirdProvider provider, Site site) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      color: Theme.of(context).colorScheme.surfaceVariant.withOpacity(0.3),
      child: ListTile(
        leading: const Icon(Icons.pin_drop, color: Colors.green),
        title: Text(site.name, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text('${site.municipality}, ${site.department}\nAltitud: ${site.altitude.toInt()}m', style: const TextStyle(fontSize: 11)),
        trailing: IconButton(
          icon: const Icon(Icons.delete, size: 18, color: Colors.red),
          onPressed: () => provider.deleteSite(site.id!),
        ),
        onTap: () {
          provider.selectSite(site);
          Navigator.push(context, MaterialPageRoute(builder: (_) => const SiteDetailScreen()));
        },
      ),
    );
  }

  void _showExportMenu(BuildContext context, BirdProvider provider) {
    showModalBottomSheet(
      context: context,
      builder: (context) => Wrap(
        children: [
          ListTile(
            leading: const Icon(Icons.picture_as_pdf, color: Colors.red),
            title: const Text('Exportar PDF'),
            onTap: () async {
              Navigator.pop(context);
              final file = await ReportService.exportProjectToPdf(
                project: provider.selectedProject!,
                sites: provider.selectedProjectSites,
                samplings: [], // Porting needed to get all samplings
                observations: provider.allObservations, // simplified
              );
              Share.shareXFiles([XFile(file.path)], text: 'Informe PDF');
            },
          ),
          ListTile(
            leading: const Icon(Icons.grid_on, color: Colors.green),
            title: const Text('Exportar CSV'),
            onTap: () async {
              Navigator.pop(context);
              final file = await ReportService.exportProjectToCsv(
                project: provider.selectedProject!,
                sites: provider.selectedProjectSites,
                samplings: [],
                observations: provider.allObservations,
              );
              Share.shareXFiles([XFile(file.path)], text: 'Reporte CSV');
            },
          ),
        ],
      ),
    );
  }

  void _showAddSiteDialog(BuildContext context, BirdProvider provider) async {
    final nameController = TextEditingController();
    final deptController = TextEditingController(text: 'Cundinamarca');
    final munController = TextEditingController(text: 'Bogotá');
    final veredaController = TextEditingController();
    final ecoController = TextEditingController(text: 'Bosque de Niebla');

    double lat = 0, lon = 0, alt = 0;
    final pos = await LocationService.getCurrentLocation();
    if (pos != null) {
      lat = pos.latitude;
      lon = pos.longitude;
      alt = pos.altitude;
    }

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Nuevo Sitio'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: nameController, decoration: const InputDecoration(labelText: 'Nombre')),
              TextField(controller: deptController, decoration: const InputDecoration(labelText: 'Departamento')),
              TextField(controller: munController, decoration: const InputDecoration(labelText: 'Municipio')),
              TextField(controller: veredaController, decoration: const InputDecoration(labelText: 'Vereda')),
              TextField(controller: ecoController, decoration: const InputDecoration(labelText: 'Ecosistema')),
              const SizedBox(height: 10),
              Text('GPS: $lat, $lon (${alt.toInt()}m)', style: const TextStyle(fontSize: 10, color: Colors.grey)),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(
            onPressed: () {
              if (nameController.text.isNotEmpty) {
                provider.createSite(nameController.text, deptController.text, munController.text, veredaController.text, ecoController.text, lat, lon, alt);
                Navigator.pop(context);
              }
            },
            child: const Text('Guardar')
          ),
        ],
      ),
    );
  }
}
