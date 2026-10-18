package io.github.gabrielevanger.xodopet.core.data.local

import androidx.datastore.core.DataStore
import io.github.gabrielevanger.xodopet.core.domain.delivery.DeliveryPlaceRepository
import io.github.gabrielevanger.xodopet.core.domain.model.DeliveryPlace
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DataStoreDeliveryPlaceRepository @Inject constructor(
    private val store: DataStore<StoredDeliveryPlaceFile>,
) : DeliveryPlaceRepository {

    override val place: Flow<DeliveryPlace?> = store.data.map { it.place?.toDomain() }

    override suspend fun set(place: DeliveryPlace) {
        store.updateData { StoredDeliveryPlaceFile(place.toStored()) }
    }
}
