import 'dart:io';
import 'package:image_picker/image_picker.dart';
import 'package:record/record.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

class MediaService {
  final ImagePicker _picker = ImagePicker();
  final AudioRecorder _recorder = AudioRecorder();
  final AudioPlayer _player = AudioPlayer();

  // Images
  Future<String?> takePhoto() async {
    final XFile? photo = await _picker.pickImage(source: ImageSource.camera, imageQuality: 80);
    if (photo != null) {
      final directory = await getApplicationDocumentsDirectory();
      final String fileName = 'IMG_${DateTime.now().millisecondsSinceEpoch}.jpg';
      final File savedImage = await File(photo.path).copy(p.join(directory.path, fileName));
      return savedImage.path;
    }
    return null;
  }

  Future<String?> pickImageFromGallery() async {
    final XFile? image = await _picker.pickImage(source: ImageSource.gallery, imageQuality: 80);
    if (image != null) {
      final directory = await getApplicationDocumentsDirectory();
      final String fileName = 'IMG_${DateTime.now().millisecondsSinceEpoch}.jpg';
      final File savedImage = await File(image.path).copy(p.join(directory.path, fileName));
      return savedImage.path;
    }
    return null;
  }

  // Audio
  Future<bool> startRecording() async {
    if (await _recorder.hasPermission()) {
      final directory = await getApplicationDocumentsDirectory();
      final String fileName = 'REC_${DateTime.now().millisecondsSinceEpoch}.m4a';
      final String path = p.join(directory.path, fileName);

      await _recorder.start(const RecordConfig(), path: path);
      return true;
    }
    return false;
  }

  Future<String?> stopRecording() async {
    return await _recorder.stop();
  }

  Future<void> playAudio(String path, {Function? onComplete}) async {
    await _player.play(DeviceFileSource(path));
    _player.onPlayerComplete.listen((event) {
      if (onComplete != null) onComplete();
    });
  }

  Future<void> stopPlayback() async {
    await _player.stop();
  }

  void dispose() {
    _recorder.dispose();
    _player.dispose();
  }
}
