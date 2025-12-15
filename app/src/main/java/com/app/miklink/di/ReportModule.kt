/*
 * Purpose: Bind report-related infrastructure (codec) and use cases for dependency injection.
 * Inputs: Hilt graph injection requests for report components.
 * Outputs: Concrete implementations for codecs and report use cases.
 * Notes: Keep report-specific bindings separate from repository bindings for clarity.
 */
package com.app.miklink.di

import com.app.miklink.core.data.report.ReportResultsCodec
import com.app.miklink.core.domain.usecase.report.ParseReportResultsUseCase
import com.app.miklink.core.domain.usecase.report.ParseReportResultsUseCaseImpl
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCase
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCaseImpl
import com.app.miklink.data.report.codec.MoshiReportResultsCodec
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportModule {

    @Binds
    @Singleton
    abstract fun bindReportResultsCodec(impl: MoshiReportResultsCodec): ReportResultsCodec

    @Binds
    abstract fun bindParseReportResultsUseCase(impl: ParseReportResultsUseCaseImpl): ParseReportResultsUseCase

    @Binds
    abstract fun bindSaveTestReportUseCase(impl: SaveTestReportUseCaseImpl): SaveTestReportUseCase
}
