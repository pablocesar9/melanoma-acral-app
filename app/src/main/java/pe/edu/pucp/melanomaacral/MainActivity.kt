package pe.edu.pucp.melanomaacral

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.edu.pucp.melanomaacral.ui.navigation.AppNavigation
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelanomaAcralTheme {
                AppNavigation()
            }
        }
    }
}
