package io.github.gabrielevanger.xodopet.core.domain.testing

import io.github.gabrielevanger.xodopet.core.domain.delivery.DeliveryPlaceRepository
import io.github.gabrielevanger.xodopet.core.domain.model.DeliveryPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeDeliveryPlaceRepository(initial: DeliveryPlace? = null) : DeliveryPlaceRepository {
    private val state = MutableStateFlow(initial)

    override val place: StateFlow<DeliveryPlace?> = state

    override suspend fun set(place: DeliveryPlace) {
        state.value = place
    }
}
