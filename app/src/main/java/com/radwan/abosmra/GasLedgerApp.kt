package com.radwan.abosmra

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.radwan.abosmra.ui.screens.AddCustomerScreenV3
import com.radwan.abosmra.ui.screens.AddDebtScreenV3
import com.radwan.abosmra.ui.screens.AddPaymentScreenV3
import com.radwan.abosmra.ui.screens.AreasScreenV4
import com.radwan.abosmra.ui.screens.CustomerProfileScreenV6
import com.radwan.abosmra.ui.screens.CustomerTransactionsScreenV6
import com.radwan.abosmra.ui.screens.CustomersScreenV3
import com.radwan.abosmra.ui.screens.DailyCollectionsScreenV4
import com.radwan.abosmra.ui.screens.DailyDebtsScreenV4
import com.radwan.abosmra.ui.screens.FollowUpScreenV4
import com.radwan.abosmra.ui.screens.HomeScreenV3
import com.radwan.abosmra.ui.screens.ReportsScreenV3
import com.radwan.abosmra.ui.screens.SettingsScreenV3
import com.radwan.abosmra.ui.screens.SmartSearchScreenV4
import com.radwan.abosmra.ui.screens.StatementScreenV7
import com.radwan.abosmra.ui.screens.TopDebtorsScreenV4
import com.radwan.abosmra.ui.theme.GasLedgerTheme

object Routes {
    const val HOME = "home"
    const val CUSTOMERS = "customers"
    const val ADD_CUSTOMER = "add_customer"
    const val CUSTOMER = "customer/{customerId}"
    const val ADD_DEBT = "add_debt/{customerId}"
    const val ADD_PAYMENT = "add_payment/{customerId}"
    const val TRANSACTIONS = "transactions/{customerId}"
    const val STATEMENT = "statement/{customerId}"
    const val COLLECTIONS = "collections"
    const val DAILY_DEBTS = "daily_debts"
    const val TOP_DEBTORS = "top_debtors"
    const val AREAS = "areas"
    const val SEARCH = "search"
    const val REPORTS = "reports"
    const val FOLLOWUP = "followup"
    const val SETTINGS = "settings"

    fun customer(id: String) = "customer/$id"
    fun addDebt(id: String) = "add_debt/$id"
    fun addPayment(id: String) = "add_payment/$id"
    fun transactions(id: String) = "transactions/$id"
    fun statement(id: String) = "statement/$id"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun GasLedgerApp(vm: GasLedgerViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomItems = listOf(
        BottomDestination(Routes.HOME, "الرئيسية", Icons.Rounded.Home),
        BottomDestination(Routes.CUSTOMERS, "الزبائن", Icons.Rounded.People),
        BottomDestination(Routes.COLLECTIONS, "التحصيلات", Icons.Rounded.Payments),
        BottomDestination(Routes.REPORTS, "التقارير", Icons.Rounded.Assessment),
        BottomDestination(Routes.SETTINGS, "المزيد", Icons.Rounded.Settings)
    )
    val bottomRoutes = bottomItems.map { it.route }.toSet()

    GasLedgerTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                bottomBar = {
                    if (currentRoute != null && currentRoute in bottomRoutes) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            bottomItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                                    alwaysShowLabel = false,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Routes.HOME) {
                        HomeScreenV3(
                            vm = vm,
                            onCustomers = { navController.navigate(Routes.CUSTOMERS) },
                            onAddCustomer = { navController.navigate(Routes.ADD_CUSTOMER) },
                            onSearch = { navController.navigate(Routes.SEARCH) },
                            onCollections = { navController.navigate(Routes.COLLECTIONS) },
                            onDailyDebts = { navController.navigate(Routes.DAILY_DEBTS) },
                            onTopDebtors = { navController.navigate(Routes.TOP_DEBTORS) },
                            onAreas = { navController.navigate(Routes.AREAS) },
                            onFollowUp = { navController.navigate(Routes.FOLLOWUP) },
                            onCustomer = { navController.navigate(Routes.customer(it)) }
                        )
                    }
                    composable(Routes.CUSTOMERS) {
                        CustomersScreenV3(
                            vm = vm,
                            onAdd = { navController.navigate(Routes.ADD_CUSTOMER) },
                            onCustomer = { navController.navigate(Routes.customer(it)) }
                        )
                    }
                    composable(Routes.ADD_CUSTOMER) {
                        AddCustomerScreenV3(
                            vm = vm,
                            onBack = navController::popBackStack,
                            onSaved = { navController.navigate(Routes.customer(it)) {
                                popUpTo(Routes.ADD_CUSTOMER) { inclusive = true }
                            } }
                        )
                    }
                    composable(
                        Routes.CUSTOMER,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) {
                        val id = it.arguments?.getString("customerId").orEmpty()
                        CustomerProfileScreenV6(
                            vm = vm,
                            customerId = id,
                            onBack = navController::popBackStack,
                            onDeleted = {
                                navController.navigate(Routes.CUSTOMERS) {
                                    popUpTo(Routes.CUSTOMERS) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onAddDebt = { navController.navigate(Routes.addDebt(id)) },
                            onPayment = { navController.navigate(Routes.addPayment(id)) },
                            onTransactions = { navController.navigate(Routes.transactions(id)) },
                            onStatement = { navController.navigate(Routes.statement(id)) }
                        )
                    }
                    composable(
                        Routes.ADD_DEBT,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) {
                        val id = it.arguments?.getString("customerId").orEmpty()
                        AddDebtScreenV3(vm, id, navController::popBackStack)
                    }
                    composable(
                        Routes.ADD_PAYMENT,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) {
                        val id = it.arguments?.getString("customerId").orEmpty()
                        AddPaymentScreenV3(vm, id, navController::popBackStack)
                    }
                    composable(
                        Routes.TRANSACTIONS,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) {
                        val id = it.arguments?.getString("customerId").orEmpty()
                        CustomerTransactionsScreenV6(vm, id, navController::popBackStack)
                    }
                    composable(
                        Routes.STATEMENT,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) {
                        val id = it.arguments?.getString("customerId").orEmpty()
                        StatementScreenV7(vm, id, navController::popBackStack)
                    }
                    composable(Routes.COLLECTIONS) {
                        DailyCollectionsScreenV4(vm, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.DAILY_DEBTS) {
                        DailyDebtsScreenV4(vm, navController::popBackStack, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.TOP_DEBTORS) {
                        TopDebtorsScreenV4(vm, navController::popBackStack, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.AREAS) {
                        AreasScreenV4(vm, navController::popBackStack, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.SEARCH) {
                        SmartSearchScreenV4(
                            vm,
                            navController::popBackStack,
                            onCustomer = { navController.navigate(Routes.customer(it)) },
                            onDebt = { navController.navigate(Routes.addDebt(it)) },
                            onPayment = { navController.navigate(Routes.addPayment(it)) }
                        )
                    }
                    composable(Routes.REPORTS) {
                        ReportsScreenV3(vm)
                    }
                    composable(Routes.FOLLOWUP) {
                        FollowUpScreenV4(vm, navController::popBackStack, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreenV3(vm)
                    }
                }
            }
        }
    }
}
