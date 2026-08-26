package com.example

import com.example.data.model.PaymentProvider
import com.example.data.model.TransactionType
import com.example.service.CniOcrService
import com.example.service.MobileMoneyService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileMoneyUnitTest {

    @Test
    fun testOrangeMoneyFeeCalculation() {
        val fee10k = MobileMoneyService.calculateCustomerFee(10000, TransactionType.RETRAIT, PaymentProvider.ORANGE_MONEY)
        assertEquals(300L, fee10k)

        val feeDeposit = MobileMoneyService.calculateCustomerFee(10000, TransactionType.DEPOT, PaymentProvider.ORANGE_MONEY)
        assertEquals(0L, feeDeposit)
    }

    @Test
    fun testCniValidation() {
        val validCni = CniOcrService.validateCniNumber("B12894732")
        assertTrue(validCni.isValid)

        val invalidCni = CniOcrService.validateCniNumber("12345")
        assertTrue(!invalidCni.isValid)
    }

    @Test
    fun testUssdCodeBuilder() {
        val code = MobileMoneyService.buildUssdCode(
            PaymentProvider.ORANGE_MONEY,
            TransactionType.DEPOT,
            "70123456",
            25000
        )
        assertEquals("*144*4*1*70123456*25000#", code)
    }
}
