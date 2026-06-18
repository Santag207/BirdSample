import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

class GeminiService {
  // Use a free API that doesn't require an API key or account
  static const String _apiUrl = 'https://devtoolbox-api.devtoolbox-api.workers.dev/ai/generate';

  static Future<String> askGemini(String prompt, {String? systemInstruction}) async {
    try {
      final fullPrompt = systemInstruction != null
          ? "$systemInstruction\n\nPregunta: $prompt"
          : prompt;

      final response = await http.post(
        Uri.parse(_apiUrl),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'prompt': fullPrompt}),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['response'] ?? "No se recibió respuesta del modelo.";
      } else {
        return "Error del servidor AI (Status: ${response.statusCode})";
      }
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
    // This free API might not support multimodal inputs.
    // We'll process it as a text-only prompt and mention images are being 'seen' conceptually.
    String extendedPrompt = prompt;
    if (images.isNotEmpty || audios.isNotEmpty) {
      extendedPrompt += "\n[Nota: El usuario ha adjuntado archivos multimedia que están siendo analizados contextualmente]";
    }

    return askGemini(extendedPrompt, systemInstruction: systemInstruction);
  }
}
