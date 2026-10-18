package io.github.gabrielevanger.xodopet.core.domain.delivery

import io.github.gabrielevanger.xodopet.core.domain.model.DeliveryPlace
import kotlinx.coroutines.flow.Flow

/** Lugar de entrega escolhido, salvo no aparelho: a vitrine e o carrinho usam o mesmo. */
interface DeliveryPlaceRepository {
    /** null enquanto o cliente não escolheu nenhum. */
    val place: Flow<DeliveryPlace?>

    suspend fun set(place: DeliveryPlace)
}
