package com.example.climapulse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.climapulse.ui.navigation.ClimaPulseNavHost
import com.example.climapulse.ui.theme.ClimaPulseTheme

// FragmentActivity es requerido por BiometricPrompt.
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.startDestination.value == null }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction { provider.remove() }
                .start()
        }
        // Barras del sistema oscuras, continuas con la Splash y el tema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            ClimaPulseTheme {
                val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
                // Surface raíz: define fondo y color de contenido por defecto (onBackground).
                Surface(color = MaterialTheme.colorScheme.background) {
                    startDestination?.let { ClimaPulseNavHost(startDestination = it) }
                }
            }
        }
    }
}
