package com.example.digitscan.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.digitscan.DigitScanApp
import com.example.digitscan.data.SessionStatus
import com.example.digitscan.ui.screens.CaptureScreen
import com.example.digitscan.ui.screens.HistoryScreen
import com.example.digitscan.ui.screens.ReviewScreen
import com.example.digitscan.ui.screens.SessionDetailScreen

private object Routes {
    const val HISTORY = "history"
    const val CAPTURE = "capture/{id}"
    const val REVIEW = "review/{id}"
    const val DETAIL = "session/{id}"

    fun capture(id: Long) = "capture/$id"
    fun review(id: Long) = "review/$id"
    fun detail(id: Long) = "session/$id"
}

private val idArgs = listOf(navArgument("id") { type = NavType.LongType })

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val repo = (LocalContext.current.applicationContext as DigitScanApp).repository

    NavHost(navController = nav, startDestination = Routes.HISTORY) {
        composable(Routes.HISTORY) {
            HistoryScreen(
                repo = repo,
                // Незавершённая сессия продолжается с этапа 1 (съёмки), завершённая открывается как результат.
                onOpen = { s ->
                    if (s.status == SessionStatus.DONE) nav.navigate(Routes.detail(s.id))
                    else nav.navigate(Routes.capture(s.id))
                },
                onCreated = { id -> nav.navigate(Routes.capture(id)) }
            )
        }
        composable(Routes.CAPTURE, arguments = idArgs) { entry ->
            val id = entry.arguments!!.getLong("id")
            CaptureScreen(
                repo = repo,
                sessionId = id,
                onBack = { nav.popBackStack() },
                onNext = { nav.navigate(Routes.review(id)) }
            )
        }
        composable(Routes.REVIEW, arguments = idArgs) { entry ->
            val id = entry.arguments!!.getLong("id")
            ReviewScreen(
                repo = repo,
                sessionId = id,
                onBack = { nav.popBackStack() },
                onFinished = {
                    nav.navigate(Routes.detail(id)) { popUpTo(Routes.HISTORY) }
                }
            )
        }
        composable(Routes.DETAIL, arguments = idArgs) { entry ->
            val id = entry.arguments!!.getLong("id")
            SessionDetailScreen(
                repo = repo,
                sessionId = id,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
