package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CniEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiAiService {
    private const val TAG = "GeminiAiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun askAssistant(
        userPrompt: String,
        recentTransactions: List<TransactionEntity>,
        lowStockProducts: List<ProductEntity>,
        registeredCnisCount: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // Build rich financial & inventory context
        val fcfa = NumberFormat.getNumberInstance(Locale.FRENCH)
        val totalVolume = recentTransactions.sumOf { it.amount }
        val totalCommission = recentTransactions.sumOf { it.commission }
        val txCount = recentTransactions.size

        val lowStockSummary = if (lowStockProducts.isEmpty()) {
            "Tous les produits sont en stock suffisant."
        } else {
            lowStockProducts.joinToString(", ") { "${it.name} (reste: ${it.stockQuantity} ${it.unit})" }
        }

        val systemContext = """
            Tu es l'Assistant IA Intelligent de la boutique et agence Mobile Money 'Wend-Lamita' (Burkina Faso / UEMOA).
            Tu es courtois, bilingue Français / expressions locales de politesse (Ne y windiga, Barka, Incha'Allah), et expert en:
            - Gestion de caisse Mobile Money (Orange Money *144#, Moov Money *555#, Wave *145#)
            - Réglementation KYC / Registre CNI (BCEAO / ARCEP)
            - Gestion des stocks boutique, rentabilité et calcul de commissions
            - Détection des fraudes et arnaques par faux SMS de transfert
            
            DONNÉES DU KIOSQUE ACTUEL:
            - Nombre de transactions enregistrées: $txCount
            - Volume total brassé: ${fcfa.format(totalVolume)} FCFA
            - Commissions totales nettes gagnées: ${fcfa.format(totalCommission)} FCFA
            - Alertes Stock Faible: $lowStockSummary
            - Nombre de clients enregistrés dans le registre CNI: $registeredCnisCount
            
            Réponds de façon claire, synthétique, professionnelle et immédiatement actionnable pour le gérant ou caissier.
        """.trimIndent()

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide high-quality local heuristic response if no API key configured
            return@withContext generateSmartLocalResponse(
                userPrompt,
                totalVolume,
                totalCommission,
                txCount,
                lowStockProducts,
                registeredCnisCount
            )
        }

        try {
            val endpoint = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val jsonPayload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", "$systemContext\n\nQuestion de l'utilisateur: $userPrompt"))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPayload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful && responseString.isNotEmpty()) {
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Pas de réponse générée.")
                    }
                }
            }
            Log.w(TAG, "Gemini API error ${response.code}: $responseString")
            generateSmartLocalResponse(userPrompt, totalVolume, totalCommission, txCount, lowStockProducts, registeredCnisCount)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call exception", e)
            generateSmartLocalResponse(userPrompt, totalVolume, totalCommission, txCount, lowStockProducts, registeredCnisCount)
        }
    }

    private fun generateSmartLocalResponse(
        prompt: String,
        totalVolume: Long,
        totalCommission: Long,
        txCount: Int,
        lowStock: List<ProductEntity>,
        cniCount: Int
    ): String {
        val p = prompt.lowercase()
        val fcfa = NumberFormat.getNumberInstance(Locale.FRENCH)

        return when {
            p.contains("bilan") || p.contains("rapport") || p.contains("journée") || p.contains("chiffre") -> {
                """
                📊 **Bilan d'Activité Wend-Lamita**
                
                • **Volume de transactions :** ${fcfa.format(totalVolume)} FCFA
                • **Nombre d'opérations :** $txCount transactions
                • **Commissions nettes engrangées :** ${fcfa.format(totalCommission)} FCFA
                • **Clients enregistrés au registre CNI :** $cniCount personnes
                
                💡 *Conseil de gestion :* N'oubliez pas d'effectuer la clôture de caisse quotidienne avant 20h pour vérifier l'exactitude des espèces physiques.
                """.trimIndent()
            }
            p.contains("cni") || p.contains("carte") || p.contains("kyc") || p.contains("identité") -> {
                """
                🪪 **Réglementation et Contrôle CNI / KYC**
                
                • Le numéro CNIB standard au Burkina Faso commence par la lettre **B** suivie de 7 ou 8 chiffres (ex: `B12894732`).
                • **Obligation légale :** Tout retrait ou transfert de plus de 5.000 FCFA doit impérativement faire l'objet d'un enregistrement au registre avec numéro de pièce valide.
                • Vous avez actuellement **$cniCount clients** enregistrés dans votre registre sécurisé.
                """.trimIndent()
            }
            p.contains("stock") || p.contains("rupture") || p.contains("produit") || p.contains("commande") -> {
                if (lowStock.isEmpty()) {
                    "📦 **Gestion des Stocks :**\n\nTous vos articles sont à un niveau de stock confortable. Aucun produit en alerte critique."
                } else {
                    val items = lowStock.joinToString("\n") { "• **${it.name}** : reste ${it.stockQuantity} ${it.unit} (seuil alerte: ${it.minStockAlert})" }
                    "⚠️ **Articles en Rupture ou Seuil Critique :**\n\n$items\n\n💡 *Action recommandée :* Passez commande auprès de vos grossistes pour éviter les pertes de ventes."
                }
            }
            p.contains("arnaque") || p.contains("fraude") || p.contains("sécurité") || p.contains("faux sms") -> {
                """
                🛡️ **Conseils Anti-Fraude & Arnaques Mobile Money :**
                
                1. **Vérifiez TOUJOURS votre solde réel** en composant le code USSD (*144# ou *555#) avant de donner de l'argent physique à un client.
                2. Ne faites jamais confiance à un simple SMS affiché sur le téléphone du client.
                3. Ne composez jamais de code USSD dicté par téléphone par quelqu'un prétendant être du service technique.
                4. Gardez votre code secret agent strictement confidentiel.
                """.trimIndent()
            }
            p.contains("commission") || p.contains("tarif") || p.contains("frais") -> {
                """
                💰 **Barème des Commissions Kiosque :**
                
                • **Dépôt OM / Moov :** Gratuit pour le client. Vous gagnez une commission de 50 FCFA à 1.500 FCFA selon le palier.
                • **Retrait OM :** Vous percevez environ 45% des frais facturés au client.
                • **Cartes de recharge :** Marge bénéficiaire de 4% à 5% par carte vendue.
                """.trimIndent()
            }
            else -> {
                """
                👋 **Wend-Lamita Assistant IA**
                
                Bonjour ! Je suis à votre service pour piloter votre agence.
                
                • Volume total : **${fcfa.format(totalVolume)} FCFA**
                • Commissions : **${fcfa.format(totalCommission)} FCFA** ($txCount opérations)
                • Articles à réapprovisionner : **${lowStock.size}**
                
                Posez-moi vos questions sur le bilan, les calculs de commissions, la vérification CNI ou la sécurité de votre caisse !
                """.trimIndent()
            }
        }
    }
}
