package pe.edu.pucp.melanomaacral.data.real

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

// Paridad de RF-05 con Python: los parámetros son los mismos archivos que usa la app y los puntajes esperados se
// calcularon con numpy (6 imágenes del entrenamiento y 3 vectores alterados, uno de ellos fuera de alcance).
class FueraDeAlcanceB0Test {
    private val dir = File("src/main/assets/modelos/fuera_de_alcance")
    private val lineas = File("src/test/resources/fuera_de_alcance_casos.txt").readLines().filter { !it.startsWith("#") }
    private val sesgo = lineas[0].split(" ")[0].toDouble()
    private val umbral = lineas[0].split(" ")[1].toDouble()
    private val regla = FueraDeAlcanceB0(
        FueraDeAlcanceB0.flotantes(File(dir, "media.f32").readBytes()),
        FueraDeAlcanceB0.flotantes(File(dir, "desv.f32").readBytes()),
        FueraDeAlcanceB0.flotantes(File(dir, "pesos.f32").readBytes()),
        sesgo, umbral
    )

    @Test
    fun puntajesYDecisionesIgualesAPython() {
        val casos = lineas.drop(1)
        assertEquals(9, casos.size)
        var fuera = 0
        for (c in casos) {
            val (p, f, v) = c.split(";")
            val capas = v.split(",").map { it.toFloat() }.toFloatArray()
            assertEquals(2064, capas.size)
            assertEquals(p.toDouble(), regla.puntaje(capas), 1e-3)
            assertEquals(f.toBoolean(), regla.fueraDeAlcance(capas))
            if (f.toBoolean()) fuera++
        }
        assertEquals(1, fuera)
    }
}
