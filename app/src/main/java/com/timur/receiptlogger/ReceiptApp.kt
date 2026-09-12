package com.timur.receiptlogger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timur.receiptlogger.export.ExportUtils
import com.timur.receiptlogger.ui.AddReceiptScreen
import com.timur.receiptlogger.ui.AddTripScreen
import com.timur.receiptlogger.ui.TripDetailScreen
import com.timur.receiptlogger.ui.TripListScreen

private object Routes {
    const val TRIPS = "trips"
    const val ADD_TRIP = "trips/add"
    const val TRIP_DETAIL = "trip/{tripId}"
    const val ADD_RECEIPT = "trip/{tripId}/add"

    fun tripDetail(tripId: Long) = "trip/$tripId"
    fun addReceipt(tripId: Long) = "trip/$tripId/add"
}

@Composable
fun ReceiptApp(viewModel: ReceiptViewModel = viewModel()) {
    val navController: NavHostController = rememberNavController()
    val trips by viewModel.trips.collectAsState()

    NavHost(navController = navController, startDestination = Routes.TRIPS) {
        composable(Routes.TRIPS) {
            TripListScreen(
                trips = trips,
                totalForTrip = { tripId -> viewModel.totalForTrip(tripId) },
                onTripClick = { tripId -> navController.navigate(Routes.tripDetail(tripId)) },
                onAddTripClick = { navController.navigate(Routes.ADD_TRIP) }
            )
        }

        composable(Routes.ADD_TRIP) {
            AddTripScreen(
                onBack = { navController.popBackStack() },
                onCreate = { name, startDate, endDate -> viewModel.addTrip(name, startDate, endDate) },
                onCreated = { tripId ->
                    // Replace the "add trip" screen with the new trip's detail screen,
                    // so Back from there goes to the trip list, not back to an empty form.
                    navController.navigate(Routes.tripDetail(tripId)) {
                        popUpTo(Routes.ADD_TRIP) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: return@composable
            val trip = trips.find { it.id == tripId }
            val receiptsFlow = remember(tripId) { viewModel.receiptsForTrip(tripId) }
            val totalFlow = remember(tripId) { viewModel.totalForTrip(tripId) }
            val receipts by receiptsFlow.collectAsState(initial = emptyList())
            val total by totalFlow.collectAsState(initial = 0.0)
            val context = LocalContext.current

            TripDetailScreen(
                tripName = trip?.name ?: "Trip",
                startDate = trip?.startDate,
                endDate = trip?.endDate,
                total = total,
                receipts = receipts,
                onBack = { navController.popBackStack() },
                onAddClick = { navController.navigate(Routes.addReceipt(tripId)) },
                onExportClick = {
                    val currentTrip = trip
                    if (currentTrip != null) {
                        val file = ExportUtils.exportTripReport(context, currentTrip, receipts)
                        ExportUtils.shareFile(context, file)
                    }
                }
            )
        }

        composable(
            route = Routes.ADD_RECEIPT,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: return@composable
            val tripName = trips.find { it.id == tripId }?.name.orEmpty()
            AddReceiptScreen(
                tripName = tripName,
                onBack = { navController.popBackStack() },
                onSave = { photoPath, amount, note ->
                    viewModel.addReceipt(tripId, photoPath, amount, note)
                    navController.popBackStack()
                }
            )
        }
    }
}
