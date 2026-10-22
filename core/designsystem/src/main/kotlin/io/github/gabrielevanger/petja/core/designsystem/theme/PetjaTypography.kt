package io.github.gabrielevanger.petja.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.gabrielevanger.petja.core.designsystem.R

/**
 * Estilos por papel na tela, não por tamanho: a tela pede "título de seção" ou "preço" e o tema
 * decide a medida. O texto corrido segue a escala do Material 3 (Roboto); títulos de destaque usam
 * a Manrope, como nos estilos "PetJá/…" do Figma.
 */
@Immutable
data class PetjaTypography(
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
    /** Título na barra do topo das telas abertas a partir de outra. */
    val topBarTitle: TextStyle,
    /** Frase grande de destaque: "Pedido #1 enviado!", "Olá, Ana". */
    val headline: TextStyle,
    /** Nome da loja no topo da página da loja. */
    val storeTitle: TextStyle,
    /** Nome do produto na página do produto. */
    val productTitle: TextStyle,
    /** Preço em destaque na página do produto. */
    val productPrice: TextStyle,
    /** Marca "petjá" no topo da vitrine. */
    val logo: TextStyle,
    /** Frase de apoio abaixo de um destaque. */
    val tagline: TextStyle,
    /** Rótulo dos destinos da barra inferior; o escolhido usa navLabelSelected. */
    val navLabel: TextStyle,
    val navLabelSelected: TextStyle,
)

/** Um arquivo só (fonte variável): cada peso aponta para o mesmo arquivo com o eixo "wght" ajustado. */
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

internal val Manrope = FontFamily(
    manrope(FontWeight.Medium),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

internal val DefaultPetjaTypography = PetjaTypography(
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
    topBarTitle = TextStyle(fontFamily = Manrope, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    headline = TextStyle(
        fontFamily = Manrope,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
    ),
    storeTitle = TextStyle(
        fontFamily = Manrope,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.8).sp,
    ),
    productTitle = TextStyle(fontFamily = Manrope, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    productPrice = TextStyle(
        fontFamily = Manrope,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFeatureSettings = TABULAR_NUMBERS,
    ),
    logo = TextStyle(fontFamily = Manrope, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold),
    tagline = TextStyle(fontFamily = Manrope, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    navLabel = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    navLabelSelected = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
)

/** Recurso OpenType de dígitos com largura fixa. */
private const val TABULAR_NUMBERS = "tnum"
