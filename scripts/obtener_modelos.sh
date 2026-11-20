#!/usr/bin/env bash
# Descarga los modelos del teléfono y el paquete de paridad desde la Release de este repositorio, verifica su sha256 y
# los deja donde los espera la compilación:
#   app/src/main/assets/modelos/b0_melanoma_gradcam{,_capas}.onnx y .tflite   (la aplicación)
#   app/src/androidTest/assets/paridad/                              (ModeloB0EnDispositivoTest)
# Sin red: DESDE=/ruta/a/la/carpeta/del/release bash scripts/obtener_modelos.sh   (copia desde una carpeta local)
set -euo pipefail
cd "$(dirname "$(readlink -f "$0")")/.."
REPO="${REPO:-pablocesar9/melanoma-acral-app}"; TAG="${TAG:-v1.0.0}"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
NOMBRES="b0_melanoma_gradcam.onnx b0_melanoma_gradcam_capas.onnx b0_melanoma_gradcam.tflite paquete_paridad.tgz SHA256SUMS"
for n in $NOMBRES; do
  if [ -n "${DESDE:-}" ]; then cp "$DESDE/$n" "$T/"
  else curl -fsSL "https://github.com/$REPO/releases/download/$TAG/$n" -o "$T/$n"; fi
done
(cd "$T" && sha256sum -c SHA256SUMS)
mkdir -p app/src/main/assets/modelos app/src/androidTest/assets/paridad
cp "$T"/b0_melanoma_gradcam.onnx "$T"/b0_melanoma_gradcam_capas.onnx "$T"/b0_melanoma_gradcam.tflite app/src/main/assets/modelos/
tar xzf "$T/paquete_paridad.tgz" -C "$T"
# La prueba en el teléfono solo usa, de cada caso del paquete, la foto original, la entrada esperada y los valores.
for d in "$T"/paridad/*/; do n=$(basename "$d"); mkdir -p "app/src/androidTest/assets/paridad/$n"
  cp "$d"/original.* "$d"/entrada_modelo_384.png "$d"/valores.json "app/src/androidTest/assets/paridad/$n/"; done
echo "Listo: modelos en app/src/main/assets/modelos y $(ls app/src/androidTest/assets/paridad | wc -l) casos de paridad."
