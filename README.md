# Melanoma acral: aplicativo móvil de apoyo al tamizaje

Prototipo Android de la tesis *Diseño e implementación de un aplicativo móvil impulsado por inteligencia artificial para el apoyo al tamizaje de melanoma acral* (Pontificia Universidad Católica del Perú, 2026). Ayuda al personal de salud del primer nivel de atención a:

- capturar una foto clínica y una imagen dermatoscópica de una lesión acral;
- obtener una recomendación en dos niveles (derivar o control), con el resultado de un modelo de IA frente a su umbral y su mapa de calor;
- programar controles y preparar la derivación en PDF.

Todo funciona **sin conexión**: el modelo corre en el teléfono.

> **Prototipo de investigación.** No es un dispositivo médico, no reemplaza el juicio clínico y no debe usarse para decisiones clínicas.

## Modelo

EfficientNet-B0 entrenado en la tesis y exportado a ONNX (se ejecuta con ONNX Runtime en la CPU del teléfono). El código de entrenamiento está en el repositorio del modelo.

## Compilar

Requisitos: JDK 17 o superior, Android SDK 37 y un teléfono Android 8 o superior con arquitectura arm64 (la APK incluye solo `arm64-v8a`).

```bash
bash scripts/obtener_modelos.sh      # descarga los modelos de la Release y verifica su sha256
./gradlew assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Los modelos (`app/src/main/assets/modelos/*.onnx` y `*.tflite`) no están en el historial: los trae `scripts/obtener_modelos.sh`.

## Pruebas

- **Unitarias (JVM):** `./gradlew testDebugUnitTest`. Incluyen las reglas de recomendación, la persistencia, la revisión de calidad de la imagen, las capturas de pantalla y la paridad del preprocesado.
- **En el teléfono:** `ModeloB0EnDispositivoTest` (latencia y paridad), `MigracionBaseDeDatosTest` y `RecordatorioNotificacionTest`. Se instalan y ejecutan con `adb`; no uses `connectedAndroidTest`, que desinstala la aplicación y borra sus datos.
  ```bash
  ./gradlew assembleDebug assembleDebugAndroidTest
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  adb shell am instrument -w -e class pe.edu.pucp.melanomaacral.ModeloB0EnDispositivoTest \
      pe.edu.pucp.melanomaacral.test/androidx.test.runner.AndroidJUnitRunner
  ```

## Estructura

| Carpeta | Contenido |
|---|---|
| `app/src/main/java/.../domain` | Modelos, reglas de recomendación y revisión de calidad de la imagen (Kotlin puro, probado en la JVM). |
| `app/src/main/java/.../data` | Base de datos local, almacenamiento de imágenes, modelo en el teléfono, PDF de derivación y recordatorios. |
| `app/src/main/java/.../ui` | Pantallas, componentes y tema (Jetpack Compose y Material 3). |
| `app/src/test` y `app/src/androidTest` | Pruebas unitarias y pruebas en el teléfono. |
| `scripts/` | Descarga de los modelos de la Release. |
| `trazabilidad-r2/` | Anexo documental de trazabilidad de los requisitos (Anexo J de la tesis): 32 requisitos, 416 pares requisito–fuente con su cita textual. |

## Licencia

MIT (ver `LICENSE`); la licencia cubre el código. Las imágenes de ejemplo de las pruebas mantienen las licencias de sus fuentes de origen. Las citas textuales de `trazabilidad-r2/` pertenecen a sus autores.
