# ClimaPulse

App Android para observar temperatura, humedad y luz. Kotlin, Jetpack Compose y Material 3.

## Abrir en Android Studio

1. Abre Android Studio.
2. Selecciona **Open** y elige esta carpeta: `AndroidStudioProjects/ClimaPulse`.
3. Espera a que termine la sincronización de Gradle.
4. Ejecuta la configuración `app` en un emulador o dispositivo Android.

## Pantallas

| Pantalla | Dónde |
|---|---|
| Splash (SplashScreen API) + Router | `MainActivity.kt`, `MainViewModel.kt` |
| Onboarding (3 vistas, permiso de notificaciones en contexto) | `ui/onboarding/` |
| Login / Registro / Recuperación + Google/Apple + huella | `ui/auth/` |
| Home con barra inferior y Navigation Graph | `ui/home/HomeScaffold.kt` |
| Tour guiado del Home (spotlight, 5 pasos, repetible desde Perfil) | `ui/tour/` |
| Detalle de métrica (argumento tipado, formulario con ViewModel) | `ui/detail/` |
| Historial con estado vacío | `ui/history/` |
| Perfil / Ajustes / Términos / Cerrar sesión | `ui/settings/` |

## Fundamentos técnicos

- **Router**: `SessionRepository.resolveStartDestination()` decide Onboarding → Login → Home mientras la Splash sigue visible (`setKeepOnScreenCondition`, sin `Thread.sleep`).
- **Onboarding y tour de un solo uso**: banderas `onboarding_done` y `home_tour_done` en DataStore. El tour se puede repetir desde Perfil → Ayuda.
- **Token seguro**: cifrado AES-GCM con llave en el **Android Keystore** (`data/TokenCipher.kt`). Nunca en texto plano.
- **Navegación tipada**: rutas `@Serializable` de Navigation Compose (equivalente a SafeArgs).
- **Estado**: cada pantalla con datos usa `UiState` (Loading / Success / Error / Empty) desde un ViewModel; los formularios usan `SavedStateHandle` y sobreviven al giro.
- **Offline**: la última lectura queda en caché; un banner avisa la pérdida de conexión y la app se resincroniza sola al volver.
- **Sistema de diseño**: Material 3 oscuro derivado del logo (azul marino `#17212F`, turquesa `#2BC1AD`, amarillo `#FFD166`, coral para temperatura). Tokens en `ui/theme/`, componentes en `ui/components/`, tipografía Poppins (títulos) + Roboto (cuerpo).
- **Íconos SVG** en `res/drawable/`: métricas en duotono (`ic_metric_*`), logos oficiales de Google y Apple (`ic_google`, `ic_apple`) e ilustraciones de estados (`ill_empty_history`, `ill_offline`).

## Notas

- **Modo demo activo**: `FeatureFlags.ALWAYS_SHOW_INTRO = true` hace que en *cada* ingreso al Home (iniciar sesión o abrir la app con sesión activa) aparezcan la ventana de permisos (`ui/permissions/PermissionsSheet.kt`) y luego el tour. Cambiarlo a `false` vuelve al comportamiento final: el tour una sola vez.

- El backend es **simulado**: el login acepta cualquier correo válido con contraseña de 6+ caracteres; Google/Apple simulan el SSO (no hay Firebase ni Credential Manager configurados).
- Las lecturas son de demostración (`USE_DEMO_DATA` en `data/EnvironmentRepository.kt`). En el emulador, apaga wifi/datos para ver el estado sin conexión.
