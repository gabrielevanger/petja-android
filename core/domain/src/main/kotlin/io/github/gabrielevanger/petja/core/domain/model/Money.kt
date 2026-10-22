package io.github.gabrielevanger.petja.core.domain.model

/** Valor em centavos, como no banco: R$ 12,90 = Money(1290). A formatação fica na interface. */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    companion object {
        val ZERO = Money(0)
    }
}
