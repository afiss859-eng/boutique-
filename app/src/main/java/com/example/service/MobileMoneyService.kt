package com.example.service

import com.example.data.model.PaymentProvider
import com.example.data.model.TransactionType

object MobileMoneyService {

    /**
     * Calculates customer fee for a withdrawal / transfer in Burkina Faso (XOF/FCFA)
     */
    fun calculateCustomerFee(amount: Long, type: TransactionType, provider: PaymentProvider): Long {
        if (amount <= 0) return 0
        return when (type) {
            TransactionType.DEPOT -> 0 // Dépôt is generally free for customer
            TransactionType.RETRAIT -> {
                when (provider) {
                    PaymentProvider.ORANGE_MONEY -> {
                        when {
                            amount <= 1000 -> 50
                            amount <= 5000 -> 150
                            amount <= 10000 -> 300
                            amount <= 25000 -> 600
                            amount <= 50000 -> 1100
                            amount <= 100000 -> 1800
                            amount <= 250000 -> 2800
                            amount <= 500000 -> 4500
                            else -> (amount * 0.01).toLong()
                        }
                    }
                    PaymentProvider.MOOV_MONEY -> {
                        when {
                            amount <= 1000 -> 45
                            amount <= 5000 -> 140
                            amount <= 10000 -> 280
                            amount <= 25000 -> 550
                            amount <= 50000 -> 1000
                            amount <= 100000 -> 1700
                            else -> (amount * 0.009).toLong()
                        }
                    }
                    PaymentProvider.WAVE -> {
                        // 1% flat fee on withdrawals/transfers
                        (amount * 0.01).toLong()
                    }
                    PaymentProvider.ESPECES -> 0
                }
            }
            TransactionType.TRANSFERT -> {
                when (provider) {
                    PaymentProvider.WAVE -> (amount * 0.01).toLong()
                    else -> (amount * 0.012).toLong()
                }
            }
            TransactionType.ACHAT_CREDIT -> 0
            TransactionType.VENTE_PRODUIT -> 0
        }
    }

    /**
     * Calculates the agent's net commission (profit earned by the kiosk)
     */
    fun calculateAgentCommission(amount: Long, type: TransactionType, provider: PaymentProvider): Long {
        if (amount <= 0) return 0
        return when (type) {
            TransactionType.DEPOT -> {
                // Agent earns commission on deposit
                when {
                    amount <= 5000 -> 50
                    amount <= 15000 -> 125
                    amount <= 30000 -> 250
                    amount <= 50000 -> 400
                    amount <= 100000 -> 750
                    amount <= 250000 -> 1500
                    else -> (amount * 0.006).toLong()
                }
            }
            TransactionType.RETRAIT -> {
                // Agent earns cut of withdrawal fee
                val fee = calculateCustomerFee(amount, type, provider)
                (fee * 0.45).toLong()
            }
            TransactionType.ACHAT_CREDIT -> {
                // Airtime margin around 4-5%
                (amount * 0.045).toLong()
            }
            TransactionType.TRANSFERT -> (amount * 0.004).toLong()
            TransactionType.VENTE_PRODUIT -> 0
        }
    }

    /**
     * Generates a valid USSD string for quick phone dialer
     * Orange Money Burkina: *144#
     * Moov Money: *555#
     * Wave: 145 / direct app link
     */
    fun buildUssdCode(
        provider: PaymentProvider,
        type: TransactionType,
        recipientPhone: String,
        amount: Long
    ): String {
        val cleanPhone = recipientPhone.replace(" ", "").replace("+226", "")
        return when (provider) {
            PaymentProvider.ORANGE_MONEY -> {
                when (type) {
                    TransactionType.DEPOT -> "*144*4*1*$cleanPhone*$amount#"
                    TransactionType.RETRAIT -> "*144*4*2*$cleanPhone*$amount#"
                    TransactionType.ACHAT_CREDIT -> "*144*2*1*$cleanPhone*$amount#"
                    TransactionType.TRANSFERT -> "*144*1*1*$cleanPhone*$amount#"
                    else -> "*144#"
                }
            }
            PaymentProvider.MOOV_MONEY -> {
                when (type) {
                    TransactionType.DEPOT -> "*555*1*1*$cleanPhone*$amount#"
                    TransactionType.RETRAIT -> "*555*2*1*$cleanPhone*$amount#"
                    TransactionType.ACHAT_CREDIT -> "*555*3*$cleanPhone*$amount#"
                    else -> "*555#"
                }
            }
            PaymentProvider.WAVE -> "*145#"
            PaymentProvider.ESPECES -> ""
        }
    }
}
