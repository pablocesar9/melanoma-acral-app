package pe.edu.pucp.melanomaacral.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import pe.edu.pucp.melanomaacral.AppContainer
import pe.edu.pucp.melanomaacral.MelanomaAcralApp

// Atajo para las Factory de los ViewModels: obtiene el AppContainer de la aplicación.
fun CreationExtras.appContainer(): AppContainer =
    (this[APPLICATION_KEY] as MelanomaAcralApp).container
