package pe.edu.pucp.melanomaacral

import android.app.Application

// Se crea una sola vez, antes que cualquier pantalla, y vive mientras la app esté abierta.
// Guarda el AppContainer para que todos los ViewModels compartan los mismos repositorios.
class MelanomaAcralApp : Application() {
    val container by lazy { AppContainer(this) }
}
