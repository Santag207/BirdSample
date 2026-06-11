import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/bird_provider.dart';
import '../../models/models.dart';

class OfflineMapScreen extends StatefulWidget {
  const OfflineMapScreen({super.key});

  @override
  State<OfflineMapScreen> createState() => _OfflineMapScreenState();
}

class _OfflineMapScreenState extends State<OfflineMapScreen> {
  Offset _panOffset = Offset.zero;
  bool _isMapDownloaded = false;

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Mapa Local Offline')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            _buildStatusHeader(theme),
            const SizedBox(height: 12),
            Expanded(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: GestureDetector(
                  onPanUpdate: (details) {
                    setState(() {
                      _panOffset += details.delta;
                    });
                  },
                  child: Container(
                    color: const Color(0xFFE0F2F1),
                    child: CustomPaint(
                      painter: MapPainter(
                        panOffset: _panOffset,
                        sites: provider.selectedProjectSites,
                        observations: provider.allObservations,
                      ),
                      child: Container(),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              'Arrastra para desplazarte. El mapa muestra la posición relativa de observaciones.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 11, fontStyle: FontStyle.italic, color: Colors.grey),
            )
          ],
        ),
      ),
    );
  }

  Widget _buildStatusHeader(ThemeData theme) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Gestor MBTiles Offline', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                Text(_isMapDownloaded ? 'Base local lista' : 'Mapa en caché remota',
                  style: TextStyle(fontSize: 11, color: _isMapDownloaded ? Colors.green : Colors.grey)),
              ],
            ),
            if (!_isMapDownloaded)
              ElevatedButton(
                onPressed: () => setState(() => _isMapDownloaded = true),
                child: const Text('Descargar', style: TextStyle(fontSize: 11)),
              )
            else
              const Icon(Icons.check_circle, color: Colors.green, size: 20),
          ],
        ),
      ),
    );
  }
}

class MapPainter extends CustomPainter {
  final Offset panOffset;
  final List<Site> sites;
  final List<Observation> observations;

  MapPainter({
    required this.panOffset,
    required this.sites,
    required this.observations,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);

    // 1. Grid
    final gridPaint = Paint()..color = Colors.grey.withOpacity(0.2)..strokeWidth = 1;
    for (double i = 0; i < size.width; i += 50) {
      canvas.drawLine(Offset(i + (panOffset.dx % 50), 0), Offset(i + (panOffset.dx % 50), size.height), gridPaint);
    }
    for (double i = 0; i < size.height; i += 50) {
      canvas.drawLine(Offset(0, i + (panOffset.dy % 50)), Offset(size.width, i + (panOffset.dy % 50)), gridPaint);
    }

    // 2. Rivers (Simulated)
    final riverPaint = Paint()..color = Colors.lightBlue.withOpacity(0.3)..strokeWidth = 5..style = PaintingStyle.stroke;
    final path = Path();
    path.moveTo(size.width * 0.2 + panOffset.dx, 0 + panOffset.dy);
    path.quadraticBezierTo(size.width * 0.5 + panOffset.dx, size.height * 0.5 + panOffset.dy, size.width * 0.1 + panOffset.dx, size.height + panOffset.dy);
    canvas.drawPath(path, riverPaint);

    // 3. Sites (Green Dots)
    final sitePaint = Paint()..color = const Color(0xFF1B5E20);
    for (var site in sites) {
      final dx = center.dx + (site.longitude + 74.0721) * 1000 + panOffset.dx;
      final dy = center.dy - (site.latitude - 4.7110) * 1000 + panOffset.dy;
      canvas.drawCircle(Offset(dx, dy), 8, sitePaint);
    }

    // 4. Observations (Orange Dots)
    final obsPaint = Paint()..color = Colors.orange;
    for (var obs in observations) {
      final dx = center.dx + (obs.longitude + 74.0721) * 1004 + panOffset.dx;
      final dy = center.dy - (obs.latitude - 4.7110) * 1004 + panOffset.dy;
      canvas.drawCircle(Offset(dx, dy), 6, obsPaint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
