import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../providers/bird_provider.dart';
import '../../models/models.dart';

class SiteDetailScreen extends StatelessWidget {
  const SiteDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();
    final site = provider.selectedSite;
    final samplings = provider.selectedSiteSamplings;

    if (site == null) return const Scaffold(body: Center(child: Text('No hay sitio seleccionado')));

    return Scaffold(
      appBar: AppBar(title: Text(site.name)),
      body: Column(
        children: [
          _buildSiteInfo(context, site),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Campañas de Muestreo', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                Text('${samplings.length} sesiones', style: const TextStyle(fontSize: 12, color: Colors.grey)),
              ],
            ),
          ),
          Expanded(
            child: samplings.isEmpty
                ? _buildEmptySamplings()
                : ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: samplings.length,
                    itemBuilder: (context, index) => _buildSamplingCard(context, provider, samplings[index]),
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddSamplingDialog(context, provider),
        child: const Icon(Icons.playlist_add),
      ),
    );
  }

  Widget _buildSiteInfo(BuildContext context, Site site) {
    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.terrain, color: Colors.green),
                const SizedBox(width: 8),
                Text(site.name.toUpperCase(), style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 8),
            Text('📍 Ubicación: Vereda ${site.vereda}, ${site.municipality}', style: const TextStyle(fontSize: 12)),
            Text('🌿 Ecosistema: ${site.ecosystem}', style: const TextStyle(fontSize: 12)),
            Text('🛰️ Lat/Lon: ${site.latitude.toStringAsFixed(5)}, ${site.longitude.toStringAsFixed(5)}',
              style: const TextStyle(fontSize: 11, fontFamily: 'monospace')),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptySamplings() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: const [
          Icon(Icons.playlist_add_check, size: 54, color: Colors.grey),
          SizedBox(height: 10),
          Text('No hay campañas registradas'),
          Text('Presiona + para iniciar una sesión.', style: TextStyle(color: Colors.grey, fontSize: 11)),
        ],
      ),
    );
  }

  Widget _buildSamplingCard(BuildContext context, BirdProvider provider, Sampling sampling) {
    final date = DateFormat('dd/MM/yyyy HH:mm').format(DateTime.fromMillisecondsSinceEpoch(sampling.date));
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        title: Text('Sesión: $date', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
        subtitle: Text('Observador: ${sampling.observer}\nMétodo: ${sampling.methodology}', style: const TextStyle(fontSize: 11)),
        trailing: const Icon(Icons.chevron_right),
        onTap: () {
          provider.selectSampling(sampling);
          // Navigate to observations list (AddObservationScreen in plan 12)
        },
      ),
    );
  }

  void _showAddSamplingDialog(BuildContext context, BirdProvider provider) {
    final observerController = TextEditingController();
    String weather = 'Soleado';
    double temp = 20;
    double hum = 60;
    String method = 'Observación Directa';

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Text('Nueva Campaña'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(controller: observerController, decoration: const InputDecoration(labelText: 'Observador')),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  value: weather,
                  items: ['Soleado', 'Nublado', 'Lluvia', 'Viento'].map((w) => DropdownMenuItem(value: w, child: Text(w))).toList(),
                  onChanged: (v) => setState(() => weather = v!),
                  decoration: const InputDecoration(labelText: 'Clima'),
                ),
                const SizedBox(height: 16),
                Text('Temp: ${temp.toInt()}°C'),
                Slider(value: temp, min: 0, max: 45, onChanged: (v) => setState(() => temp = v)),
                Text('Hum: ${hum.toInt()}%'),
                Slider(value: hum, min: 0, max: 100, onChanged: (v) => setState(() => hum = v)),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
            TextButton(
              onPressed: () {
                if (observerController.text.isNotEmpty) {
                  provider.startSampling(observerController.text, weather, temp, hum, method);
                  Navigator.pop(context);
                }
              },
              child: const Text('Comenzar')
            ),
          ],
        ),
      ),
    );
  }
}
