package io.github.gabrielevanger.xodopet.core.ui

import io.github.gabrielevanger.xodopet.core.domain.model.Money
import kotlin.math.abs

/**
 * R$ 1.234,50. Feito à mão em vez de NumberFormat para o resultado não depender do aparelho
 * (algumas versões do Android usam espaço não separável depois do "R$", outras não).
 */
fun Money.formatBrl(): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = abs(cents)
    val reais = (absolute / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    val centavos = (absolute % 100).toString().padStart(2, '0')
    return "${sign}R$ $reais,$centavos"
}

/** 450 m, 1,6 km, 12 km. */
fun formatDistance(meters: Int): String = when {
    meters < 1000 -> "${(meters / 10) * 10} m"

    meters < 10_000 -> {
        val tenths = (meters + 50) / 100
        "${tenths / 10},${tenths % 10} km"
    }

    else -> "${(meters + 500) / 1000} km"
}
