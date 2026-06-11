import 'dart:io';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:googleapis/drive/v3.dart' as drive;
import 'package:extension_google_sign_in_as_googleapis_auth/extension_google_sign_in_as_googleapis_auth.dart';

class GoogleDriveService {
  static final GoogleSignIn _googleSignIn = GoogleSignIn(
    scopes: [
      drive.DriveApi.driveFileScope,
    ],
  );

  static Future<GoogleSignInAccount?> signIn() async {
    try {
      return await _googleSignIn.signIn();
    } catch (error) {
      print('Error en Google Sign In: $error');
      return null;
    }
  }

  static Future<GoogleSignInAccount?> signInSilently() async {
    try {
      return await _googleSignIn.signInSilently();
    } catch (error) {
      print('Error en Google Sign In Silently: $error');
      return null;
    }
  }

  static Future<void> signOut() => _googleSignIn.signOut();

  static Future<bool> isSignedIn() => _googleSignIn.isSignedIn();

  static Future<GoogleSignInAccount?> get currentUser async => _googleSignIn.currentUser;

  static Future<drive.DriveApi?> _getDriveApi() async {
    final account = _googleSignIn.currentUser;
    if (account == null) return null;

    final authClient = await _googleSignIn.authenticatedClient();
    if (authClient == null) return null;

    return drive.DriveApi(authClient);
  }

  static Future<String?> _getOrCreateFolder(drive.DriveApi driveApi, String folderName, {String? parentId}) async {
    String query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
    if (parentId != null) {
      query += " and '$parentId' in parents";
    }

    final folderList = await driveApi.files.list(q: query);
    if (folderList.files != null && folderList.files!.isNotEmpty) {
      return folderList.files!.first.id;
    }

    final folder = drive.File()
      ..name = folderName
      ..mimeType = 'application/vnd.google-apps.folder';

    if (parentId != null) {
      folder.parents = [parentId];
    }

    final createdFolder = await driveApi.files.create(folder);
    return createdFolder.id;
  }

  static Future<bool> uploadFileToDrive(File file, String fileName, {String? mimeType}) async {
    final driveApi = await _getDriveApi();
    if (driveApi == null) return false;

    // 1. Root folder for the app
    final rootFolderId = await _getOrCreateFolder(driveApi, 'BirdSample');
    if (rootFolderId == null) return false;

    // 2. User specific folder (based on email)
    final userEmail = _googleSignIn.currentUser?.email ?? 'unknown_user';
    final userFolderId = await _getOrCreateFolder(driveApi, userEmail, parentId: rootFolderId);
    if (userFolderId == null) return false;

    // 3. Check if file exists to update it instead of creating a duplicate
    String query = "name = '$fileName' and '$userFolderId' in parents and trashed = false";
    final fileList = await driveApi.files.list(q: query);

    final driveFile = drive.File()..name = fileName;
    final media = drive.Media(file.openRead(), file.lengthSync(), contentType: mimeType ?? 'application/octet-stream');

    if (fileList.files != null && fileList.files!.isNotEmpty) {
      // Update existing file
      final existingFileId = fileList.files!.first.id!;
      final response = await driveApi.files.update(
        driveFile,
        existingFileId,
        uploadMedia: media,
      );
      return response.id != null;
    } else {
      // Create new file
      driveFile.parents = [userFolderId];
      final response = await driveApi.files.create(
        driveFile,
        uploadMedia: media,
      );
      return response.id != null;
    }
  }
}
