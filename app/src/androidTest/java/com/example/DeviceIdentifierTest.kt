package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pos.security.DeviceInfoHelper
import com.example.pos.security.DeviceIdentifierManager
import com.example.pos.security.DrmDeviceIdentifier
import com.example.pos.security.PosDeviceIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceIdentifierTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testHardwareDeviceId_generatesAndPersistsKeyStoreKey() {
        val id1 = DeviceIdentifierManager.getHardwareDeviceId(context)
        assertNotNull(id1)
        assertTrue("Device ID should not be blank", id1.isNotBlank())

        // Re-querying must retrieve the same persistent hardware key
        val id2 = DeviceIdentifierManager.getHardwareDeviceId(context)
        assertEquals("Subsequent calls must return identical hardware-backed device ID", id1, id2)
    }

    @Test
    fun testHardwareDeviceSignature_signsPayloadWithECDSA() {
        val payload = "TEST_TRANSACTION_PAYLOAD_1001".toByteArray(Charsets.UTF_8)
        val signature = DeviceIdentifierManager.signPayload(context, payload)

        assertNotNull(signature)
        assertTrue("Signature should not be blank", signature.isNotBlank())
    }

    @Test
    fun testDeviceInfoHelper_returnsValidAndroidIdAndLabel() {
        val androidId = DeviceInfoHelper.getDisplayDeviceId(context)
        assertNotNull(androidId)
        assertTrue("Android ID should not be blank", androidId.isNotBlank())

        val label = DeviceInfoHelper.getFormattedTerminalLabel(context)
        assertNotNull(label)
        assertTrue("Terminal label should not be blank", label.isNotBlank())
        assertTrue("Label should format with parentheses", label.contains("(") && label.contains(")"))
    }

    @Test
    fun testDrmDeviceIdentifier_executesSafely() {
        // Widevine DRM may or may not be available on emulators/test runners,
        // but must execute and release safely without throwing uncaught exceptions.
        val drmId = DrmDeviceIdentifier.getWidevineDeviceId()
        if (drmId != null) {
            assertTrue(drmId.isNotBlank())
        }
    }

    @Test
    fun testPosDeviceIdentity_resolvesReliableIdentifier() {
        val deviceId = PosDeviceIdentity.getDeviceIdentifier(context)
        assertNotNull(deviceId)
        assertTrue(deviceId.isNotBlank())

        val terminalLabel = PosDeviceIdentity.getDisplayTerminalName(context)
        assertNotNull(terminalLabel)
        assertTrue(terminalLabel.isNotBlank())

        val signature = PosDeviceIdentity.signShiftChallenge(context, "cashier_101", 1690000000000L)
        assertNotNull(signature)
        assertTrue(signature.isNotBlank())
    }
}
