package cl.samuel.barzarena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import cl.samuel.barzarena.ui.BarzarenaApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Hilt se encarga de todo. No se necesita crear nada manualmente.
                BarzarenaApp()
            }
        }
    }
}
