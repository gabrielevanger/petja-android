package io.github.gabrielevanger.xodopet.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de espaçamento em passos de 4 dp. Telas e componentes não usam valores fora dela.
 *
 * Fica em Kotlin e não em res/values/dimens.xml: no Compose os tokens são tipados, entram no tema
 * e trocam junto com ele, sem `dimensionResource` espalhado pelas telas.
 */
@Immutable
data class XodoSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

/** Tamanhos fixos que não são espaçamento. */
@Immutable
data class XodoSizes(
    /** Alvo de toque mínimo recomendado pelo Material e pelas diretrizes de acessibilidade (RNF-07). */
    val minTouchTarget: Dp = 48.dp,
    val divider: Dp = 1.dp,
    /** Indicador de carregamento dentro de um botão, do tamanho do texto. */
    val inlineProgress: Dp = 18.dp,
    /** Ícones de traço do layout (Lucide), um pouco menores que os 24 dp do Material. */
    val icon: Dp = 22.dp,
    /** Altura dos botões de ação, acima do alvo de toque mínimo. */
    val button: Dp = 52.dp,
    /** Largura de cada destino da barra inferior. */
    val bottomBarItem: Dp = 72.dp,
)

@Immutable
data class XodoShapes(
    val badge: Shape = RoundedCornerShape(8.dp),
    /** Cartões de lista, opções, botões e campos. */
    val card: Shape = RoundedCornerShape(16.dp),
    val notice: Shape = RoundedCornerShape(12.dp),
    /** Blocos grandes: resumo, perfil, cabeçalho do status do pedido. */
    val panel: Shape = RoundedCornerShape(24.dp),
)
