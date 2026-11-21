# RF-03: calibración con fotos del teléfono

Fotos clínicas de lunares tomadas con la cámara del teléfono de prueba, originales (4080×3056), medidas en el teléfono con el mismo código de la app (`CalidadEnDispositivoTest`). Tanda 1 (calibración) y tanda 2 (prueba), repartidas al azar con semilla fija (`reparto.csv`). La regla de calibración está en `calibrar.py`.

Las fotos no se publican, porque son de la piel de una persona; `reparto.csv` da el sha256 de cada una. Para repetir el cálculo: `python3 calibrar.py` (lee `medidas.csv`; escribe `umbrales.json` y este informe). Para medir otras fotos en un teléfono: `CalidadEnDispositivoTest`, en `app/src/androidTest`.

## Umbrales para la foto clínica

| Umbral | Anterior | Nuevo | Regla |
|---|---|---|---|
| nitidez | 5.0 | 32.941 | punto medio entre la adecuada más cercana (42.135) y la borrosa más cercana más allá (23.746); detecta 5 de 5 en la tanda 1 |
| nitidez_suavizada | 1.9 | 3.629 | punto medio entre la adecuada más cercana (3.723) y la borrosa más cercana más allá (3.534); detecta 5 de 5 en la tanda 1 |
| brillo_min | 80.0 | 123.564 | punto medio entre la adecuada más cercana (124.116) y la oscura más cercana más allá (123.012); detecta 3 de 5 en la tanda 1 |
| fraccion_oscura_max | 0.25 | 0.25 | sin cambio: ninguna foto mala de la tanda 1 queda más allá de las adecuadas |
| brillo_max | 200.0 | 156.872 | punto medio entre la adecuada más cercana (156.636) y la sobreexpuesta más cercana más allá (157.109); detecta 3 de 5 en la tanda 1 |
| fraccion_clara_max | 0.25 | 0.019 | punto medio entre la adecuada más cercana (0.000) y la sobreexpuesta más cercana más allá (0.038); detecta 4 de 5 en la tanda 1 |
| desv_centro_min | 8.0 | 8.0 | sin cambio: ninguna foto mala de la tanda 1 queda más allá de las adecuadas |

La imagen dermoscópica conserva los umbrales anteriores, calibrados con 600 imágenes dermatoscópicas del dataset.

## Resultados

### Tanda 1 con los umbrales anteriores

| Tipo | Fotos | Resultado esperado | Correctas | Marcadas por otro criterio |
|---|---|---|---|---|
| adecuada | 5 | aceptada | 5 de 5 | — |
| borrosa | 5 | marcada por borrosa | 0 de 5 | 0 |
| oscura | 5 | marcada por oscura | 1 de 5 | 0 |
| sobreexpuesta | 5 | marcada por sobreexpuesta | 1 de 5 | 1 |
| encuadre | 5 | marcada por encuadre | 0 de 5 | 0 |

### Tanda 1 con los umbrales nuevos (calibración)

| Tipo | Fotos | Resultado esperado | Correctas | Marcadas por otro criterio |
|---|---|---|---|---|
| adecuada | 5 | aceptada | 5 de 5 | — |
| borrosa | 5 | marcada por borrosa | 5 de 5 | 0 |
| oscura | 5 | marcada por oscura | 3 de 5 | 2 |
| sobreexpuesta | 5 | marcada por sobreexpuesta | 5 de 5 | 0 |
| encuadre | 5 | marcada por encuadre | 0 de 5 | 3 |

### Tanda 2 con los umbrales anteriores

| Tipo | Fotos | Resultado esperado | Correctas | Marcadas por otro criterio |
|---|---|---|---|---|
| adecuada | 5 | aceptada | 5 de 5 | — |
| borrosa | 6 | marcada por borrosa | 0 de 6 | 0 |
| oscura | 5 | marcada por oscura | 1 de 5 | 0 |
| sobreexpuesta | 5 | marcada por sobreexpuesta | 3 de 5 | 1 |
| encuadre | 5 | marcada por encuadre | 0 de 5 | 2 |

### Tanda 2 con los umbrales nuevos (prueba)

| Tipo | Fotos | Resultado esperado | Correctas | Marcadas por otro criterio |
|---|---|---|---|---|
| adecuada | 5 | aceptada | 2 de 5 | — |
| borrosa | 6 | marcada por borrosa | 5 de 6 | 1 |
| oscura | 5 | marcada por oscura | 3 de 5 | 1 |
| sobreexpuesta | 5 | marcada por sobreexpuesta | 4 de 5 | 0 |
| encuadre | 5 | marcada por encuadre | 0 de 5 | 4 |

El encuadre se evalúa con el criterio de la imagen dermoscópica (desviación del brillo en el cuarto central), porque la app no lo revisa en la foto clínica.

## Fotos de la tanda 2 con resultado distinto del esperado (umbrales nuevos)

| Tipo | Archivo | Nitidez | Brillo medio | Fracción clara | Criterio que falló o faltó |
|---|---|---|---|---|---|
| adecuada | IMG_20261120_230740.jpg | 130.3 | 111.8 | 0.000 | rechazada por: oscura |
| adecuada | IMG_20261120_230913.jpg | 95.5 | 100.1 | 0.000 | rechazada por: oscura |
| adecuada | IMG_20261120_230957.jpg | 25.3 | 127.4 | 0.000 | rechazada por: borrosa |
| borrosa | IMG_20261121_040520.jpg | 35.9 | 114.7 | 0.000 | no marcada por borrosa; marcada por: oscura |
| encuadre | IMG_20261121_040818.jpg | 40.7 | 160.4 | 0.000 | no marcada por encuadre; marcada por: sobreexpuesta |
| encuadre | IMG_20261121_040831.jpg | 7.7 | 131.2 | 0.000 | no marcada por encuadre; marcada por: borrosa |
| encuadre | IMG_20261121_040835.jpg | 48.0 | 122.5 | 0.000 | no marcada por encuadre; marcada por: oscura |
| encuadre | IMG_20261121_040856.jpg | 84.6 | 128.0 | 0.000 | no marcada por encuadre; aceptada |
| encuadre | IMG_20261121_040900.jpg | 107.4 | 98.7 | 0.000 | no marcada por encuadre; marcada por: oscura |
| oscura | IMG_20261121_041346.jpg | 79.1 | 133.1 | 0.000 | no marcada por oscura; aceptada |
| oscura | IMG_20261121_041428.jpg | 19.7 | 151.8 | 0.000 | no marcada por oscura; marcada por: borrosa |
| sobreexpuesta | IMG_20261121_041209.jpg | 55.7 | 130.0 | 0.000 | no marcada por sobreexpuesta; aceptada |

**Comprobación:** la decisión que dio la app en el teléfono coincide con la de este script en 51 de 51 fotos como foto clínica y en 51 de 51 como imagen dermoscópica.

## Limitaciones

- Cinco fotos por tipo para calibrar y cinco o seis para probar, de una sola persona, un solo teléfono y lunares de piel no acral (brazo y tronco): cada foto cambia el resultado de su tipo en 17 a 20 puntos porcentuales.
- La regla no rechaza ninguna foto adecuada de la tanda 1, así que los umbrales de brillo quedan en el borde de esas cinco fotos; en la tanda 2 rechazan fotos adecuadas algo más oscuras.
- El tipo de cada foto es el que le dio quien la tomó. La cámara corrige la exposición, así que varias fotos tomadas con poca luz no tienen un brillo medio bajo.
- La medida de encuadre detecta un centro uniforme (dermatoscopio sin apoyar, lente tapada), no una lesión fuera del centro.
- Los umbrales de la imagen dermoscópica no se probaron con fotos de un dermatoscopio, porque no se dispuso de uno.
