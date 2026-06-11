import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../providers/bird_provider.dart';

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

    return Scaffold(
      appBar: AppBar(title: const Text('Sincronización Cloud')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Central de Sincronización', style: TextStyle(fontWeight: FontWeight.w900, fontSize: 18)),
            const Text('Carga tus datos de forma incremental.', style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 16),
            _buildGoogleAccount(provider),
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

  Widget _buildGoogleAccount(BirdProvider provider) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Colors.red.shade100,
          child: const Icon(Icons.account_circle, color: Colors.red),
        ),
        title: Text(provider.googleUser?.displayName ?? 'No autenticado'),
        subtitle: Text(provider.googleUser?.email ?? 'Inicia sesión con Google para sincronizar'),
        trailing: provider.googleUser == null
            ? TextButton(onPressed: () => provider.loginWithGoogle(), child: const Text('Conectar'))
            : TextButton(onPressed: () => provider.logoutFromGoogle(), child: const Text('Salir')),
      ),
    );
  }

  Widget _buildSyncControl(BuildContext context, BirdProvider provider) {
    final theme = Theme.of(context);
    final bool isReady = provider.selectedProject != null && provider.googleUser != null;

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
                  decoration: BoxDecoration(
                    color: provider.googleUser != null ? Colors.green.shade50 : Colors.red.shade50,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    provider.googleUser != null ? 'VINCULADO' : 'DESCONECTADO',
                    style: TextStyle(
                      fontSize: 9,
                      fontWeight: FontWeight.bold,
                      color: provider.googleUser != null ? Colors.green : Colors.red,
                    ),
                  ),
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
                  onPressed: !isReady ? null : () => provider.performIncrementalSync(),
                  icon: const Icon(Icons.sync),
                  label: const Text('Iniciar Sincronización'),
                ),
              ),
            if (provider.selectedProject == null)
              const Padding(
                padding: EdgeInsets.only(top: 8.0),
                child: Text('⚠️ Selecciona un proyecto en Muestreos.', style: TextStyle(fontSize: 10, color: Colors.orange)),
              ),
            if (provider.googleUser == null)
              const Padding(
                padding: EdgeInsets.only(top: 4.0),
                child: Text('⚠️ Debes iniciar sesión con Google.', style: TextStyle(fontSize: 10, color: Colors.red)),
              ),
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
