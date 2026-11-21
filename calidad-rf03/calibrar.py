"""Calibración de RF-03 para fotos clínicas del teléfono (tanda 1) y prueba (tanda 2).

Entrada: medidas.csv, que produce CalidadEnDispositivoTest en el teléfono con el mismo código de la app.
Salida: umbrales.json y RESULTADO.md.

Regla (la misma para cada medida). Para cada medida y su tipo de foto mala, el umbral se pone en el punto medio
entre la foto adecuada de la tanda 1 más cercana al lado malo y la foto mala más cercana que queda más allá de ella.
Así ninguna foto adecuada de la tanda 1 se rechaza por esa medida, y se detecta el mayor número posible de fotos
malas de ese tipo. Si ninguna foto mala queda más allá de las adecuadas, el umbral no cambia.
La tanda 2 no se usa para fijar nada: solo se evalúa con los umbrales ya fijados.
"""
import csv
import json
import sys

ACTUALES = {  # umbrales vigentes antes de esta calibración (ImageQualityAnalyzer)
    "nitidez": 5.0, "nitidez_suavizada": 1.9, "brillo_min": 80.0, "brillo_max": 200.0,
    "fraccion_oscura_max": 0.25, "fraccion_clara_max": 0.25, "desv_centro_min": 8.0,
}
# medida, tipo de foto mala, columna, dirección («min»: lo malo es un valor bajo; «max»: un valor alto)
MEDIDAS = [
    ("nitidez", "borrosa", "nitidez", "min"),
    ("nitidez_suavizada", "borrosa", "nitidez_suavizada", "min"),
    ("brillo_min", "oscura", "brillo_medio", "min"),
    ("fraccion_oscura_max", "oscura", "fraccion_oscura", "max"),
    ("brillo_max", "sobreexpuesta", "brillo_medio", "max"),
    ("fraccion_clara_max", "sobreexpuesta", "fraccion_clara", "max"),
    ("desv_centro_min", "encuadre", "desv_centro", "min"),
]
COLS = ["nitidez", "nitidez_suavizada", "brillo_medio", "fraccion_oscura", "fraccion_clara", "desv_centro"]
TIPOS = ["adecuada", "borrosa", "oscura", "sobreexpuesta", "encuadre"]


def leer(path):
    filas = list(csv.DictReader(open(path)))
    for f in filas:
        for c in COLS:
            f[c] = float(f[c])
    return filas


def calibrar(t1):
    umbrales, notas = {}, {}
    for nombre, malo, col, dirc in MEDIDAS:
        buenas = [f[col] for f in t1 if f["tipo"] == "adecuada"]
        malas = [f[col] for f in t1 if f["tipo"] == malo]
        if dirc == "min":
            borde = min(buenas)
            mas_alla = [v for v in malas if v < borde]
            if mas_alla:
                umbrales[nombre] = round((borde + max(mas_alla)) / 2, 3)
        else:
            borde = max(buenas)
            mas_alla = [v for v in malas if v > borde]
            if mas_alla:
                umbrales[nombre] = round((borde + min(mas_alla)) / 2, 3)
        if nombre not in umbrales:
            umbrales[nombre] = ACTUALES[nombre]
            notas[nombre] = "sin cambio: ninguna foto mala de la tanda 1 queda más allá de las adecuadas"
        else:
            notas[nombre] = (f"punto medio entre la adecuada más cercana ({borde:.3f}) y la {malo} más cercana más allá "
                             f"({max(mas_alla) if dirc == 'min' else min(mas_alla):.3f}); detecta {len(mas_alla)} de "
                             f"{len(malas)} en la tanda 1")
    return umbrales, notas


def decidir(f, u, encuadre=True):
    foco = f["nitidez"] >= u["nitidez"] and f["nitidez_suavizada"] >= u["nitidez_suavizada"]
    oscura = f["brillo_medio"] < u["brillo_min"] or f["fraccion_oscura"] > u["fraccion_oscura_max"]
    clara = (not oscura) and (f["brillo_medio"] > u["brillo_max"] or f["fraccion_clara"] > u["fraccion_clara_max"])
    enc = (not encuadre) or f["desv_centro"] >= u["desv_centro_min"]
    return {"foco": foco, "oscura": oscura, "clara": clara, "encuadre": enc,
            "adecuada": foco and not oscura and not clara and enc}


def detectada(tipo, d):
    return {"adecuada": d["adecuada"], "borrosa": not d["foco"], "oscura": d["oscura"],
            "sobreexpuesta": d["clara"], "encuadre": not d["encuadre"]}[tipo]


def tabla(filas, u, titulo):
    out = [f"### {titulo}", "", "| Tipo | Fotos | Resultado esperado | Correctas | Marcadas por otro criterio |",
           "|---|---|---|---|---|"]
    for t in TIPOS:
        fs = [f for f in filas if f["tipo"] == t]
        ds = [decidir(f, u) for f in fs]
        ok = sum(detectada(t, d) for d in ds)
        otro = sum((not d["adecuada"]) and not detectada(t, d) for d in ds) if t != "adecuada" else 0
        esperado = "aceptada" if t == "adecuada" else f"marcada por {t}"
        out.append(f"| {t} | {len(fs)} | {esperado} | {ok} de {len(fs)} | {otro if t != 'adecuada' else '—'} |")
    return out


def main(path="medidas.csv"):
    filas = leer(path)
    t1 = [f for f in filas if f["tanda"] == "tanda1"]
    t2 = [f for f in filas if f["tanda"] == "tanda2"]
    u, notas = calibrar(t1)
    json.dump({"umbrales_foto_clinica": u, "reglas": notas, "umbrales_anteriores": ACTUALES},
              open("umbrales.json", "w"), indent=2, ensure_ascii=False)
    md = ["# RF-03: calibración con fotos del teléfono", "",
          "Fotos clínicas de lunares tomadas con la cámara del teléfono de prueba, originales (4080×3056), "
          "medidas en el teléfono con el mismo código de la app (`CalidadEnDispositivoTest`). "
          "Tanda 1 (calibración) y tanda 2 (prueba), repartidas al azar con semilla fija (`reparto.csv`). "
          "La regla de calibración está en `calibrar.py`.", "",
          "Las fotos no se publican, porque son de la piel de una persona; `reparto.csv` da el sha256 de cada una. "
          "Para repetir el cálculo: `python3 calibrar.py` (lee `medidas.csv`; escribe `umbrales.json` y este informe). "
          "Para medir otras fotos en un teléfono: `CalidadEnDispositivoTest`, en `app/src/androidTest`.", "",
          "## Umbrales para la foto clínica", "", "| Umbral | Anterior | Nuevo | Regla |", "|---|---|---|---|"]
    for nombre, *_ in MEDIDAS:
        md.append(f"| {nombre} | {ACTUALES[nombre]} | {u[nombre]} | {notas[nombre]} |")
    md += ["", "La imagen dermoscópica conserva los umbrales anteriores, calibrados con 600 imágenes dermatoscópicas "
           "del dataset.", "", "## Resultados", ""]
    md += tabla(t1, ACTUALES, "Tanda 1 con los umbrales anteriores") + [""]
    md += tabla(t1, u, "Tanda 1 con los umbrales nuevos (calibración)") + [""]
    md += tabla(t2, ACTUALES, "Tanda 2 con los umbrales anteriores") + [""]
    md += tabla(t2, u, "Tanda 2 con los umbrales nuevos (prueba)") + [""]
    md += ["El encuadre se evalúa con el criterio de la imagen dermoscópica (desviación del brillo en el cuarto "
           "central), porque la app no lo revisa en la foto clínica.", "",
           "## Fotos de la tanda 2 con resultado distinto del esperado (umbrales nuevos)", "",
           "| Tipo | Archivo | Nitidez | Brillo medio | Fracción clara | Criterio que falló o faltó |", "|---|---|---|---|---|---|"]
    for f in t2:
        d = decidir(f, u)
        if detectada(f["tipo"], d):
            continue
        marcas = [n for n, k in (("borrosa", "foco"), ("encuadre", "encuadre")) if not d[k]]
        marcas += ["oscura"] if d["oscura"] else []
        marcas += ["sobreexpuesta"] if d["clara"] else []
        motivo = ("rechazada por: " + ", ".join(marcas)) if f["tipo"] == "adecuada" else (
            "no marcada por " + f["tipo"] + ("; marcada por: " + ", ".join(marcas) if marcas else "; aceptada"))
        md.append(f"| {f['tipo']} | {f['archivo']} | {f['nitidez']:.1f} | {f['brillo_medio']:.1f} | "
                  f"{f['fraccion_clara']:.3f} | {motivo} |")
    if "clinica_ok" in filas[0]:
        n_cl = sum(str(decidir(f, u, encuadre=False)["adecuada"]).lower() == f["clinica_ok"] for f in filas)
        n_de = sum(str(decidir(f, ACTUALES)["adecuada"]).lower() == f["dermo_ok"] for f in filas)
        md += ["", f"**Comprobación:** la decisión que dio la app en el teléfono coincide con la de este script en "
               f"{n_cl} de {len(filas)} fotos como foto clínica y en {n_de} de {len(filas)} como imagen dermoscópica."]
    md += ["", "## Limitaciones", "",
           "- Cinco fotos por tipo para calibrar y cinco o seis para probar, de una sola persona, un solo teléfono y "
           "lunares de piel no acral (brazo y tronco): cada foto cambia el resultado de su tipo en 17 a 20 puntos porcentuales.",
           "- La regla no rechaza ninguna foto adecuada de la tanda 1, así que los umbrales de brillo quedan en el borde "
           "de esas cinco fotos; en la tanda 2 rechazan fotos adecuadas algo más oscuras.",
           "- El tipo de cada foto es el que le dio quien la tomó. La cámara corrige la exposición, así que varias fotos "
           "tomadas con poca luz no tienen un brillo medio bajo.",
           "- La medida de encuadre detecta un centro uniforme (dermatoscopio sin apoyar, lente tapada), no una lesión "
           "fuera del centro.",
           "- Los umbrales de la imagen dermoscópica no se probaron con fotos de un dermatoscopio, porque no se dispuso "
           "de uno.", ""]
    open("RESULTADO.md", "w").write("\n".join(md))
    print("\n".join(md))


if __name__ == "__main__":
    main(*sys.argv[1:])
