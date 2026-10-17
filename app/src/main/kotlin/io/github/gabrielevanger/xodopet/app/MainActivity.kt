package io.github.gabrielevanger.xodopet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.feature.storefront.StorefrontRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Desenha atrás das barras do sistema; as telas usam safeDrawingPadding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            XodoTheme {
                // Navegação entra quando houver a segunda tela (página da loja).
                StorefrontRoute()
            }
        }
    }
}
