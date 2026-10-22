package io.github.gabrielevanger.petja.core.data.local

import androidx.datastore.core.DataStore
import io.github.gabrielevanger.petja.core.domain.cart.CartRepository
import io.github.gabrielevanger.petja.core.domain.model.Cart
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DataStoreCartRepository @Inject constructor(private val store: DataStore<StoredCartFile>) :
    CartRepository {

    override val cart: Flow<Cart?> = store.data.map { it.cart?.toDomain() }

    // O DataStore aplica uma atualização por vez, então dois toques rápidos em "Adicionar" somam os dois.
    override suspend fun update(transform: (Cart?) -> Cart?) {
        store.updateData { file -> StoredCartFile(transform(file.cart?.toDomain())?.toStored()) }
    }
}
