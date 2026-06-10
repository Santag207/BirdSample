import 'dart:convert';
import 'dart:io';
import 'package:google_generative_ai/google_generative_ai.dart';
import '../models/models.dart';

class GeminiService {
  static const String _modelName = 'gemini-1.5-flash';

  // In a real app, this should be handled securely (e.g., via --dart-define or a vault)
  // For the conversion, we'll assume it's provided or mocked.
  static String apiKey = "";

  static Future<String> askGemini(String prompt, {String? systemInstruction}) async {
    if (apiKey.isEmpty) {
      return "No se ha configurado la API Key de Gemini. Por favor, configúrala para habilitar el asistente de IA.";
    }

    try {
      final model = GenerativeModel(
        model: _modelName,
        apiKey: apiKey,
        systemInstruction: systemInstruction != null ? Content.system(systemInstruction) : null,
      );

      final content = [Content.text(prompt)];
      final response = await model.generateContent(content);
      return response.text ?? "No se recibió respuesta del modelo.";
    } catch (e) {
      return "Ocurrió un error al contactar al asistente de IA: $e";
    }
  }

  static Future<String> askGeminiMultimodal(
    String prompt, {
    String? systemInstruction,
    List<File> images = const [],
    List<File> audios = const [],
  }) async {
    if (apiKey.isEmpty) {
      return "No se ha configurado la API Key de Gemini. Por favor, configúrala para habilitar el asistente de IA.";
    }

    try {
      final model = GenerativeModel(
        model: _modelName,
        apiKey: apiKey,
        systemInstruction: systemInstruction != null ? Content.system(systemInstruction) : null,
      );

      final List<DataPart> dataParts = [];

      for (var image in images) {
        if (await image.exists()) {
          final bytes = await image.readAsBytes();
          dataParts.add(DataPart('image/jpeg', bytes));
        }
      }

      for (var audio in audios) {
        if (await audio.exists()) {
          final bytes = await audio.readAsBytes();
          dataParts.add(DataPart('audio/3gpp', bytes)); // Standard for recorded memos usually
        }
      }

      final content = [
        Content.multi([
          TextPart(prompt),
          ...dataParts,
        ])
      ];

      final response = await model.generateContent(content);
      return response.text ?? "No se recibió respuesta del modelo.";
    } catch (e) {
      return "Ocurrió un error al contactar al asistente de IA: $e";
    }
  }
}
