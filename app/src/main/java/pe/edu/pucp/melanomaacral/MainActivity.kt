package pe.edu.pucp.melanomaacral

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.edu.pucp.melanomaacral.ui.navigation.AppNavigation
import pe.edu.pucp.melanomaacral.ui.screens.access.AccessScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelanomaAcralTheme {
                // RNF-12 (P-14): sin sesión se muestra el ingreso; con sesión, la aplicación.
                val access = (application as MelanomaAcralApp).container.accessRepository
                val user by access.currentUser.collectAsState()
                val scope = rememberCoroutineScope()
                if (user == null) AccessScreen()
                else AppNavigation(currentUser = user, onLogout = { scope.launch { access.logout() } })
            }
        }
    }
}
