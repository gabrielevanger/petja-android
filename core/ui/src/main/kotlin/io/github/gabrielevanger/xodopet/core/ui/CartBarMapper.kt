package io.github.gabrielevanger.xodopet.core.ui

import io.github.gabrielevanger.xodopet.core.domain.model.Cart

fun Cart?.toCartBar(): CartBarUi? = this?.let { CartBarUi(it.storeName, it.itemCount, it.subtotal.formatBrl()) }
