package com.timur.receiptlogger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.timur.receiptlogger.ui.AddReceiptScreen
import com.timur.receiptlogger.ui.ReceiptListScreen

private object Routes {
    const val LIST = "list"
    const val ADD = "add"
}

@Composable
fun ReceiptApp(viewModel: ReceiptViewModel = viewModel()) {
    val navController: NavHostController = rememberNavController()
    val receipts by viewModel.receipts.collectAsState()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            ReceiptListScreen(
                receipts = receipts,
                onAddClick = { navController.navigate(Routes.ADD) }
            )
        }
        composable(Routes.ADD) {
            AddReceiptScreen(
                onBack = { navController.popBackStack() },
                onSave = { photoPath, amount, note ->
                    viewModel.addReceipt(photoPath, amount, note)
                    navController.popBackStack()
                }
            )
        }
    }
}
