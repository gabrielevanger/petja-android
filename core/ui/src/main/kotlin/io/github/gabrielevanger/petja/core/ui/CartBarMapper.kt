package io.github.gabrielevanger.petja.core.ui

import io.github.gabrielevanger.petja.core.domain.model.Cart

fun Cart?.toCartBar(): CartBarUi? = this?.let { CartBarUi(it.storeName, it.itemCount, it.subtotal.formatBrl()) }
