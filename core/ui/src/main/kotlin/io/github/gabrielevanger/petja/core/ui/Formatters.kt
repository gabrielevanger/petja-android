package io.github.gabrielevanger.petja.core.ui

import io.github.gabrielevanger.petja.core.domain.model.Money
import kotlin.math.abs

private const val CURRENCY_PREFIX = "R$ "
private const val CENTS_PER_REAL = 100
private const val CENT_DIGITS = 2
private const val THOUSANDS_GROUP = 3
private const val THOUSANDS_SEPARATOR = "."
private const val DECIMAL_SEPARATOR = ","

private const val METERS_PER_KM = 1000
private const val METERS_PER_TENTH_KM = 100
private const val TENTHS_PER_KM = 10

/** Abaixo disto, a distância aparece em metros, arredondada para dezenas. */
private const val METERS_DISPLAY_LIMIT = 1000

/** Abaixo disto, a distância aparece em km com uma casa decimal; acima, em km inteiros. */
private const val DECIMAL_KM_DISPLAY_LIMIT = 10_000

private const val METERS_ROUNDING_STEP = 10

/**
 * R$ 1.234,50. Feito à mão em vez de NumberFormat para o resultado não depender do aparelho
 * (algumas versões do Android usam espaço não separável depois do "R$", outras não).
 * Símbolo e separadores ficam aqui, e não em strings.xml, porque são do real, não do idioma.
 */
fun Money.formatBrl(): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = abs(cents)
    val reais = (absolute / CENTS_PER_REAL).toString()
        .reversed()
        .chunked(THOUSANDS_GROUP)
        .joinToString(THOUSANDS_SEPARATOR)
        .reversed()
    val centavos = (absolute % CENTS_PER_REAL).toString().padStart(CENT_DIGITS, '0')
    return "$sign$CURRENCY_PREFIX$reais$DECIMAL_SEPARATOR$centavos"
}

/** 450 m, 1,6 km, 12 km. As unidades são do Sistema Internacional e não mudam com o idioma. */
fun formatDistance(meters: Int): String = when {
    meters < METERS_DISPLAY_LIMIT -> "${(meters / METERS_ROUNDING_STEP) * METERS_ROUNDING_STEP} m"

    meters < DECIMAL_KM_DISPLAY_LIMIT -> {
        val tenths = (meters + METERS_PER_TENTH_KM / 2) / METERS_PER_TENTH_KM
        "${tenths / TENTHS_PER_KM}$DECIMAL_SEPARATOR${tenths % TENTHS_PER_KM} km"
    }

    else -> "${(meters + METERS_PER_KM / 2) / METERS_PER_KM} km"
}

/** Casas de centavos aceitas ao digitar um valor ("100,5" vale R$ 100,50). */
private val TYPED_MONEY = Regex("""^(\d{1,7})(?:[.,](\d{1,2}))?$""")

/**
 * Valor digitado pelo cliente, como o troco: "100", "100,50", "100.5" ou "R$ 100".
 * null quando não dá para entender; separador de milhar não é aceito, para "1.000" não virar R$ 1,00.
 */
fun parseBrl(text: String): Money? {
    val clean = text.trim().removePrefix("R$").trim()
    val match = TYPED_MONEY.matchEntire(clean) ?: return null
    val reais = match.groupValues[1].toLong()
    val centavos = match.groupValues[2].padEnd(CENT_DIGITS, '0').toLong()
    return Money(reais * CENTS_PER_REAL + centavos)
}
