package io.github.gabrielevanger.petja.feature.storefront

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// Dados de exemplo só para o preview do Android Studio; não vão para o app.
private val PreviewPlaces = BoaVistaSamplePlaces.toImmutableList()

@Preview(showBackground = true)
@Composable
private fun StorefrontPreview() {
    PetjaTheme {
        StorefrontScreen(
            state = StorefrontUiState(
                places = PreviewPlaces,
                selectedPlace = PreviewPlaces.first(),
                content = StorefrontContent.Loaded(
                    partners = persistentListOf(
                        StoreCardUi("1", "Petshop Lavrado", "Centro", true, 40, "R$ 5,00", false, "R$ 30,00", "440 m"),
                        StoreCardUi("2", "Bicho Feliz", "Santa Tereza", false, 40, "R$ 6,50", false, null, "3,6 km"),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoPartnerPreview() {
    PetjaTheme {
        StorefrontScreen(
            state = StorefrontUiState(
                places = PreviewPlaces,
                selectedPlace = PreviewPlaces.last(),
                content = StorefrontContent.Loaded(
                    nonPartners = persistentListOf(
                        ExternalStoreCardUi(
                            "e1",
                            "Agropet Satélite",
                            "Rua Fictícia A, 10",
                            "Cidade Satélite",
                            "310 m",
                            "https://wa.me/5595900001001",
                            "tel:9530001001",
                        ),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
