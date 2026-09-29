package com.triviamap.domain.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    @Test fun nothingToShowWithoutUpdate() = assertFalse(UpdatePolicy.shouldShow(UpdateState.None, 0))

    @Test fun availableUpdateShownUntilDismissedForThatVersion() {
        assertTrue(UpdatePolicy.shouldShow(UpdateState.Available(5), 0))
        assertFalse(UpdatePolicy.shouldShow(UpdateState.Available(5), 5))
    }

    @Test fun dismissalDoesNotHideNewerVersion() = assertTrue(UpdatePolicy.shouldShow(UpdateState.Available(6), 5))

    @Test fun downloadAndRestartAlwaysShown() {
        assertTrue(UpdatePolicy.shouldShow(UpdateState.Downloading, 5))
        assertTrue(UpdatePolicy.shouldShow(UpdateState.ReadyToInstall, 5))
    }
}
