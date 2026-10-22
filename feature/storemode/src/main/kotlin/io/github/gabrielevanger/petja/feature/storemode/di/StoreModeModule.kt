package io.github.gabrielevanger.petja.feature.storemode.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gabrielevanger.petja.feature.storemode.NewOrderAlert
import io.github.gabrielevanger.petja.feature.storemode.RingtoneNewOrderAlert

@Module
@InstallIn(SingletonComponent::class)
internal abstract class StoreModeModule {
    @Binds
    abstract fun newOrderAlert(impl: RingtoneNewOrderAlert): NewOrderAlert
}
