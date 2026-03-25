// File: android/app/src/main/kotlin/com/diva/wault/MemoryDirector.kt
package com.diva.wault

object MemoryDirector {

    data class MemoryPolicy(
        val maxAccounts: Int,
        val maxWarm: Int,
        val monitorIntervalSeconds: Int,
        val freezeToColdOnLowMemory: Boolean
    )

    fun policyForTier(tier: DeviceProfiler.Tier): MemoryPolicy {
        return when (tier) {
            DeviceProfiler.Tier.POTATO -> MemoryPolicy(
                maxAccounts = 4,
                maxWarm = 1,
                monitorIntervalSeconds = 2,
                freezeToColdOnLowMemory = true
            )

            DeviceProfiler.Tier.CAPABLE -> MemoryPolicy(
                maxAccounts = 5,
                maxWarm = 2,
                monitorIntervalSeconds = 3,
                freezeToColdOnLowMemory = true
            )

            DeviceProfiler.Tier.POWERFUL -> MemoryPolicy(
                maxAccounts = 5,
                maxWarm = 3,
                monitorIntervalSeconds = 4,
                freezeToColdOnLowMemory = true
            )

            DeviceProfiler.Tier.BEAST -> MemoryPolicy(
                maxAccounts = 5,
                maxWarm = 4,
                monitorIntervalSeconds = 5,
                freezeToColdOnLowMemory = true
            )
        }
    }
}