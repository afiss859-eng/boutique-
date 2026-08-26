package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CniEntity
import com.example.service.LocalAppLanguage
import com.example.service.tr
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.TopFintechBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangePrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.WendLamitaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WendLamitaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val loginError by viewModel.authErrorMessage.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
                MyApplicationTheme {
                    AnimatedContent(
                        targetState = isLoggedIn,
                        transitionSpec = {
                            fadeIn() + slideInVertically { it / 4 } togetherWith fadeOut() + slideOutVertically { -it / 4 }
                        },
                        label = "auth_transition"
                    ) { loggedIn ->
                        if (!loggedIn) {
                            LoginScreen(
                                errorMessage = loginError,
                                onLoginWithPin = { pin -> viewModel.loginWithPin(pin) },
                                onLoginWithPassword = { u, p -> viewModel.loginWithPassword(u, p) },
                                onLoginBiometric = { viewModel.loginWithBiometric() },
                                currentLanguage = currentLanguage,
                                onSelectLanguage = { lang -> viewModel.setLanguage(lang) }
                            )
                        } else {
                            MainKioskApp(
                                viewModel = viewModel,
                                currentUser = currentUser,
                                currentLanguage = currentLanguage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainKioskApp(
    viewModel: WendLamitaViewModel,
    currentUser: com.example.data.model.UserEntity?,
    currentLanguage: com.example.service.AppLanguage
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val cashFloat by viewModel.cashFloat.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val cniRecords by viewModel.allCniRecords.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val debts by viewModel.allDebts.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val lastCompletedTx by viewModel.lastCompletedTransaction.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allSubscriptions by viewModel.allSubscriptions.collectAsStateWithLifecycle()
    val activeSubscription by viewModel.activeSubscription.collectAsStateWithLifecycle()
    val allClosures by viewModel.allClosures.collectAsStateWithLifecycle()
    val allFloatAdjustments by viewModel.allFloatAdjustments.collectAsStateWithLifecycle()

    val pendingDebtsCount = remember(debts) { debts.count { !it.isSettled } }
    val totalDebtAmount = remember(debts) { debts.filter { !it.isSettled }.sumOf { it.remainingAmount } }

    // State to hold prefilled customer for Mobile Money screen
    var preselectedCniForOm by remember { mutableStateOf<CniEntity?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopFintechBar(
                storeName = "Wend-Lamita Services",
                agentName = currentUser?.fullName ?: "Caisse Principale",
                userRole = currentUser?.role,
                currentLanguage = currentLanguage,
                onSelectLanguage = { viewModel.setLanguage(it) },
                onOpenSubscription = { viewModel.selectTab(AppTab.SUBSCRIPTION) },
                onOpenAdmin = { viewModel.selectTab(AppTab.ADMIN_PANEL) },
                onLogout = { viewModel.logout() },
                onRefresh = {}
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Accueil"
                        )
                    },
                    label = { Text(tr("nav_dashboard")) },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.MOBILE_MONEY,
                    onClick = { viewModel.selectTab(AppTab.MOBILE_MONEY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.MOBILE_MONEY) Icons.Default.CurrencyExchange else Icons.Outlined.CurrencyExchange,
                            contentDescription = "Mobile Money"
                        )
                    },
                    label = { Text(tr("nav_mobile_money")) },
                    modifier = Modifier.testTag("nav_tab_mobile_money")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.CNI_REGISTRY,
                    onClick = { viewModel.selectTab(AppTab.CNI_REGISTRY) },
                    icon = {
                        BadgedBox(badge = {
                            if (cniRecords.isNotEmpty()) {
                                Badge { Text("${cniRecords.size}") }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTab == AppTab.CNI_REGISTRY) Icons.Default.Badge else Icons.Outlined.Badge,
                                contentDescription = "CNI KYC"
                            )
                        }
                    },
                    label = { Text(tr("nav_cni")) },
                    modifier = Modifier.testTag("nav_tab_cni")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.BOUTIQUE_POS,
                    onClick = { viewModel.selectTab(AppTab.BOUTIQUE_POS) },
                    icon = {
                        BadgedBox(badge = {
                            if (cart.isNotEmpty()) {
                                Badge { Text("${cart.sumOf { it.quantity }}") }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTab == AppTab.BOUTIQUE_POS) Icons.Default.Storefront else Icons.Outlined.Storefront,
                                contentDescription = "Boutique"
                            )
                        }
                    },
                    label = { Text(tr("nav_boutique")) },
                    modifier = Modifier.testTag("nav_tab_boutique")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.DEBTS_BOOK,
                    onClick = { viewModel.selectTab(AppTab.DEBTS_BOOK) },
                    icon = {
                        BadgedBox(badge = {
                            if (pendingDebtsCount > 0) {
                                Badge { Text("$pendingDebtsCount") }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTab == AppTab.DEBTS_BOOK) Icons.Default.Book else Icons.Outlined.Book,
                                contentDescription = "Dettes"
                            )
                        }
                    },
                    label = { Text(tr("nav_debts")) },
                    modifier = Modifier.testTag("nav_tab_debts")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.ADMIN_PANEL,
                    onClick = { viewModel.selectTab(AppTab.ADMIN_PANEL) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.ADMIN_PANEL) Icons.Default.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = if (currentTab == AppTab.ADMIN_PANEL) OrangePrimary else LocalContentColor.current
                        )
                    },
                    label = { Text(tr("nav_admin")) },
                    modifier = Modifier.testTag("nav_tab_admin")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.SUBSCRIPTION,
                    onClick = { viewModel.selectTab(AppTab.SUBSCRIPTION) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.SUBSCRIPTION) Icons.Default.WorkspacePremium else Icons.Outlined.WorkspacePremium,
                            contentDescription = "Abonnement"
                        )
                    },
                    label = { Text("Pro") },
                    modifier = Modifier.testTag("nav_tab_subscription")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> {
                    DashboardScreen(
                        cashFloat = cashFloat,
                        transactions = transactions,
                        lowStockCount = lowStockProducts.size,
                        cniCount = cniRecords.size,
                        pendingDebtsCount = pendingDebtsCount,
                        totalDebtAmount = totalDebtAmount,
                        onNavigate = { viewModel.selectTab(it) },
                        onViewTransaction = { _ -> }
                    )
                }
                AppTab.MOBILE_MONEY -> {
                    MobileMoneyScreen(
                        cniRecords = cniRecords,
                        initialCustomer = preselectedCniForOm,
                        onClearInitialCustomer = { preselectedCniForOm = null },
                        onExecuteTransaction = { type, provider, amount, phone, name, cniNum, note ->
                            viewModel.executeMobileMoneyTransaction(
                                type = type,
                                provider = provider,
                                amount = amount,
                                clientPhone = phone,
                                clientName = name,
                                clientCniNumber = cniNum,
                                note = note
                            )
                        }
                    )
                }
                AppTab.CNI_REGISTRY -> {
                    CniRegistryScreen(
                        cniRecords = cniRecords,
                        onSaveCni = { viewModel.saveCniRecord(it) },
                        onDeleteCni = { viewModel.deleteCniRecord(it) },
                        onInitiateTransactionForCustomer = { cni ->
                            preselectedCniForOm = cni
                            viewModel.selectTab(AppTab.MOBILE_MONEY)
                        }
                    )
                }
                AppTab.BOUTIQUE_POS -> {
                    BoutiquePosScreen(
                        products = products,
                        cart = cart,
                        onAddToCart = { viewModel.addToCart(it) },
                        onRemoveFromCart = { viewModel.removeFromCart(it) },
                        onClearCart = { viewModel.clearCart() },
                        onCheckout = { name, phone, provider ->
                            viewModel.checkoutCart(name, phone, provider)
                        },
                        onSaveProduct = { viewModel.saveProduct(it) },
                        onDeleteProduct = { viewModel.deleteProduct(it) }
                    )
                }
                AppTab.DEBTS_BOOK -> {
                    CustomerDebtsScreen(
                        debts = debts,
                        onSaveDebt = { viewModel.saveDebt(it) },
                        onMakePayment = { debt, amt -> viewModel.makeDebtPayment(debt, amt) },
                        onDeleteDebt = { viewModel.deleteDebt(it) }
                    )
                }
                AppTab.AI_ASSISTANT -> {
                    AiAssistantScreen(
                        messages = aiMessages,
                        isThinking = isAiThinking,
                        onSendMessage = { viewModel.sendAiPrompt(it) }
                    )
                }
                AppTab.REPORTS -> {
                    ReportsScreen(
                        cashFloat = cashFloat,
                        transactions = transactions,
                        onUpdateCashFloat = { cash, om, moov, wave ->
                            viewModel.updateCashFloat(cash, om, moov, wave)
                        }
                    )
                }
                AppTab.ADMIN_PANEL -> {
                    AdminPanelScreen(
                        currentUser = currentUser,
                        cashFloat = cashFloat,
                        transactions = transactions,
                        allUsers = allUsers,
                        closures = allClosures,
                        floatAdjustments = allFloatAdjustments,
                        onSaveUser = { viewModel.saveUser(it) },
                        onDeleteUser = { viewModel.deleteUser(it) },
                        onPerformZClosure = { phys, theo, om, moov, wave, notes, callback ->
                            viewModel.performCashClosure(phys, theo, om, moov, wave, notes, callback)
                        },
                        onRecordFloatAdjustment = { type, prov, amt, note ->
                            viewModel.recordFloatAdjustment(type, prov, amt, note)
                        },
                        onLogout = { viewModel.logout() }
                    )
                }
                AppTab.SUBSCRIPTION -> {
                    SubscriptionScreen(
                        activeSubscription = activeSubscription,
                        allSubscriptions = allSubscriptions,
                        onActivateSubscription = { plan, txId, op, amt, days, callback ->
                            viewModel.activateSubscription(plan, txId, op, amt, days, callback)
                        }
                    )
                }
            }
        }

        // Success Receipt Dialog Modal
        lastCompletedTx?.let { tx ->
            ReceiptDialog(
                transaction = tx,
                onDismiss = { viewModel.dismissReceiptDialog() }
            )
        }
    }
}
