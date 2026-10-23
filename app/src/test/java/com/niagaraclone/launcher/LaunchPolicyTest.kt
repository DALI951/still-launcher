package com.niagaraclone.launcher

import com.niagaraclone.launcher.data.LaunchPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchPolicyTest {

    @Test
    fun testMissingPermissionOrUnknownUsageFailsOpen() {
        // When permission is not granted or usage stats are unavailable, usedMinutes is negative.
        // It should fail open (return false) so user is never locked out due to missing permission.
        assertFalse("usedMinutes=-1 (permission missing) must not lock out", LaunchPolicy.isOverDailyLimit(-1, 30))
        assertFalse("usedMinutes=-100 must not lock out", LaunchPolicy.isOverDailyLimit(-100, 30))
        assertFalse("usedMinutes=-1 with small limit must not lock out", LaunchPolicy.isOverDailyLimit(-1, 5))
    }

    @Test
    fun testUnderDailyLimitDoesNotLockOut() {
        assertFalse("0 mins used out of 30 should not lock out", LaunchPolicy.isOverDailyLimit(0, 30))
        assertFalse("10 mins used out of 30 should not lock out", LaunchPolicy.isOverDailyLimit(10, 30))
        assertFalse("29 mins used out of 30 should not lock out", LaunchPolicy.isOverDailyLimit(29, 30))
        assertFalse("4 mins used out of 5 should not lock out", LaunchPolicy.isOverDailyLimit(4, 5))
        assertFalse("59 mins used out of 60 should not lock out", LaunchPolicy.isOverDailyLimit(59, 60))
    }

    @Test
    fun testExactlyAtDailyLimitLocksOut() {
        assertTrue("30 mins used out of 30 must lock out", LaunchPolicy.isOverDailyLimit(30, 30))
        assertTrue("5 mins used out of 5 must lock out", LaunchPolicy.isOverDailyLimit(5, 5))
        assertTrue("15 mins used out of 15 must lock out", LaunchPolicy.isOverDailyLimit(15, 15))
        assertTrue("45 mins used out of 45 must lock out", LaunchPolicy.isOverDailyLimit(45, 45))
        assertTrue("60 mins used out of 60 must lock out", LaunchPolicy.isOverDailyLimit(60, 60))
    }

    @Test
    fun testOverDailyLimitLocksOut() {
        assertTrue("31 mins used out of 30 must lock out", LaunchPolicy.isOverDailyLimit(31, 30))
        assertTrue("45 mins used out of 30 must lock out", LaunchPolicy.isOverDailyLimit(45, 30))
        assertTrue("120 mins used out of 60 must lock out", LaunchPolicy.isOverDailyLimit(120, 60))
    }

    @Test
    fun testZeroOrNegativeLimitMinutesDoesNotLockOut() {
        assertFalse("Zero limit should not lock out", LaunchPolicy.isOverDailyLimit(10, 0))
        assertFalse("Negative limit should not lock out", LaunchPolicy.isOverDailyLimit(10, -15))
    }
}
