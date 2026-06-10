import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/models.dart';

class DatabaseService {
  static final DatabaseService instance = DatabaseService._init();
  static Database? _database;

  DatabaseService._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('bird_sample.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDB,
    );
  }

  Future _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE projects (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        client TEXT NOT NULL,
        contractNumber TEXT NOT NULL,
        description TEXT NOT NULL,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL,
        status TEXT NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE sites (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        projectId INTEGER NOT NULL,
        name TEXT NOT NULL,
        department TEXT NOT NULL,
        municipality TEXT NOT NULL,
        vereda TEXT NOT NULL,
        ecosystem TEXT NOT NULL,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        altitude REAL NOT NULL,
        accuracy REAL NOT NULL,
        FOREIGN KEY (projectId) REFERENCES projects (id) ON DELETE CASCADE
      )
    ''');

    await db.execute('''
      CREATE TABLE samplings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        siteId INTEGER NOT NULL,
        observer TEXT NOT NULL,
        date INTEGER NOT NULL,
        weather TEXT NOT NULL,
        temperature REAL NOT NULL,
        humidity REAL NOT NULL,
        methodology TEXT NOT NULL,
        customFieldsJson TEXT NOT NULL,
        FOREIGN KEY (siteId) REFERENCES sites (id) ON DELETE CASCADE
      )
    ''');

    await db.execute('''
      CREATE TABLE observations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        samplingId INTEGER NOT NULL,
        birdScientificName TEXT NOT NULL,
        birdCommonName TEXT NOT NULL,
        birdFamily TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        sex TEXT NOT NULL,
        age TEXT NOT NULL,
        behavior TEXT NOT NULL,
        photoPath TEXT,
        audioPath TEXT,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        altitude REAL NOT NULL,
        notes TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        FOREIGN KEY (samplingId) REFERENCES samplings (id) ON DELETE CASCADE
      )
    ''');

    await db.execute('''
      CREATE TABLE form_templates (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        fieldsJson TEXT NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE sync_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        action TEXT NOT NULL,
        status TEXT NOT NULL,
        summary TEXT NOT NULL,
        timestamp INTEGER NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE bird_articles (
        link TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        description TEXT NOT NULL,
        pubDate TEXT NOT NULL,
        creator TEXT NOT NULL,
        category TEXT NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE sampling_sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        template TEXT NOT NULL,
        projectName TEXT NOT NULL,
        date TEXT NOT NULL,
        author TEXT NOT NULL,
        institution TEXT NOT NULL,
        location TEXT NOT NULL,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        altitude REAL NOT NULL,
        ecosystem TEXT NOT NULL,
        weather TEXT NOT NULL,
        temperature REAL NOT NULL,
        humidity REAL NOT NULL,
        methodology TEXT NOT NULL,
        duration INTEGER NOT NULL,
        timeStart TEXT NOT NULL,
        timeEnd TEXT NOT NULL,
        observer TEXT NOT NULL,
        notes TEXT NOT NULL,
        speciesJson TEXT NOT NULL,
        generalPhotosJson TEXT NOT NULL,
        createdAt INTEGER NOT NULL,
        isSynced INTEGER NOT NULL
      )
    ''');
  }

  Future close() async {
    final db = await instance.database;
    db.close();
  }
}
