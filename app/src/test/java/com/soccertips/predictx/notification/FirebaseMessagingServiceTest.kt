package com.soccertips.predictx.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseMessagingServiceTest {

    @Test
    fun `selectPayloadHandling prefers data when both are present`() {
        val service = FirebaseMessagingService()

        val result = service.selectPayloadHandling(hasData = true, hasNotification = true)

        assertEquals(FirebaseMessagingService.PayloadHandling.DATA, result)
    }

    @Test
    fun `selectPayloadHandling uses notification when only notification is present`() {
        val service = FirebaseMessagingService()

        val result = service.selectPayloadHandling(hasData = false, hasNotification = true)

        assertEquals(FirebaseMessagingService.PayloadHandling.NOTIFICATION, result)
    }

    @Test
    fun `selectPayloadHandling returns none when no payloads are present`() {
        val service = FirebaseMessagingService()

        val result = service.selectPayloadHandling(hasData = false, hasNotification = false)

        assertEquals(FirebaseMessagingService.PayloadHandling.NONE, result)
    }
}
