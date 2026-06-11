import 'package:flutter_test/flutter_test.dart';
import 'package:bird_sample/models/models.dart';

void main() {
  group('Data Models', () {
    test('Project toMap and fromMap', () {
      final project = Project(
        id: 1,
        name: 'Test Project',
        client: 'Test Client',
        contractNumber: '123',
        description: 'Test Desc',
      );

      final map = project.toMap();
      expect(map['name'], 'Test Project');

      final fromMap = Project.fromMap(map);
      expect(fromMap.name, 'Test Project');
      expect(fromMap.id, 1);
    });

    test('Site toMap and fromMap', () {
      final site = Site(
        id: 1,
        projectId: 1,
        name: 'Test Site',
        department: 'Dept',
        municipality: 'Mun',
        vereda: 'Ver',
        ecosystem: 'Eco',
        latitude: 1.0,
        longitude: 2.0,
        altitude: 3.0,
      );

      final map = site.toMap();
      expect(map['name'], 'Test Site');

      final fromMap = Site.fromMap(map);
      expect(fromMap.latitude, 1.0);
    });
  });
}
