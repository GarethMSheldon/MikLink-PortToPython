/*
 * Purpose: Run LLDP/CDP neighbor discovery and surface domain neighbors.
 * Inputs: Test execution context (probe configuration with interface).
 * Outputs: StepResult carrying a list of NeighborData items or failure reason.
 * Notes: Repository returns domain neighbors; this step keeps control-flow and error mapping only.
 */
package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import javax.inject.Inject

/**
 * Implementazione di NeighborDiscoveryStep.
 * Usa MikroTikTestRepository per eseguire discovery LLDP/CDP.
 */
class NeighborDiscoveryStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository
) : NeighborDiscoveryStep {
    override suspend fun run(context: TestExecutionContext): StepResult<List<NeighborData>> {
        return try {
            val neighbors = mikrotikTestRepository.neighbors(
                probe = context.probeConfig,
                interfaceName = context.probeConfig.testInterface
            )
            StepResult.Success(neighbors)
        } catch (e: Exception) {
            StepResult.Failed(TestError.NetworkError(e.message ?: "Neighbor discovery failed"))
        }
    }
}
