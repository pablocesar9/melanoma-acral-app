# Trazabilidad de los requisitos (R2)

Anexo documental de trazabilidad del resultado R2 del objetivo 3 de la tesis (Anexo J). Reúne, para cada uno de los 32 requisitos de la aplicación, las fuentes que lo respaldan y la cita textual, con su página, que se eligió en cada par requisito–fuente: 416 pares en total.

Fuentes: 8 normas (N-xx), 6 documentos institucionales (I-xx), la entrevista P02 y 121 documentos de la literatura (identificados con el código que recibieron en el registro de la búsqueda). La referencia completa de cada fuente está en `trazabilidad-R2-fuentes.csv` y en el Anexo J de la tesis.

## Archivos

`Trazabilidad-R2.xlsx` es el libro completo. Cada CSV (UTF-8, separado por comas) corresponde a una de sus hojas.

| Archivo | Contenido |
|---|---|
| `trazabilidad-R2-requisitos.csv` | Los 32 requisitos: texto, clase (firme o no), número de fuentes por tipo y si la clase se sostiene con respaldo total o por síntesis. |
| `trazabilidad-R2-trazabilidad.csv` | Los 416 pares requisito–fuente: cita textual, página o fila, respaldo (total o parcial), cómo se eligió la cita y nota. |
| `trazabilidad-R2-síntesis.csv` | Requisitos firmes por síntesis (RF-09, RF-11, RF-14 y RNF-07): cada parte del requisito con la cita que la respalda. |
| `trazabilidad-R2-citas.csv` | Citas extraídas de cada documento, por tema o apartado, de las que se eligió la cita de cada par. |
| `trazabilidad-R2-fuentes.csv` | Código, tipo y referencia de cada fuente. |
| `SHA256SUMS` | Suma sha256 de cada archivo (`sha256sum -c SHA256SUMS`). |

## Criterio

Un requisito es firme si una norma vinculante o al menos dos fuentes primarias lo respaldan del todo, o si cada una de sus partes tiene al menos una cita de la revisión (síntesis). El protocolo de la revisión está en el Anexo I de la tesis.

## Uso de las citas

Las citas textuales pertenecen a sus autores y se reproducen como cita, con su referencia, con fines de investigación. La licencia MIT del repositorio cubre el código; esta carpeta no incluye los documentos originales.
