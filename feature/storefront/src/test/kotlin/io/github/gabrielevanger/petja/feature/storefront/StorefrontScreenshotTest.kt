package io.github.gabrielevanger.petja.feature.storefront

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests da tela Storefront usando Roborazzi.
 *
 * Para gerar/atualizar as imagens de baseline, execute:
 *   ./scripts/record-screenshots.sh
 *
 * Para verificar regressões visuais contra o baseline, execute:
 *   ./gradlew verifyRoborazziDebug
 *
 * As imagens são salvas em src/test/snapshots/ e devem ser comitadas no repositório.
 * O modo gráfico nativo é obrigatório: sem ele o Robolectric não desenha pixels e a imagem
 * vira um diagrama da árvore de semântica.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumPhone)
class StorefrontScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val lugares = BoaVistaSamplePlaces.toImmutableList()

    private fun exibir(conteudo: StorefrontContent) {
        composeRule.setContent {
            PetjaTheme {
                StorefrontScreen(
                    state = StorefrontUiState(
                        places = lugares,
                        selectedPlace = lugares.first(),
                        content = conteudo,
                    ),
                    onAction = {},
                )
            }
        }
    }

    /**
     * Captura o estado de carregamento inicial da tela Storefront.
     * Valida que o indicador de progresso e os chips de bairro são exibidos.
     */
    @Test
    fun estadoDeCarregamento() {
        exibir(StorefrontContent.Loading)
        composeRule.onRoot().captureRoboImage("src/test/snapshots/storefront_loading.png")
    }

    /**
     * Captura o estado carregado com lojas parceiras disponíveis.
     * Valida que os cards de loja, status (aberta/fechada), taxa de entrega
     * e tempo estimado são renderizados corretamente.
     */
    @Test
    fun estadoCarregadoComLojas() {
        exibir(
            StorefrontContent.Loaded(
                partners = persistentListOf(
                    StoreCardUi(
                        id = "1",
                        name = "Petshop Lavrado",
                        neighborhood = "Centro",
                        isOpen = true,
                        estimatedMinutes = 40,
                        deliveryFee = "R\$ 5,00",
                        isFreeDelivery = false,
                        minOrder = "R\$ 30,00",
                        distance = "440 m",
                    ),
                    StoreCardUi(
                        id = "2",
                        name = "Bicho Feliz",
                        neighborhood = "Santa Tereza",
                        isOpen = false,
                        estimatedMinutes = 35,
                        deliveryFee = "R\$ 0,00",
                        isFreeDelivery = true,
                        minOrder = null,
                        distance = "3,6 km",
                    ),
                ),
            ),
        )
        composeRule.onRoot().captureRoboImage("src/test/snapshots/storefront_loaded.png")
    }
}
