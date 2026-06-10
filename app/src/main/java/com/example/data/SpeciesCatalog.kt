package com.example.data

data class BirdSpecies(
    val commonName: String,
    val scientificName: String,
    val family: String,
    val statusIUCN: String, // LC, NT, VU, EN, CR
    val isEndemic: Boolean = false,
    val isMigratorial: Boolean = false
)

object SpeciesCatalog {
    val families = listOf(
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
    )

    val species = listOf(
        // Trochilidae
        BirdSpecies("Colibrí rutilante", "Colibri coruscans", "Trochilidae (Colibríes)", "Preocupación Menor (LC)"),
        BirdSpecies("Colibrí picoespada", "Ensifera ensifera", "Trochilidae (Colibríes)", "Preocupación Menor (LC)", isEndemic = true),
        BirdSpecies("Colibrí coliancho", "Selasphorus platycercus", "Trochilidae (Colibríes)", "Preocupación Menor (LC)", isMigratorial = true),
        
        // Ramphastidae
        BirdSpecies("Tucán de pico acanalado", "Ramphastos vitellinus", "Ramphastidae (Tucanes)", "Vulnerable (VU)"),
        BirdSpecies("Tucancito esmeralda", "Aulacorhynchus prasinus", "Ramphastidae (Tucanes)", "Preocupación Menor (LC)"),
        
        // Psittacidae
        BirdSpecies("Guacamaya bandera", "Ara macao", "Psittacidae (Loros y Guacamayas)", "Preocupación Menor (LC)"),
        BirdSpecies("Loro orejiamarillo", "Ognorhynchus icterotis", "Psittacidae (Loros y Guacamayas)", "En Peligro Crítico (CR)", isEndemic = true),
        
        // Cotingidae
        BirdSpecies("Gallito de las rocas", "Rupicola peruvianus", "Cotingidae (Gallitos y Cotingas)", "Preocupación Menor (LC)"),
        
        // Falconidae
        BirdSpecies("Cernícalo americano", "Falco sparverius", "Falconidae (Halcones y Caracaras)", "Preocupación Menor (LC)"),
        BirdSpecies("Halcón peregrino", "Falco peregrinus", "Falconidae (Halcones y Caracaras)", "Preocupación Menor (LC)", isMigratorial = true),
        
        // Columbidae
        BirdSpecies("Torcaza común", "Zenaida auriculata", "Columbidae (Palomas y Tórtolas)", "Preocupación Menor (LC)"),
        
        // Thraupidae
        BirdSpecies("Azulejo común", "Thraupis episcopus", "Thraupidae (Tangaras y semilleros)", "Preocupación Menor (LC)"),
        BirdSpecies("Tangara rastrojera", "Tangara vitriolina", "Thraupidae (Tangaras y semilleros)", "Preocupación Menor (LC)"),
        
        // Turdidae
        BirdSpecies("Mirla común", "Turdus fuscater", "Turdidae (Mirlas y tordos)", "Preocupación Menor (LC)"),
        
        // Ardeidae
        BirdSpecies("Garza blanca", "Ardea alba", "Ardeidae (Garzas)", "Preocupación Menor (LC)"),
        
        // Cathartidae
        BirdSpecies("Cóndor andino", "Vultur gryphus", "Cathartidae (Buitres y Cóndores)", "Vulnerable (VU)")
    )

    fun getSpeciesByFamily(family: String): List<BirdSpecies> {
        return species.filter { it.family == family }
    }
}
