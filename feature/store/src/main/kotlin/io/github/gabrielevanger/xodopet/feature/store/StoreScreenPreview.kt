package io.github.gabrielevanger.xodopet.feature.store

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import kotlinx.collections.immutable.persistentListOf

// Dados de exemplo só para o preview do Android Studio; não vão para o app.
@Preview(showBackground = true)
@Composable
private fun StorePreview() {
    XodoTheme {
        StoreScreen(
            state = StoreUiState.Loaded(
                header = StoreHeaderUi(
                    "Petshop Lavrado",
                    "Centro",
                    "Ração, petiscos e acessórios.",
                    false,
                    40,
                    "R$ 30,00",
                ),
                sections = persistentListOf(
                    CatalogSectionUi(
                        1,
                        "Ração",
                        persistentListOf(
                            ProductRowUi("1", "Ração Golden Adultos", "Golden", "R$ 30,90", true, true),
                            ProductRowUi("2", "Ração Pedigree Adultos", "Pedigree", null, false, false),
                        ),
                    ),
                ),
            ),
            onBack = {},
            onRetry = {},
            onProductClick = {},
            onProductDismiss = {},
        )
    }
}
