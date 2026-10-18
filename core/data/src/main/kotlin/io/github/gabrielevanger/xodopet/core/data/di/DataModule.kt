package io.github.gabrielevanger.xodopet.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gabrielevanger.xodopet.core.data.LogcatDiagnosticLog
import io.github.gabrielevanger.xodopet.core.data.SupabaseConfig
import io.github.gabrielevanger.xodopet.core.data.remote.SupabaseAccountRepository
import io.github.gabrielevanger.xodopet.core.data.remote.SupabaseAuthLinkHandler
import io.github.gabrielevanger.xodopet.core.data.remote.SupabaseCheckoutRepository
import io.github.gabrielevanger.xodopet.core.data.remote.SupabaseOrderRepository
import io.github.gabrielevanger.xodopet.core.data.remote.SupabaseStoreRepository
import io.github.gabrielevanger.xodopet.core.data.remote.SupabaseStorefrontRepository
import io.github.gabrielevanger.xodopet.core.data.remote.createXodoSupabaseClient
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.account.AccountRepository
import io.github.gabrielevanger.xodopet.core.domain.account.AuthLinkHandler
import io.github.gabrielevanger.xodopet.core.domain.cart.CartActions
import io.github.gabrielevanger.xodopet.core.domain.cart.CartRepository
import io.github.gabrielevanger.xodopet.core.domain.checkout.CheckoutRepository
import io.github.gabrielevanger.xodopet.core.domain.checkout.PlaceOrder
import io.github.gabrielevanger.xodopet.core.domain.delivery.QuoteDelivery
import io.github.gabrielevanger.xodopet.core.domain.orders.OrderRepository
import io.github.gabrielevanger.xodopet.core.domain.orders.ReorderToCart
import io.github.gabrielevanger.xodopet.core.domain.store.LoadStoreCatalog
import io.github.gabrielevanger.xodopet.core.domain.store.StoreRepository
import io.github.gabrielevanger.xodopet.core.domain.storefront.LoadStorefront
import io.github.gabrielevanger.xodopet.core.domain.storefront.StorefrontRepository
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

/** O SupabaseConfig vem do módulo app, que lê o local.properties. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun storefrontRepository(impl: SupabaseStorefrontRepository): StorefrontRepository

    @Binds
    abstract fun storeRepository(impl: SupabaseStoreRepository): StoreRepository

    @Binds
    abstract fun accountRepository(impl: SupabaseAccountRepository): AccountRepository

    @Binds
    abstract fun authLinkHandler(impl: SupabaseAuthLinkHandler): AuthLinkHandler

    @Binds
    abstract fun checkoutRepository(impl: SupabaseCheckoutRepository): CheckoutRepository

    @Binds
    abstract fun orderRepository(impl: SupabaseOrderRepository): OrderRepository

    @Binds
    abstract fun diagnosticLog(impl: LogcatDiagnosticLog): DiagnosticLog

    companion object {
        @Provides
        @Singleton
        fun supabaseClient(config: SupabaseConfig): SupabaseClient = createXodoSupabaseClient(config, OkHttp.create())

        // O domínio é Kotlin puro, sem anotações de injeção: as regras são criadas aqui.
        @Provides
        fun loadStorefront(repository: StorefrontRepository): LoadStorefront = LoadStorefront(repository)

        @Provides
        fun loadStoreCatalog(repository: StoreRepository): LoadStoreCatalog = LoadStoreCatalog(repository)

        @Provides
        fun placeOrder(repository: CheckoutRepository, cartActions: CartActions): PlaceOrder =
            PlaceOrder(repository, cartActions)

        @Provides
        fun reorderToCart(stores: StoreRepository, carts: CartRepository): ReorderToCart = ReorderToCart(stores, carts)

        @Provides
        fun quoteDelivery(repository: StorefrontRepository): QuoteDelivery = QuoteDelivery(repository)
    }
}
