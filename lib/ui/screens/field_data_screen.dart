import 'package:flutter/material.dart';
import 'project_list_screen.dart';
import 'sampling_wizard_screen.dart';

class FieldDataScreen extends StatelessWidget {
  const FieldDataScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Gestión de Datos de Campo', style: TextStyle(fontWeight: FontWeight.bold)),
          bottom: const TabBar(
            tabs: [
              Tab(icon: Icon(Icons.assignment), text: 'Proyectos'),
              Tab(icon: Icon(Icons.bolt), text: 'Sesiones Rápidas'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            ProjectListScreen(),
            SamplingWizardScreen(),
          ],
        ),
      ),
    );
  }
}
