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
    const val EDIT_TRIP = "trip/{tripId}/edit"
    const val TRIP_DETAIL = "trip/{tripId}"
    const val ADD_RECEIPT = "trip/{tripId}/add"
    const val EDIT_RECEIPT = "trip/{tripId}/receipt/{receiptId}/edit"

    fun tripDetail(tripId: Long) = "trip/$tripId"
    fun editTrip(tripId: Long) = "trip/$tripId/edit"
    fun addReceipt(tripId: Long) = "trip/$tripId/add"
    fun editReceipt(tripId: Long, receiptId: Long) = "trip/$tripId/receipt/$receiptId/edit"
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
                onAddTripClick = { navController.navigate(Routes.ADD_TRIP) },
                onEditTripClick = { tripId -> navController.navigate(Routes.editTrip(tripId)) },
                onDeleteTrip = { trip -> viewModel.deleteTrip(trip) }
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
            route = Routes.EDIT_TRIP,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: return@composable
            val existingTrip = trips.find { it.id == tripId }

            AddTripScreen(
                existingTrip = existingTrip,
                onBack = { navController.popBackStack() },
                onCreate = { name, startDate, endDate ->
                    viewModel.updateTrip(tripId, name, startDate, endDate)
                    tripId
                },
                onCreated = { navController.popBackStack() }
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
                },
                onEditReceipt = { entry -> navController.navigate(Routes.editReceipt(tripId, entry.id)) },
                onDeleteReceipt = { entry -> viewModel.deleteReceipt(entry) }
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

        composable(
            route = Routes.EDIT_RECEIPT,
            arguments = listOf(
                navArgument("tripId") { type = NavType.LongType },
                navArgument("receiptId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: return@composable
            val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: return@composable
            val tripName = trips.find { it.id == tripId }?.name.orEmpty()
            val receiptsFlow = remember(tripId) { viewModel.receiptsForTrip(tripId) }
            // null means "the flow hasn't emitted yet" (distinct from "emitted an empty list"),
            // so AddReceiptScreen is only composed once the real entry is known — otherwise its
            // remembered form fields would capture a still-null entry on the first frame and
            // never notice the real data arriving a moment later.
            val receipts by receiptsFlow.collectAsState(initial = null)
            val loadedReceipts = receipts ?: return@composable
            val existingEntry = loadedReceipts.find { it.id == receiptId }

            AddReceiptScreen(
                tripName = tripName,
                existingEntry = existingEntry,
                onBack = { navController.popBackStack() },
                onSave = { photoPath, amount, note ->
                    val entry = existingEntry
                    if (entry != null) {
                        viewModel.updateReceipt(entry, photoPath, amount, note)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}
