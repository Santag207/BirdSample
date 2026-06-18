import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:bird_sample/main.dart';
import 'package:bird_sample/providers/bird_provider.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => BirdProvider(),
        child: const MyApp(),
      ),
    );

    // Verify that we are on the home screen or main screen
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
