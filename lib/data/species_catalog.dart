class BirdSpecies {
  final String commonName;
  final String scientificName;
  final String family;
  final String statusIUCN;
  final bool isEndemic;
  final bool isMigratorial;

  const BirdSpecies({
    required this.commonName,
    required this.scientificName,
    required this.family,
    required this.statusIUCN,
    this.isEndemic = false,
    this.isMigratorial = false,
  });
}

class SpeciesCatalog {
  static const families = [
    "Trochilidae (Colibríes)",
    "Ramphastidae (Tucanes)",
    "Psittacidae (Loros y Guacamayas)",
    "Cotingidae (Gallitos y Cotingas)",
    "Falconidae (Halcones y Caracaras)",
    "Columbidae (Palomas y Tórtolas)",
    "Thraupidae (Tangaras y semilleros)",
    "Turdidae (Mirlas y tordos)",
    "Ardeidae (Garzas)",
    "Cathartidae (Buitres y Cóndores)"
  ];

  static const species = [
    // Trochilidae
    BirdSpecies(
        commonName: "Colibrí rutilante",
        scientificName: "Colibri coruscans",
        family: "Trochilidae (Colibríes)",
        statusIUCN: "Preocupación Menor (LC)"),
    BirdSpecies(
        commonName: "Colibrí picoespada",
        scientificName: "Ensifera ensifera",
        family: "Trochilidae (Colibríes)",
        statusIUCN: "Preocupación Menor (LC)",
        isEndemic: true),
    BirdSpecies(
        commonName: "Colibrí coliancho",
        scientificName: "Selasphorus platycercus",
        family: "Trochilidae (Colibríes)",
        statusIUCN: "Preocupación Menor (LC)",
        isMigratorial: true),

    // Ramphastidae
    BirdSpecies(
        commonName: "Tucán de pico acanalado",
        scientificName: "Ramphastos vitellinus",
        family: "Ramphastidae (Tucanes)",
        statusIUCN: "Vulnerable (VU)"),
    BirdSpecies(
        commonName: "Tucancito esmeralda",
        scientificName: "Aulacorhynchus prasinus",
        family: "Ramphastidae (Tucanes)",
        statusIUCN: "Preocupación Menor (LC)"),

    // Psittacidae
    BirdSpecies(
        commonName: "Guacamaya bandera",
        scientificName: "Ara macao",
        family: "Psittacidae (Loros y Guacamayas)",
        statusIUCN: "Preocupación Menor (LC)"),
    BirdSpecies(
        commonName: "Loro orejiamarillo",
        scientificName: "Ognorhynchus icterotis",
        family: "Psittacidae (Loros y Guacamayas)",
        statusIUCN: "En Peligro Crítico (CR)",
        isEndemic: true),

    // Cotingidae
    BirdSpecies(
        commonName: "Gallito de las rocas",
        scientificName: "Rupicola peruvianus",
        family: "Cotingidae (Gallitos y Cotingas)",
        statusIUCN: "Preocupación Menor (LC)"),

    // Falconidae
    BirdSpecies(
        commonName: "Cernícalo americano",
        scientificName: "Falco sparverius",
        family: "Falconidae (Halcones y Caracaras)",
        statusIUCN: "Preocupación Menor (LC)"),
    BirdSpecies(
        commonName: "Halcón peregrino",
        scientificName: "Falco peregrinus",
        family: "Falconidae (Halcones y Caracaras)",
        statusIUCN: "Preocupación Menor (LC)",
        isMigratorial: true),

    // Columbidae
    BirdSpecies(
        commonName: "Torcaza común",
        scientificName: "Zenaida auriculata",
        family: "Columbidae (Palomas y Tórtolas)",
        statusIUCN: "Preocupación Menor (LC)"),

    // Thraupidae
    BirdSpecies(
        commonName: "Azulejo común",
        scientificName: "Thraupis episcopus",
        family: "Thraupidae (Tangaras y semilleros)",
        statusIUCN: "Preocupación Menor (LC)"),
    BirdSpecies(
        commonName: "Tangara rastrojera",
        scientificName: "Tangara vitriolina",
        family: "Thraupidae (Tangaras y semilleros)",
        statusIUCN: "Preocupación Menor (LC)"),

    // Turdidae
    BirdSpecies(
        commonName: "Mirla común",
        scientificName: "Turdus fuscater",
        family: "Turdidae (Mirlas y tordos)",
        statusIUCN: "Preocupación Menor (LC)"),

    // Ardeidae
    BirdSpecies(
        commonName: "Garza blanca",
        scientificName: "Ardea alba",
        family: "Ardeidae (Garzas)",
        statusIUCN: "Preocupación Menor (LC)"),

    // Cathartidae
    BirdSpecies(
        commonName: "Cóndor andino",
        scientificName: "Vultur gryphus",
        family: "Cathartidae (Buitres y Cóndores)",
        statusIUCN: "Vulnerable (VU)")
  ];

  static List<BirdSpecies> getSpeciesByFamily(String family) {
    return species.where((s) => s.family == family).toList();
  }
}
