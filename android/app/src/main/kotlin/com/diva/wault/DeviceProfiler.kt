// android/app/src/main/kotlin/com/diva/wault/DeviceProfiler.kt

package com.diva.wault

import android.app.ActivityManager
import android.content.Context

object DeviceProfiler {

    enum class Tier {
        POTATO,
        CAPABLE,
        POWERFUL,
        BEAST
    }

    data class DeviceInfo(
        val totalRamMB: Int,
        val availableRamMB: Int,
        val cpuCores: Int,
        val tier: Tier,
        val maxAccounts: Int,
        val maxWarm: Int
    )

    fun profile(context: Context): DeviceInfo {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalRamMB = (memInfo.totalMem / (1024 * 1024)).toInt()
        val availableRamMB = (memInfo.availMem / (1024 * 1024)).toInt()
        val cpuCores = Runtime.getRuntime().availableProcessors()

        val tier = when {
            totalRamMB < 5120 -> Tier.POTATO
            totalRamMB < 7168 -> Tier.CAPABLE
            totalRamMB < 10240 -> Tier.POWERFUL
            else -> Tier.BEAST
        }

        val maxAccounts = if (tier == Tier.POTATO) 4 else 5
        val maxWarm = when (tier) {
            Tier.POTATO -> 1
            Tier.CAPABLE -> 2
            Tier.POWERFUL -> 3
            Tier.BEAST -> 4
        }

        return DeviceInfo(
            totalRamMB = totalRamMB,
            availableRamMB = availableRamMB,
            cpuCores = cpuCores,
            tier = tier,
            maxAccounts = maxAccounts,
            maxWarm = maxWarm
        )
    }
}