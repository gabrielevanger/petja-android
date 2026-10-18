package io.github.gabrielevanger.xodopet.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Estilos por papel na tela, não por tamanho: a tela pede "título de seção" ou "preço" e o tema
 * decide a medida. Os valores seguem a escala do Material 3 para combinar com os componentes dele.
 */
@Immutable
data class XodoTypography(
    /** Título da tela e do detalhe de produto. */
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    /** Nome da loja no cartão da vitrine. */
    val cardTitle: TextStyle,
    /** Nome do produto na lista. */
    val itemName: TextStyle,
    val body: TextStyle,
    /** Detalhes secundários: bairro, marca, distância, avisos curtos. */
    val caption: TextStyle,
    /** Rótulo acima de um grupo de controles, como "Entregar em". */
    val label: TextStyle,
    val badge: TextStyle,
    /** Dígitos tabulares: os preços de uma lista ficam alinhados. */
    val price: TextStyle,
    /** Mensagem central de estado vazio ou de erro. */
    val message: TextStyle,
)

internal val DefaultXodoTypography = XodoTypography(
    screenTitle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal),
    sectionTitle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    cardTitle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    itemName = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    badge = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    price = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TABULAR_NUMBERS,
    ),
    message = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
)

/** Recurso OpenType de dígitos com largura fixa. */
private const val TABULAR_NUMBERS = "tnum"
