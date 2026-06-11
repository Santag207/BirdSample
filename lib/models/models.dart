class Project {
  final int? id;
  final String name;
  final String client;
  final String contractNumber;
  final String description;
  final int createdAt;
  final int updatedAt;
  final String status;

  Project({
    this.id,
    required this.name,
    required this.client,
    required this.contractNumber,
    required this.description,
    int? createdAt,
    int? updatedAt,
    this.status = "Borrador",
  })  : createdAt = createdAt ?? DateTime.now().millisecondsSinceEpoch,
        updatedAt = updatedAt ?? DateTime.now().millisecondsSinceEpoch;

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'name': name,
      'client': client,
      'contractNumber': contractNumber,
      'description': description,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
      'status': status,
    };
  }

  factory Project.fromMap(Map<String, dynamic> map) {
    return Project(
      id: map['id'],
      name: map['name'],
      client: map['client'],
      contractNumber: map['contractNumber'],
      description: map['description'],
      createdAt: map['createdAt'],
      updatedAt: map['updatedAt'],
      status: map['status'],
    );
  }

  Project copyWith({
    int? id,
    String? name,
    String? client,
    String? contractNumber,
    String? description,
    int? createdAt,
    int? updatedAt,
    String? status,
  }) {
    return Project(
      id: id ?? this.id,
      name: name ?? this.name,
      client: client ?? this.client,
      contractNumber: contractNumber ?? this.contractNumber,
      description: description ?? this.description,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      status: status ?? this.status,
    );
  }
}

class Site {
  final int? id;
  final int projectId;
  final String name;
  final String department;
  final String municipality;
  final String vereda;
  final String ecosystem;
  final double latitude;
  final double longitude;
  final double altitude;
  final double accuracy;

  Site({
    this.id,
    required this.projectId,
    required this.name,
    required this.department,
    required this.municipality,
    required this.vereda,
    required this.ecosystem,
    required this.latitude,
    required this.longitude,
    required this.altitude,
    this.accuracy = 0.0,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'projectId': projectId,
      'name': name,
      'department': department,
      'municipality': municipality,
      'vereda': vereda,
      'ecosystem': ecosystem,
      'latitude': latitude,
      'longitude': longitude,
      'altitude': altitude,
      'accuracy': accuracy,
    };
  }

  factory Site.fromMap(Map<String, dynamic> map) {
    return Site(
      id: map['id'],
      projectId: map['projectId'],
      name: map['name'],
      department: map['department'],
      municipality: map['municipality'],
      vereda: map['vereda'],
      ecosystem: map['ecosystem'],
      latitude: map['latitude'],
      longitude: map['longitude'],
      altitude: map['altitude'],
      accuracy: map['accuracy'],
    );
  }
}

class Sampling {
  final int? id;
  final int siteId;
  final String observer;
  final int date;
  final String weather;
  final double temperature;
  final double humidity;
  final String methodology;
  final String customFieldsJson;

  Sampling({
    this.id,
    required this.siteId,
    required this.observer,
    int? date,
    required this.weather,
    required this.temperature,
    required this.humidity,
    required this.methodology,
    this.customFieldsJson = "{}",
  }) : date = date ?? DateTime.now().millisecondsSinceEpoch;

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'siteId': siteId,
      'observer': observer,
      'date': date,
      'weather': weather,
      'temperature': temperature,
      'humidity': humidity,
      'methodology': methodology,
      'customFieldsJson': customFieldsJson,
    };
  }

  factory Sampling.fromMap(Map<String, dynamic> map) {
    return Sampling(
      id: map['id'],
      siteId: map['siteId'],
      observer: map['observer'],
      date: map['date'],
      weather: map['weather'],
      temperature: map['temperature'],
      humidity: map['humidity'],
      methodology: map['methodology'],
      customFieldsJson: map['customFieldsJson'],
    );
  }

  Sampling copyWith({
    int? id,
    int? siteId,
    String? observer,
    int? date,
    String? weather,
    double? temperature,
    double? humidity,
    String? methodology,
    String? customFieldsJson,
  }) {
    return Sampling(
      id: id ?? this.id,
      siteId: siteId ?? this.siteId,
      observer: observer ?? this.observer,
      date: date ?? this.date,
      weather: weather ?? this.weather,
      temperature: temperature ?? this.temperature,
      humidity: humidity ?? this.humidity,
      methodology: methodology ?? this.methodology,
      customFieldsJson: customFieldsJson ?? this.customFieldsJson,
    );
  }
}

class Observation {
  final int? id;
  final int samplingId;
  final String birdScientificName;
  final String birdCommonName;
  final String birdFamily;
  final int quantity;
  final String sex;
  final String age;
  final String behavior;
  final String? photoPath;
  final String? audioPath;
  final double latitude;
  final double longitude;
  final double altitude;
  final String notes;
  final int timestamp;

  Observation({
    this.id,
    required this.samplingId,
    required this.birdScientificName,
    required this.birdCommonName,
    required this.birdFamily,
    required this.quantity,
    required this.sex,
    required this.age,
    required this.behavior,
    this.photoPath,
    this.audioPath,
    this.latitude = 0.0,
    this.longitude = 0.0,
    this.altitude = 0.0,
    this.notes = "",
    int? timestamp,
  }) : timestamp = timestamp ?? DateTime.now().millisecondsSinceEpoch;

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'samplingId': samplingId,
      'birdScientificName': birdScientificName,
      'birdCommonName': birdCommonName,
      'birdFamily': birdFamily,
      'quantity': quantity,
      'sex': sex,
      'age': age,
      'behavior': behavior,
      'photoPath': photoPath,
      'audioPath': audioPath,
      'latitude': latitude,
      'longitude': longitude,
      'altitude': altitude,
      'notes': notes,
      'timestamp': timestamp,
    };
  }

  factory Observation.fromMap(Map<String, dynamic> map) {
    return Observation(
      id: map['id'],
      samplingId: map['samplingId'],
      birdScientificName: map['birdScientificName'],
      birdCommonName: map['birdCommonName'],
      birdFamily: map['birdFamily'],
      quantity: map['quantity'],
      sex: map['sex'],
      age: map['age'],
      behavior: map['behavior'],
      photoPath: map['photoPath'],
      audioPath: map['audioPath'],
      latitude: map['latitude'],
      longitude: map['longitude'],
      altitude: map['altitude'],
      notes: map['notes'],
      timestamp: map['timestamp'],
    );
  }
}

class FormTemplate {
  final int? id;
  final String name;
  final String fieldsJson;

  FormTemplate({
    this.id,
    required this.name,
    required this.fieldsJson,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'name': name,
      'fieldsJson': fieldsJson,
    };
  }

  factory FormTemplate.fromMap(Map<String, dynamic> map) {
    return FormTemplate(
      id: map['id'],
      name: map['name'],
      fieldsJson: map['fieldsJson'],
    );
  }
}

class SyncLog {
  final int? id;
  final String action;
  final String status;
  final String summary;
  final int timestamp;

  SyncLog({
    this.id,
    required this.action,
    required this.status,
    required this.summary,
    int? timestamp,
  }) : timestamp = timestamp ?? DateTime.now().millisecondsSinceEpoch;

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'action': action,
      'status': status,
      'summary': summary,
      'timestamp': timestamp,
    };
  }

  factory SyncLog.fromMap(Map<String, dynamic> map) {
    return SyncLog(
      id: map['id'],
      action: map['action'],
      status: map['status'],
      summary: map['summary'],
      timestamp: map['timestamp'],
    );
  }
}

class BirdArticle {
  final String link;
  final String title;
  final String description;
  final String pubDate;
  final String creator;
  final String category;

  BirdArticle({
    required this.link,
    required this.title,
    required this.description,
    required this.pubDate,
    this.creator = "",
    this.category = "",
  });

  Map<String, dynamic> toMap() {
    return {
      'link': link,
      'title': title,
      'description': description,
      'pubDate': pubDate,
      'creator': creator,
      'category': category,
    };
  }

  factory BirdArticle.fromMap(Map<String, dynamic> map) {
    return BirdArticle(
      link: map['link'],
      title: map['title'],
      description: map['description'],
      pubDate: map['pubDate'],
      creator: map['creator'] ?? "",
      category: map['category'] ?? "",
    );
  }
}

class SamplingSession {
  final int? id;
  final String template;
  final String projectName;
  final String date;
  final String author;
  final String institution;
  final String location;
  final double latitude;
  final double longitude;
  final double altitude;
  final String ecosystem;
  final String weather;
  final double temperature;
  final double humidity;
  final String methodology;
  final int duration;
  final String timeStart;
  final String timeEnd;
  final String observer;
  final String notes;
  final String speciesJson;
  final String generalPhotosJson;
  final int createdAt;
  final bool isSynced;

  SamplingSession({
    this.id,
    this.template = "aves_estandar",
    this.projectName = "",
    this.date = "",
    this.author = "",
    this.institution = "",
    this.location = "",
    this.latitude = 0.0,
    this.longitude = 0.0,
    this.altitude = 0.0,
    this.ecosystem = "",
    this.weather = "",
    this.temperature = 0.0,
    this.humidity = 0.0,
    this.methodology = "",
    this.duration = 0,
    this.timeStart = "",
    this.timeEnd = "",
    this.observer = "",
    this.notes = "",
    this.speciesJson = "[]",
    this.generalPhotosJson = "[]",
    int? createdAt,
    this.isSynced = false,
  }) : createdAt = createdAt ?? DateTime.now().millisecondsSinceEpoch;

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'template': template,
      'projectName': projectName,
      'date': date,
      'author': author,
      'institution': institution,
      'location': location,
      'latitude': latitude,
      'longitude': longitude,
      'altitude': altitude,
      'ecosystem': ecosystem,
      'weather': weather,
      'temperature': temperature,
      'humidity': humidity,
      'methodology': methodology,
      'duration': duration,
      'timeStart': timeStart,
      'timeEnd': timeEnd,
      'observer': observer,
      'notes': notes,
      'speciesJson': speciesJson,
      'generalPhotosJson': generalPhotosJson,
      'createdAt': createdAt,
      'isSynced': isSynced ? 1 : 0,
    };
  }

  factory SamplingSession.fromMap(Map<String, dynamic> map) {
    return SamplingSession(
      id: map['id'],
      template: map['template'],
      projectName: map['projectName'],
      date: map['date'],
      author: map['author'],
      institution: map['institution'],
      location: map['location'],
      latitude: map['latitude'],
      longitude: map['longitude'],
      altitude: map['altitude'],
      ecosystem: map['ecosystem'],
      weather: map['weather'],
      temperature: map['temperature'],
      humidity: map['humidity'],
      methodology: map['methodology'],
      duration: map['duration'],
      timeStart: map['timeStart'],
      timeEnd: map['timeEnd'],
      observer: map['observer'],
      notes: map['notes'],
      speciesJson: map['speciesJson'],
      generalPhotosJson: map['generalPhotosJson'],
      createdAt: map['createdAt'],
      isSynced: map['isSynced'] == 1,
    );
  }

  SamplingSession copyWith({
    int? id,
    String? template,
    String? projectName,
    String? date,
    String? author,
    String? institution,
    String? location,
    double? latitude,
    double? longitude,
    double? altitude,
    String? ecosystem,
    String? weather,
    double? temperature,
    double? humidity,
    String? methodology,
    int? duration,
    String? timeStart,
    String? timeEnd,
    String? observer,
    String? notes,
    String? speciesJson,
    String? generalPhotosJson,
    int? createdAt,
    bool? isSynced,
  }) {
    return SamplingSession(
      id: id ?? this.id,
      template: template ?? this.template,
      projectName: projectName ?? this.projectName,
      date: date ?? this.date,
      author: author ?? this.author,
      institution: institution ?? this.institution,
      location: location ?? this.location,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      altitude: altitude ?? this.altitude,
      ecosystem: ecosystem ?? this.ecosystem,
      weather: weather ?? this.weather,
      temperature: temperature ?? this.temperature,
      humidity: humidity ?? this.humidity,
      methodology: methodology ?? this.methodology,
      duration: duration ?? this.duration,
      timeStart: timeStart ?? this.timeStart,
      timeEnd: timeEnd ?? this.timeEnd,
      observer: observer ?? this.observer,
      notes: notes ?? this.notes,
      speciesJson: speciesJson ?? this.speciesJson,
      generalPhotosJson: generalPhotosJson ?? this.generalPhotosJson,
      createdAt: createdAt ?? this.createdAt,
      isSynced: isSynced ?? this.isSynced,
    );
  }
}

class SessionSpecies {
  final String id;
  final String family;
  final String name;
  final String commonName;
  final int count;
  final String behavior;
  final String sexAge;
  final String notes;
  final List<SessionPhoto> photos;
  final String? audioPath;

  SessionSpecies({
    required this.id,
    required this.family,
    required this.name,
    this.commonName = "",
    this.count = 1,
    this.behavior = "",
    this.sexAge = "",
    this.notes = "",
    this.photos = const [],
    this.audioPath,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'family': family,
      'name': name,
      'commonName': commonName,
      'count': count,
      'behavior': behavior,
      'sexAge': sexAge,
      'notes': notes,
      'photos': photos.map((p) => p.toMap()).toList(),
      'audioPath': audioPath,
    };
  }

  factory SessionSpecies.fromMap(Map<String, dynamic> map) {
    return SessionSpecies(
      id: map['id'],
      family: map['family'],
      name: map['name'],
      commonName: map['commonName'] ?? "",
      count: map['count'] ?? 1,
      behavior: map['behavior'] ?? "",
      sexAge: map['sexAge'] ?? "",
      notes: map['notes'] ?? "",
      photos: (map['photos'] as List? ?? [])
          .map((p) => SessionPhoto.fromMap(p))
          .toList(),
      audioPath: map['audioPath'],
    );
  }
}

class SessionPhoto {
  final String path;
  final String caption;
  final String name;

  SessionPhoto({
    required this.path,
    this.caption = "",
    this.name = "",
  });

  Map<String, dynamic> toMap() {
    return {
      'path': path,
      'caption': caption,
      'name': name,
    };
  }

  factory SessionPhoto.fromMap(Map<String, dynamic> map) {
    return SessionPhoto(
      path: map['path'],
      caption: map['caption'] ?? "",
      name: map['name'] ?? "",
    );
  }
}
