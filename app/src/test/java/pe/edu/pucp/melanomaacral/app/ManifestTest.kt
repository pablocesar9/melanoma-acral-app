package pe.edu.pucp.melanomaacral.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// RNF-01, RNF-06, RNF-05: la app no pide Internet, no se respalda en la nube y la cámara no es obligatoria.
// Gradle ejecuta las pruebas con la carpeta del módulo como directorio de trabajo.
class ManifestTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test fun noPideInternet() {
        assertFalse(manifest.contains("android.permission.INTERNET"))
    }

    @Test fun noPermiteRespaldoEnLaNube() {
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
    }

    @Test fun noPideAccesoALaGaleria() {
        assertFalse(manifest.contains("READ_MEDIA_IMAGES"))
        assertFalse(manifest.contains("READ_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("WRITE_EXTERNAL_STORAGE"))
    }
}
