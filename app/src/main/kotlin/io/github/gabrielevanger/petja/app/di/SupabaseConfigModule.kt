package io.github.gabrielevanger.petja.app.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gabrielevanger.petja.app.BuildConfig
import io.github.gabrielevanger.petja.core.data.SupabaseConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseConfigModule {

    @Provides
    @Singleton
    fun supabaseConfig(): SupabaseConfig {
        val config = SupabaseConfig(url = BuildConfig.SUPABASE_URL, anonKey = BuildConfig.SUPABASE_ANON_KEY)
        if (!config.isConfigured) {
            Log.w("PetJa", "Supabase não configurado: defina supabase.url e supabase.anonKey no local.properties.")
        }
        return config
    }
}
