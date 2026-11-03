# ComeMejor — recetas, recetario y termómetro de cocina con ESP32

**Proyecto final · Temas Selectos de Programación II · 2027-I · Grupo 8**
Omar Rosario Carrasco · Prof. M. en I. Jorge Armando Rodríguez Vera · FI-UNAM

App Android (Kotlin + Jetpack Compose) **para comer mejor**: busca recetas en la API **TheMealDB**
(la misma de las prácticas 2 y 3), las guarda y califica en un **recetario local con Room**, y al
cocinarlas usa un **termómetro de cocina hecho con ESP32 + LM35 por Bluetooth clásico**, que avisa
y enciende un LED cuando el platillo llega a su **temperatura segura** (pollo 74 °C, carne 63 °C…).

> Versión 1 (`pfinalv1`): funcional de extremo a extremo, con pendientes por pulir listados al final.
> Para desarrollar o demostrar sin la tarjeta, la pantalla Cocina tiene un **modo simulación**.

---

## 1. Objetivo

Que una persona pueda **planear** qué comer (recetas por categoría: vegetariana, vegana, pescado,
pollo…), **recordar** lo que le funcionó (recetario con calificación, favoritas, veces cocinada y
notas) y **cocinar seguro** (temperaturas internas mínimas de la FDA/USDA medidas con un sensor real),
desde el teléfono y con lo visto en el curso.

Criterios de éxito de esta versión:

- Buscar y guardar una receta de TheMealDB en menos de 3 toques; explorar ≥ 10 recetas con foto por categoría.
- Recetario que funciona sin internet: consultar por id, calificar, marcar favorita, sumar preparaciones, notas, eliminar.
- Conectarse al ESP32 emparejado y leer la temperatura cada 2 s; LED encendido automáticamente al llegar al objetivo.
- Cada temperatura guardada queda ligada a la receta que se estaba cocinando.

## 2. Temas del curso que integra

| Tema visto en clase | Dónde está en ComeMejor |
|---|---|
| Kotlin (clases, `data class`, `sealed`, nulabilidad, lambdas) | `model/`, `domain/`, `viewmodel/` |
| Compose básico, IU y estado (`remember`, `Switch`, `TextField`, chips) | `ui/views/*`, `ui/components/*` |
| Listas desplazables, `Card`, `LazyVerticalGrid` | Recetario, cuadrícula de categorías, Historial, Dispositivos |
| Testing (local + instrumentación) | `src/test/*`, `src/androidTest/CocinaViewTest.kt` |
| Corrutinas (`launch`, `delay`, `Job.cancel`, `Dispatchers.IO`, `Flow`) | monitoreo automático en `BluetoothViewModel`; lectura del socket en `BluetoothRepositorio` |
| Web-API con Retrofit + `sealed interface UiState` (Práctica 2) | `network/*`, `BusquedaViewModel`, pantalla Recetas |
| Imágenes asíncronas (Coil) | fotos de TheMealDB en Recetas, Recetario y detalle |
| SQLite / Room + navegación (Ejercicio 10, Práctica 3) | `room/*`, `RecetarioViewModel`, `HistorialViewModel`, `navigation/NavManager.kt` |
| Interfaz software-hardware (puerto serie, Bluetooth, ESP32, LM35) | `repositorio/*`, pantallas Cocina y Dispositivos |
| MVVM con capa `domain` y tema Material personalizado | toda la app; `ui/theme/*` |

Se dejaron fuera a propósito Firebase (requiere cuenta y `google-services.json`) y Cámara (aún no se ve
en clase); ambas quedan como extensiones (foto del platillo terminado, recetario compartido en Firestore).

## 3. Arquitectura (MVVM)

```
com.example.pfinalv1
├── MainActivity.kt          crea la BD (singleton) y arranca NavManager con los dos DAO
├── model/                   Receta + Ingrediente (API) · RecetaGuardada (@Entity) · Lectura (@Entity) · DispositivoBluetooth
├── domain/                  lógica pura y probada: Receta → RecetaGuardada, objetivo sugerido por categoría,
│                            tramas "24.36C", °C→°F/K, nivel respecto al objetivo, termostato con histéresis, estadísticas
├── data/                    recetasDeMuestra, ObjetivosCoccion (temperaturas seguras), SimuladorEsp32, lecturas de muestra
├── network/                 MealApiService (Retrofit), MealDto (Gson), RetrofitClient (TheMealDB)
├── room/                    RecetaDao · LecturaDao · AppDatabase ("comemejor_db", 2 tablas)
├── repositorio/             BluetoothRepositorio (socket SPP, lectura/escritura), DispositivosRepositorio
├── viewmodel/               RecetarioViewModel · BusquedaViewModel · BluetoothViewModel · DispositivosViewModel · HistorialViewModel
├── navigation/              NavManager (un Scaffold, NavigationBar, Snackbar) · Rutas
└── ui/
    ├── theme/               Color.kt · Theme.kt · Type.kt
    ├── components/          BarraSuperior, TarjetaReceta, Tarjetas, Estrellas, Etiquetas, Estados, PermisosBluetooth
    └── views/               RecetarioView · RecetasView · DetalleRecetaView · CocinaView · DispositivosView · HistorialView · DetalleLecturaView
```

Reglas que se siguieron (las que el profe revisa):

- **Un ViewModel por tarea distinta**: recetario (Room), API, comunicación con el ESP32, lista de emparejados e historial.
- **Las vistas no conocen al NavController ni a los repositorios**: reciben estado y lambdas
  (`CocinaContent(estado, acciones)`), por eso se pueden previsualizar (`@Preview`) y probar.
- **Operaciones en `domain`**: si cambia el formato de la trama, la regla del termostato o la tabla de
  temperaturas seguras, solo se tocan `ConversionTemperatura.kt` / `ConversionReceta.kt` y sus pruebas.
- **Patrón de repositorio para Bluetooth**, igual que `ConectaESP32` del profe: `EstadoConexion`
  (Inactivo / Conectando / Conectado / Error / Desconectado) en un `StateFlow` y los bytes recibidos en un `SharedFlow`.

## 4. Pantallas

| Pestaña | Qué hace |
|---|---|
| **Recetario** (inicio) | Tabla `recetas_guardadas` desde Room (`Flow` → la lista se actualiza sola), consulta de un registro por id, botón a Recetas. Arranca con 5 recetas de muestra. |
| **Recetas** | TheMealDB: buscar por nombre, receta aleatoria o **explorar una categoría** (Vegetarian, Vegan, Seafood, Chicken, Pasta…) en una cuadrícula de fotos; tocar una abre la receta completa; *Guardar en mi recetario* → INSERT. |
| **Receta #id** | Foto, ingredientes e instrucciones; columnas con su tipo de dato; UPDATE (estrellas, favorita, +1 cocinada, notas); DELETE con confirmación; **Cocinar con el termómetro** → fija la receta en curso y el objetivo sugerido por su categoría; últimas temperaturas registradas con esa receta. |
| **Cocina** | Estado de la conexión; receta en curso; lectura grande del LM35 con nivel *Lejos / Calentando / Casi listo / ¡Listo!*; aviso animado al alcanzar el objetivo; *Leer temperatura* (`'t'`) y *Monitoreo automático* (corrutina cada 2 s); **objetivos de cocción** predefinidos (chips) y ajuste − / +; LED manual (`'1'`/`'0'`) o **automático al llegar al objetivo** (histéresis 0.5 °C); modo simulación con *Enfriar* / *Calentar*; *Guardar lectura*; bitácora de comandos. |
| **Dispositivos** | Permisos (`BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` en Android 12+), lista de emparejados con los ESP32 primero, conectar; ayuda con el kit y el protocolo. |
| **Historial** / **Lectura #id** | Temperaturas guardadas (ligadas a su receta y objetivo), resumen (promedio, mínima, máxima, objetivos alcanzados), nota editable, eliminar, vaciar. |

## 5. Hardware y protocolo

Kit del curso (`ESP32Pack`): ESP32 DevKit V1 (WROOM-32, 30 pines), LM35 (TO-92), LED, protoboard y Dupont.
Sketch: `EBESP32_LED_Bt.ino` del profe, sin cambios.

| Elemento | Pin | Nota |
|---|---|---|
| LM35 Vout | GPIO34 (ADC) | `temperaturaC = lecturaADC * 0.081` (≈ 3.3 V / 4095 × 100 °C/V); rango del LM35: −55 a 150 °C |
| LM35 +Vs / GND | VIN (5 V) / GND | su salida (10 mV/°C) es segura para el ADC |
| LED | GPIO5 → resistencia 220–330 Ω → GND | el diagrama del pack lo pone directo; conviene la resistencia |
| Bluetooth | `BluetoothSerial`, nombre `ESP32_BT_Serial` | Bluetooth **clásico** (SPP, UUID `00001101-…`); no sirve en ESP32-S2/S3/C3 |

Protocolo de un carácter: `'1'` enciende el LED · `'0'` lo apaga · `'t'` responde la temperatura con dos
decimales seguida de `C` (ej. `24.36C`). Como el socket entrega los bytes sin formato, la app acumula lo
recibido y corta cada trama en la letra `C` (`separarTramas`), así no importa si `24.36C` llega partido.

**Uso en la cocina:** el LM35 va dentro de una funda/sonda sellada (o se mide el líquido de cocción con el
sensor protegido); el LED del ESP32 queda a la vista como indicador de "¡listo!".

### Cómo probarlo

1. Cargar el sketch en el ESP32 (Arduino IDE, placa *ESP32 Dev Module*). El monitor serie a 115200 debe decir *Bluetooth iniciado*.
2. En el teléfono: Ajustes → Bluetooth → emparejar **ESP32_BT_Serial** (una sola vez).
3. **Recetas** → categoría *Chicken* → tocar una → *Guardar en mi recetario*.
4. **Recetario** → abrir la receta → *Cocinar con el termómetro* (fija objetivo 74 °C y va a Cocina).
5. **Cocina** → *Elegir dispositivo* → conceder permisos → ESP32 → **Conectar**. Activar *Monitoreo automático* y *LED automático*; calentar el sensor hasta ver *¡Listo!* y el LED encendido. *Guardar lectura*.
6. **Historial** → la lectura aparece con la receta y el objetivo; en el detalle de la receta se listan sus últimas temperaturas.
7. Sin tarjeta: activar **Modo simulación** en Cocina y usar *Calentar +10°*; todo lo anterior funciona igual.

## 6. Pruebas

- **Locales** (`src/test`, corren sin emulador): `ConversionTemperaturaTest` (tramas partidas, conversiones,
  niveles respecto al objetivo, termostato con histéresis, formato), `ConversionRecetaTest` (API → Room,
  ingredientes, objetivo sugerido por categoría, DTO con nulos) y `EstadisticasLecturasTest` (resumen, objetivos alcanzados, simulador).
- **Instrumentación** (`src/androidTest`): `CocinaViewTest` dibuja `CocinaContent` con un estado fijo y
  verifica la lectura "68.4 °C", el nivel, el aviso de "listo" y que *Leer temperatura* dispare su evento
  (y no lo haga cuando está deshabilitado).

Ejecutar: clic derecho en la carpeta `test` o `androidTest` → *Run tests*, o `./gradlew test` y `./gradlew connectedAndroidTest`.

## 7. Decisiones justificadas

- **API TheMealDB** (como en las prácticas 2 y 3): gratuita, sin registro (key pública "1"), JSON sencillo,
  fotos incluidas, categorías que sirven para comer mejor (Vegetarian, Vegan, Seafood…). Sus 40 campos
  `strIngredientN/strMeasureN` se convierten en una lista en `MealDto.aReceta()`.
- **Room y no Firebase**: el recetario y las lecturas deben funcionar en la cocina sin internet y son
  personales; Room es el ORM oficial sobre SQLite, valida el SQL al compilar y entrega `Flow`. Firestore
  tendría sentido para compartir recetas entre personas (extensión futura).
- **Esquema**: `recetas_guardadas` (Int, String, Float, Boolean, Long) y `lecturas` (Int, Double, Long,
  String, Boolean). La lectura guarda el **nombre** de la receta y no su id para que sobreviva aunque la
  receta se borre. La fecha es `Long` (ordena bien con `ORDER BY`). Cambiar el esquema obliga a subir
  `version` y escribir una `Migration`.
- **Objetivos de cocción**: temperaturas internas mínimas seguras de la FDA/USDA; el hervor se ajustó a la
  altitud de la CDMX (≈ 92 °C). Cada categoría de TheMealDB sugiere un objetivo (`objetivoSugerido`).
- **Histéresis en el LED automático**: sin ella parpadearía cuando la lectura oscila alrededor del objetivo;
  es el mismo principio de un termostato.
- **Tema Material propio** (Material You apagado): *primary* azul acero (instrumento de cocina),
  *secondary* verde hoja (alimentación saludable y datos en vivo del ESP32), *tertiary* ámbar (calor y
  alertas). Colores fijos para los niveles de cocción (azul/verde/naranja/rojo) y el LED. Tipografía: la
  lectura principal en **monoespaciada** para que los dígitos no "bailen" al actualizarse.
- **Bluetooth clásico (SPP) y solo dispositivos emparejados**: es lo que usa `BluetoothSerial` en el
  ESP32 y evita implementar descubrimiento; emparejar desde Ajustes es un paso único.
- **Modo simulación**: permite desarrollar en el emulador (no tiene Bluetooth clásico), hacer la demo si
  falla la tarjeta y correr las pruebas de instrumentación sin hardware.

## 8. Pendientes por pulir (siguiente versión)

- Botón "Activar Bluetooth" con `BluetoothAdapter.ACTION_REQUEST_ENABLE` (hoy solo avisa que está apagado).
- Reconexión automática si el ESP32 se apaga y vuelve; hoy hay que tocar *Conectar* otra vez.
- Gráfica de la temperatura en el tiempo durante la cocción (hoy solo lectura actual y lista).
- Sumar automáticamente "+1 cocinada" a la receta cuando se alcanza el objetivo con ella en curso.
- El objetivo y el modo simulación se pierden al cerrar la app (guardarlos con DataStore).
- Buscar también por ingrediente y por área (TheMealDB lo permite); textos de las vistas portadas de la
  Práctica 3 todavía en código (moverlos a `strings.xml`).
- Aviso de privacidad (la app no envía datos personales, pero la rúbrica lo recomienda) y preparación para Play Store.
- Foto del platillo terminado con la cámara (tema del 10-nov) y recetario compartido en Firestore.

## 9. Dependencias

Compose BOM 2026.02.01 · Material 3 · Navigation Compose 2.10.2 · Lifecycle 2.11.0 · Room 2.8.5 (KSP 2.3.6)
· Retrofit 3.0.0 + converter-gson · OkHttp logging-interceptor 4.12.0 · Coil 3.3.0. AGP 9.5.0 · Kotlin 2.2.10 · Gradle 9.6.
Íconos: vectores propios en estilo de línea basados en Feather Icons (MIT). Fotos: TheMealDB.
