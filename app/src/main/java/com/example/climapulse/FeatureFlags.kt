package com.example.climapulse

/** Interruptores de comportamiento para demos y pruebas. */
object FeatureFlags {
    /**
     * true (temporal): la ventana de permisos y el tour aparecen en CADA ingreso al Home.
     * false: comportamiento final, el tour se muestra una sola vez (bandera en DataStore).
     */
    const val ALWAYS_SHOW_INTRO = true
}
