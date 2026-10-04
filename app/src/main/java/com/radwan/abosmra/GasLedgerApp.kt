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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.radwan.abosmra.ui.screens.AddCustomerScreen
import com.radwan.abosmra.ui.screens.AddDebtScreen
import com.radwan.abosmra.ui.screens.AddPaymentScreen
import com.radwan.abosmra.ui.screens.AreasScreen
import com.radwan.abosmra.ui.screens.CustomerProfileScreen
import com.radwan.abosmra.ui.screens.CustomerTransactionsScreen
import com.radwan.abosmra.ui.screens.CustomersScreen
import com.radwan.abosmra.ui.screens.DailyCollectionsScreen
import com.radwan.abosmra.ui.screens.DailyDebtsScreen
import com.radwan.abosmra.ui.screens.FollowUpScreen
import com.radwan.abosmra.ui.screens.HomeScreen
import com.radwan.abosmra.ui.screens.ReportsScreen
import com.radwan.abosmra.ui.screens.SettingsScreen
import com.radwan.abosmra.ui.screens.SmartSearchScreen
import com.radwan.abosmra.ui.screens.StatementScreen
import com.radwan.abosmra.ui.screens.TopDebtorsScreen
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
                    if (currentRoute in bottomRoutes) {
                        NavigationBar {
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
                                    label = { Text(item.label) }
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
                        HomeScreen(
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
                        CustomersScreen(
                            vm = vm,
                            onAdd = { navController.navigate(Routes.ADD_CUSTOMER) },
                            onCustomer = { navController.navigate(Routes.customer(it)) }
                        )
                    }
                    composable(Routes.ADD_CUSTOMER) {
                        AddCustomerScreen(
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
                        CustomerProfileScreen(
                            vm = vm,
                            customerId = id,
                            onBack = navController::popBackStack,
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
                        AddDebtScreen(vm, id, navController::popBackStack)
                    }
                    composable(
                        Routes.ADD_PAYMENT,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) {
                        val id = it.arguments?.getString("customerId").orEmpty()
                        AddPaymentScreen(vm, id, navController::popBackStack)
                    }
                    composable(
                        Routes.TRANSACTIONS,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) {
                        val id = it.arguments?.getString("customerId").orEmpty()
                        CustomerTransactionsScreen(vm, id, navController::popBackStack)
                    }
                    composable(
                        Routes.STATEMENT,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) {
                        val id = it.arguments?.getString("customerId").orEmpty()
                        StatementScreen(vm, id, navController::popBackStack)
                    }
                    composable(Routes.COLLECTIONS) {
                        DailyCollectionsScreen(vm, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.DAILY_DEBTS) {
                        DailyDebtsScreen(vm, navController::popBackStack, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.TOP_DEBTORS) {
                        TopDebtorsScreen(vm, navController::popBackStack, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.AREAS) {
                        AreasScreen(vm, navController::popBackStack, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.SEARCH) {
                        SmartSearchScreen(
                            vm,
                            navController::popBackStack,
                            onCustomer = { navController.navigate(Routes.customer(it)) },
                            onDebt = { navController.navigate(Routes.addDebt(it)) },
                            onPayment = { navController.navigate(Routes.addPayment(it)) }
                        )
                    }
                    composable(Routes.REPORTS) {
                        ReportsScreen(vm)
                    }
                    composable(Routes.FOLLOWUP) {
                        FollowUpScreen(vm, navController::popBackStack, onCustomer = { navController.navigate(Routes.customer(it)) })
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(vm)
                    }
                }
            }
        }
    }
}
