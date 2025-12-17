/*
 * Purpose: Persist test profiles without triggering UNIQUE constraint errors by routing edits to update.
 * Inputs: TestProfile instances with profileId set (0 for new profiles, existing id for edits).
 * Outputs: Identifier of the saved profile, delegating insert/update to the repository as appropriate.
 * Notes: Centralizes the upsert policy so UI layers do not call insert on existing primary keys.
 */
package com.app.miklink.core.domain.usecase.testprofile

import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.model.TestProfile
import javax.inject.Inject

interface SaveTestProfileUseCase {
    suspend operator fun invoke(profile: TestProfile): Long
}

class SaveTestProfileUseCaseImpl @Inject constructor(
    private val testProfileRepository: TestProfileRepository
) : SaveTestProfileUseCase {
    override suspend fun invoke(profile: TestProfile): Long {
        return if (profile.profileId == 0L) {
            testProfileRepository.insertProfile(profile)
        } else {
            testProfileRepository.updateProfile(profile)
            profile.profileId
        }
    }
}
