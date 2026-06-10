import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../providers/bird_provider.dart';
import '../../models/models.dart';

class SyncScreen extends StatefulWidget {
  const SyncScreen({super.key});

  @override
  State<SyncScreen> createState() => _SyncScreenState();
}

class _SyncScreenState extends State<SyncScreen> {
  String _conflictRule = 'T_WIN';

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Sincronización Cloud')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Central de Sincronización', style: TextStyle(fontWeight: FontWeight.black, fontSize: 18)),
            const Text('Carga tus datos de forma incremental.', style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 16),
            _buildSyncControl(context, provider),
            const SizedBox(height: 24),
            const Text('Historial de Transacciones', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
            const SizedBox(height: 8),
            _buildSyncLogs(provider),
          ],
        ),
      ),
    );
  }

  Widget _buildSyncControl(BuildContext context, BirdProvider provider) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(Icons.cloud_queue, color: theme.colorScheme.primary),
                    const SizedBox(width: 10),
                    const Text('Estado del Motor Sync', style: TextStyle(fontWeight: FontWeight.bold)),
                  ],
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(color: Colors.green.shade50, borderRadius: BorderRadius.circular(20)),
                  child: const Text('VINCULADO', style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: Colors.green)),
                )
              ],
            ),
            const SizedBox(height: 16),
            _buildConflictOption('El timestamp más reciente gana', 'T_WIN'),
            _buildConflictOption('Subir ambos en paralelo', 'BOTH'),
            const SizedBox(height: 16),
            if (provider.isSyncing)
              Column(
                children: [
                  LinearProgressIndicator(value: provider.syncProgress),
                  const SizedBox(height: 8),
                  Text(provider.syncLogMessage, style: const TextStyle(fontSize: 11)),
                ],
              )
            else
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: provider.selectedProject == null ? null : () => provider.performIncrementalSync(),
                  icon: const Icon(Icons.sync),
                  label: const Text('Iniciar Sincronización'),
                ),
              ),
            if (provider.selectedProject == null)
              const Padding(
                padding: EdgeInsets.only(top: 8.0),
                child: Text('Selecciona un proyecto en Muestreos para sincronizar.', style: TextStyle(fontSize: 10, color: Colors.red)),
              )
          ],
        ),
      ),
    );
  }

  Widget _buildConflictOption(String title, String value) {
    return RadioListTile<String>(
      title: Text(title, style: const TextStyle(fontSize: 12)),
      value: value,
      groupValue: _conflictRule,
      onChanged: (v) => setState(() => _conflictRule = v!),
      contentPadding: EdgeInsets.zero,
      dense: true,
    );
  }

  Widget _buildSyncLogs(BirdProvider provider) {
    if (provider.syncLogs.isEmpty) {
      return const Text('No hay registros previos.', style: TextStyle(fontSize: 11, fontStyle: FontStyle.italic, color: Colors.grey));
    }
    return Column(
      children: provider.syncLogs.map((log) => Card(
        color: Colors.grey.shade50,
        elevation: 0,
        margin: const EdgeInsets.only(bottom: 8),
        child: ListTile(
          leading: Icon(log.status == 'EXITOSO' ? Icons.cloud_done : Icons.cloud_off,
            color: log.status == 'EXITOSO' ? Colors.green : Colors.red),
          title: Text(log.action, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
          subtitle: Text('${log.summary}\n${DateFormat('dd/MM/yyyy HH:mm').format(DateTime.fromMillisecondsSinceEpoch(log.timestamp))}',
            style: const TextStyle(fontSize: 11)),
        ),
      )).toList(),
    );
  }
}
