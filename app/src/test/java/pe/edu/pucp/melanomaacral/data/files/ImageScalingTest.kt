package pe.edu.pucp.melanomaacral.data.files

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageScalingTest {
    @Test fun imagenPequena_noSeReduce() = assertEquals(1, ImageScaling.sampleSize(1600, 1200, 2048))
    @Test fun imagenGrande_seReduceEnPotenciasDeDos() {
        assertEquals(1, ImageScaling.sampleSize(4000, 3000, 2048))
        assertEquals(2, ImageScaling.sampleSize(8000, 6000, 2048))
        assertEquals(4, ImageScaling.sampleSize(16000, 12000, 2048))
    }
    @Test fun fit_conservaLaProporcion() {
        assertEquals(2048 to 1536, ImageScaling.fit(4000, 3000, 2048))
        assertEquals(1536 to 2048, ImageScaling.fit(3000, 4000, 2048))
    }
    @Test fun fit_noAmplia() = assertEquals(800 to 600, ImageScaling.fit(800, 600, 2048))
}
