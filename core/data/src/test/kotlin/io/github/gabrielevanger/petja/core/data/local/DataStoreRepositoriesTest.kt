package io.github.gabrielevanger.petja.core.data.local

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.gabrielevanger.petja.core.domain.cart.CartActions
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.testing.CENTRO_PLACE
import io.github.gabrielevanger.petja.core.domain.testing.aProduct
import io.github.gabrielevanger.petja.core.domain.testing.aStoreDetails
import io.github.gabrielevanger.petja.core.domain.testing.aVariant
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * O DataStore só permite uma instância ativa por arquivo. Para simular o app reaberto, a primeira
 * instância roda num escopo próprio, cancelado antes de abrir a segunda.
 */
class DataStoreRepositoriesTest {
    @TempDir
    lateinit var dir: File

    private val cartFile get() = File(dir, "cart.json")
    private val placeFile get() = File(dir, "place.json")

    private fun TestScope.childScope() = CoroutineScope(backgroundScope.coroutineContext + Job())

    private fun cartRepository(scope: CoroutineScope) = DataStoreCartRepository(
        DataStoreFactory.create(
            serializer = JsonFileSerializer(StoredCartFile.serializer(), StoredCartFile()),
            corruptionHandler = ReplaceFileCorruptionHandler { StoredCartFile() },
            scope = scope,
            produceFile = { cartFile },
        ),
    )

    private fun placeRepository(scope: CoroutineScope) = DataStoreDeliveryPlaceRepository(
        DataStoreFactory.create(
            serializer = JsonFileSerializer(StoredDeliveryPlaceFile.serializer(), StoredDeliveryPlaceFile()),
            scope = scope,
            produceFile = { placeFile },
        ),
    )

    @Test
    fun `cart survives the app being reopened`() = runTest {
        val firstRun = childScope()
        CartActions(cartRepository(firstRun)).add(aStoreDetails(), aProduct(), aVariant(priceCents = 3090, stock = 5))
        firstRun.coroutineContext[Job]!!.cancelAndJoin()

        val reopened = cartRepository(childScope()).cart.first()

        assertEquals(1, reopened?.itemCount)
        assertEquals(Money(3090), reopened?.subtotal)
    }

    @Test
    fun `corrupted file becomes an empty cart`() = runTest {
        cartFile.writeText("{isto não é json")

        assertNull(cartRepository(childScope()).cart.first())
    }

    @Test
    fun `delivery place survives the app being reopened`() = runTest {
        val firstRun = childScope()
        val first = placeRepository(firstRun)
        assertNull(first.place.first())
        first.set(CENTRO_PLACE)
        firstRun.coroutineContext[Job]!!.cancelAndJoin()

        assertEquals(CENTRO_PLACE, placeRepository(childScope()).place.first())
    }
}
