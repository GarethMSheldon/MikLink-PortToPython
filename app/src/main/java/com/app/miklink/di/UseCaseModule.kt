/*
 * Purpose: Bind cross-feature use cases to their implementations for Hilt injection.
 * Inputs: Hilt requests for client/profile save use cases across UI layers.
 * Outputs: Concrete implementations wired into the SingletonComponent graph.
 * Notes: Keeps business logic bindings separate from repository providers to follow modular DI guidance.
 */
package com.app.miklink.di

import com.app.miklink.core.domain.usecase.client.SaveClientUseCase
import com.app.miklink.core.domain.usecase.client.SaveClientUseCaseImpl
import com.app.miklink.core.domain.usecase.testprofile.SaveTestProfileUseCase
import com.app.miklink.core.domain.usecase.testprofile.SaveTestProfileUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {
    @Binds
    abstract fun bindSaveClientUseCase(impl: SaveClientUseCaseImpl): SaveClientUseCase

    @Binds
    abstract fun bindSaveTestProfileUseCase(impl: SaveTestProfileUseCaseImpl): SaveTestProfileUseCase
}
