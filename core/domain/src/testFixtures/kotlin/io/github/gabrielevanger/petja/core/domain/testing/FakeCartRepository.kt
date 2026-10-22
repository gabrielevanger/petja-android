package io.github.gabrielevanger.petja.core.domain.testing

import io.github.gabrielevanger.petja.core.domain.cart.CartRepository
import io.github.gabrielevanger.petja.core.domain.model.Cart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeCartRepository(initial: Cart? = null) : CartRepository {
    private val state = MutableStateFlow(initial)

    override val cart: StateFlow<Cart?> = state

    override suspend fun update(transform: (Cart?) -> Cart?) = state.update(transform)
}
