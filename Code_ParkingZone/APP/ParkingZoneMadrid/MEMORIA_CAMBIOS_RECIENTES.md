# Script para memoria — Cambios recientes (ParkingZone Madrid)

Documento auxiliar para incorporar a la memoria del proyecto.  
**No modifica** el documento de Google Drive; copiar y pegar las secciones que correspondan.

---

## 1. Arquitectura y persistencia de datos

### Room (`parkingzone_madrid.db`)

Base de datos local SQLite gestionada con Room (versión 3). Tablas activas:

| Tabla | Contenido |
|-------|-----------|
| `usuarios` | Cuentas: `correo` (clave), `nom_usuario`, `password` (hash SHA-256 con sal = correo) |
| `favoritos_zona_ser` | Favoritos del mapa: `correo_usuario` + `id_zona` (identificador de calle derivado del CSV) |
| `zonas` | Definida en el esquema; importación opcional desde CSV. **El mapa principal no depende de esta tabla**: las calles se leen del asset `calles_SER_2026.csv` vía `ParkingZonesData`. |

### SharedPreferences (`parking_zone_prefs`)

Guarda solo la **sesión activa** (nombre y correo). No almacena la contraseña en claro.

### Fuente del mapa

Datos SER del CSV oficial en `assets/calles_SER_2026.csv`. Cada fila es una plaza; la app agrupa por `(distrito, calle)` y convierte coordenadas UTM 30N a WGS84.

---

## 2. Login y gestión de usuarios (flujo acordado — opción A)

- El **launcher** sigue siendo el mapa (`MapActivity`).
- Acceso al login desde el menú del mapa: **Iniciar sesión** / **Cerrar sesión**.
- Si el usuario intenta guardar un favorito sin sesión, se le pide iniciar sesión.

### Crear cuenta

Correo, contraseña, confirmación de contraseña y nombre de usuario → guardado en Room (`usuarios`).

### Iniciar sesión

Correo y contraseña → validación contra Room. Si la contraseña es incorrecta, mensaje de error y nuevo intento.

### Cerrar sesión

Borra la sesión en SharedPreferences; la cuenta **permanece en Room**, de modo que el usuario puede volver a entrar con el mismo correo y contraseña y recuperar favoritos.

### Implementación

- `LoginActivity` + `LoginViewModel`
- `ParkingLocalRepository` (`registerUser`, `signInUser`)
- `PasswordHasher` para no guardar contraseñas en texto plano

### Estructura de carpetas UI

`ui/login/`, `ui/mapa/`, `ui/mapa/adapters/`, `ui/mapa/components/`

---

## 3. Mapa: filtros y leyenda (solo datos del CSV)

### Colores de zona (pines)

Alineados con la columna `color` del CSV:

- **Verde** → solo plazas verdes en la calle
- **Azul** → solo plazas azules
- **Mixta** → calle con plazas verdes y azules (agregación; no es un valor literal del CSV)
- **Naranja / Rojo / Alta rotación** → pin gris (`pin_otro`)

### Filtros del menú (columna `bateria_linea`)

- Todas las plazas
- Con plazas en línea
- Con plazas en batería

Se eliminó la opción “Solo mixto” del menú porque **no existe** como valor en el CSV.

### Leyenda superior (Verde / Azul / Mixta)

Clicable: filtra el mapa por ese color; segundo clic desactiva el filtro.

### Tarifas mostradas

Textos orientativos alineados con referencias tipo parking-madrid.es (verde residente, azul rotación, etc.), con la salvedad de contrastar siempre con la fuente oficial del Ayuntamiento.

---

## 4. Mejoras de UX en el mapa (feedback de la tutora)

### Popup de calle (InfoWindow)

Al pulsar un pin, el recuadro resumen **permanece abierto** hasta tocar otra calle o el mapa vacío. Se corrigió el cierre automático causado por el refresco de marcadores al centrar el mapa.

### Rendimiento

- Zoom inicial de barrio (~15.5), no todo Madrid.
- Solo se pintan pines **visibles en el viewport** (tope ~350 marcadores).
- Animación suave al centrar (`animateTo`, ~800 ms).

### Favoritos desde el cajón

Al pulsar una calle en “Mis favoritos”: cierra el menú, centra el mapa, abre el popup con tarifa/plazas y el botón **Cómo llegar (Google Maps)**.

### Barra de estado

Insets del sistema (`WindowInsetsCompat`) para que la toolbar no quede bajo la barra de notificaciones.

### Geolocalización

- Permisos `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`.
- `MyLocationNewOverlay` con icono personalizado: círculo azul con borde blanco (`ic_my_location_dot.xml`).
- Si no hay permiso: diálogo explicativo al abrir/volver a la app; botón **Mi ubicación** en el menú.
- En emulador, la posición se configura en Extended Controls → Location (no en Ajustes del Android virtual).

### Texto del popup

Las plazas se describen como datos del **CSV oficial** (no “en tiempo real”), coherente con la limitación del dataset.

---

## 5. Cómo verificar Room en desarrollo

1. Ejecutar la app, crear cuenta y guardar un favorito.
2. Android Studio → **App Inspection** / **Database Inspector** → `parkingzone_madrid.db`.
3. Revisar tablas `usuarios` y `favoritos_zona_ser`.

Alternativa: extraer `/data/data/com.example.parkingzonemadrid/databases/parkingzone_madrid.db` y abrirlo con DB Browser for SQLite.

---

## 6. Limitaciones documentadas (para la memoria)

| Aspecto | Situación actual |
|---------|------------------|
| Datos en tiempo real de plazas libres | No disponibles en el CSV SER; la app muestra plazas totales por calle, no ocupación en vivo |
| Aparcamiento gratuito | No incluido: el CSV solo contiene plazas SER reguladas con tarifa |
| Datos en tiempo real (sensores) | Requeriría API del Ayuntamiento (Portal Datos Abiertos); fuera del alcance actual |
| Tabla `zonas` en Room | Opcional; el mapa usa el CSV en assets directamente |

---

## 7. Archivos principales tocados (referencia técnica)

- `AppDatabase.kt`, `UsuarioEntity`, `FavoritoZonaEntity`, `UsuarioDao`, `FavoritoZonaDao`
- `ParkingLocalRepository.kt`, `AuthResult.kt`, `PasswordHasher.kt`
- `LoginActivity.kt`, `LoginViewModel.kt`, `activity_login.xml`
- `MapActivity.kt`, `StreetInfoWindow.kt`, `FavoritesAdapter.kt`
- `ParkingZonesData.kt`, `StreetZone.kt`
- `activity_map.xml`, `menu_map.xml`, `infowindow_street.xml`, `strings.xml`
- `AndroidManifest.xml` (permisos de ubicación)
- Drawables: `ic_my_location_dot.xml`, `ic_my_location_direction.xml`, `ic_lock.xml`

---

## 8. Frase sugerida para conclusiones de la memoria

*“ParkingZone Madrid muestra las zonas SER de Madrid a partir del CSV oficial del Ayuntamiento, con filtros por color y tipo de plaza, favoritos por usuario persistidos en Room, login local con correo y contraseña, geolocalización en mapa y navegación a Google Maps. Los datos de plazas no son en tiempo real; reflejan el inventario SER publicado en el dataset, no la ocupación actual de cada plaza.”*

---

*Generado como apoyo para la memoria del TFG. Tu compañera puede copiar secciones al documento principal cuando corresponda.*
