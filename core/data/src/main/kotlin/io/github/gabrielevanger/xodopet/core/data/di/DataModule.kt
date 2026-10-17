package io.github.gabrielevanger.xodopet.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gabrielevanger.xodopet.core.data.SupabaseConfig
import io.github.gabrielevanger.xodopet.core.data.remote.SupabaseStorefrontRepository
import io.github.gabrielevanger.xodopet.core.data.remote.createXodoSupabaseClient
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

    companion object {
        @Provides
        @Singleton
        fun supabaseClient(config: SupabaseConfig): SupabaseClient = createXodoSupabaseClient(config, OkHttp.create())

        // O domínio é Kotlin puro, sem anotações de injeção: as regras são criadas aqui.
        @Provides
        fun loadStorefront(repository: StorefrontRepository): LoadStorefront = LoadStorefront(repository)
    }
}
