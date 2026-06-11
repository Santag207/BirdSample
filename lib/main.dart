import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import 'providers/bird_provider.dart';
import 'ui/screens/main_screen.dart';

void main() {
  runApp(
    ChangeNotifierProvider(
      create: (_) => BirdProvider(),
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'OrnithoCache Pro',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF006D32),
          primary: const Color(0xFF006D32),
          onPrimary: Colors.white,
          primaryContainer: const Color(0xFFD7E8CD),
          onPrimaryContainer: const Color(0xFF05210B),
          secondary: const Color(0xFF2E7D32),
          surface: const Color(0xFFFBFDF8),
          onSurface: const Color(0xFF191C19),
          surfaceVariant: const Color(0xFFDCE5D5),
          onSurfaceVariant: const Color(0xFF424940),
          outline: const Color(0xFFC3C8BC),
          error: const Color(0xFFBA1A1A),
        ),
        textTheme: GoogleFonts.interTextTheme(),
      ),
      home: const MainScreen(),
    );
  }
}
