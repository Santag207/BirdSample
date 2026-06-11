import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/bird_provider.dart';
import '../../services/gemini_service.dart';

class GeminiAssistantScreen extends StatefulWidget {
  const GeminiAssistantScreen({super.key});

  @override
  State<GeminiAssistantScreen> createState() => _GeminiAssistantScreenState();
}

class _GeminiAssistantScreenState extends State<GeminiAssistantScreen> {
  final _feathersController = TextEditingController();
  final _beakController = TextEditingController();
  final _habitatController = TextEditingController();

  @override
  void dispose() {
    _feathersController.dispose();
    _beakController.dispose();
    _habitatController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Asistente Ornitólogo IA')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildAiBanner(theme),
            const SizedBox(height: 24),
            const Text('Describir Características', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
            const Text('Ingresa datos del ave para deducir su familia.', style: TextStyle(fontSize: 11, color: Colors.grey)),
            const SizedBox(height: 12),
            _buildTextField(_feathersController, 'Plumaje y Patrones'),
            _buildTextField(_beakController, 'Tipo de Pico y Cuerpo'),
            _buildTextField(_habitatController, 'Hábitat local'),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: FilledButton.icon(
                    onPressed: provider.aiLoading ? null : () {
                      provider.searchBirdWithGemini(_feathersController.text, _beakController.text, _habitatController.text);
                    },
                    icon: const Icon(Icons.psychology, size: 18),
                    label: const Text('Identificar', style: TextStyle(fontSize: 12)),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: provider.aiLoading ? null : () {
                      // Generate Summary logic
                    },
                    icon: const Icon(Icons.menu_book, size: 18),
                    label: const Text('Resumen', style: TextStyle(fontSize: 12)),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            const Text('Diagnóstico Ornitológico:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
            const SizedBox(height: 8),
            _buildResponseBox(provider),
          ],
        ),
      ),
    );
  }

  Widget _buildAiBanner(ThemeData theme) {
    return Card(
      color: theme.colorScheme.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            Icon(Icons.auto_awesome, color: theme.colorScheme.primary, size: 36),
            const SizedBox(width: 14),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Virtual Ornitologist Gemini', style: TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
                  Text('Basado en redes neuronales de Google.', style: TextStyle(fontSize: 11)),
                ],
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget _buildTextField(TextEditingController controller, String label) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: TextField(
        controller: controller,
        decoration: InputDecoration(
          labelText: label,
          isDense: true,
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
        ),
        style: const TextStyle(fontSize: 13),
      ),
    );
  }

  Widget _buildResponseBox(BirdProvider provider) {
    return Container(
      width: double.infinity,
      constraints: const BoxConstraints(minHeight: 180),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: provider.aiLoading
          ? const Center(child: CircularProgressIndicator())
          : SelectableText(
              provider.aiResponse.isEmpty ? 'Completa la descripción para ver el diagnóstico.' : provider.aiResponse,
              style: const TextStyle(fontSize: 12, height: 1.5),
            ),
    );
  }
}
