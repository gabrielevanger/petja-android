package io.github.gabrielevanger.xodopet.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gabrielevanger.xodopet.core.data.local.DataStoreCartRepository
import io.github.gabrielevanger.xodopet.core.data.local.DataStoreDeliveryPlaceRepository
import io.github.gabrielevanger.xodopet.core.data.local.JsonFileSerializer
import io.github.gabrielevanger.xodopet.core.data.local.StoredCartFile
import io.github.gabrielevanger.xodopet.core.data.local.StoredDeliveryPlaceFile
import io.github.gabrielevanger.xodopet.core.domain.cart.CartActions
import io.github.gabrielevanger.xodopet.core.domain.cart.CartRepository
import io.github.gabrielevanger.xodopet.core.domain.delivery.DeliveryPlaceRepository
import javax.inject.Singleton

private const val CART_FILE = "cart.json"
private const val DELIVERY_PLACE_FILE = "delivery_place.json"

/** Dados salvos no aparelho. Arquivo corrompido volta ao vazio em vez de travar o app. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalDataModule {

    @Binds
    abstract fun cartRepository(impl: DataStoreCartRepository): CartRepository

    @Binds
    abstract fun deliveryPlaceRepository(impl: DataStoreDeliveryPlaceRepository): DeliveryPlaceRepository

    companion object {
        @Provides
        @Singleton
        fun cartStore(@ApplicationContext context: Context): DataStore<StoredCartFile> = DataStoreFactory.create(
            serializer = JsonFileSerializer(StoredCartFile.serializer(), StoredCartFile()),
            corruptionHandler = ReplaceFileCorruptionHandler { StoredCartFile() },
            produceFile = { context.dataStoreFile(CART_FILE) },
        )

        @Provides
        @Singleton
        fun deliveryPlaceStore(@ApplicationContext context: Context): DataStore<StoredDeliveryPlaceFile> =
            DataStoreFactory.create(
                serializer = JsonFileSerializer(StoredDeliveryPlaceFile.serializer(), StoredDeliveryPlaceFile()),
                corruptionHandler = ReplaceFileCorruptionHandler { StoredDeliveryPlaceFile() },
                produceFile = { context.dataStoreFile(DELIVERY_PLACE_FILE) },
            )

        @Provides
        fun cartActions(repository: CartRepository): CartActions = CartActions(repository)
    }
}
