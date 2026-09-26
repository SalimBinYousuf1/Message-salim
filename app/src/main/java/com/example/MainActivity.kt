package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.telephony.NotificationHelper
import com.example.ui.screens.compose.NewMessageScreen
import com.example.ui.screens.compose.NewMessageViewModel
import com.example.ui.screens.conversation.ConversationDetailScreen
import com.example.ui.screens.conversation.ConversationDetailViewModel
import com.example.ui.screens.conversationlist.ConversationListScreen
import com.example.ui.screens.conversationlist.ConversationListViewModel
import com.example.ui.screens.media.MediaViewerScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.SalimTheme
import java.net.URLDecoder
import java.net.URLEncoder

const val ROUTE_CONVERSATIONS = "conversations"
const val ROUTE_CONVERSATION_DETAIL = "conversation/{threadId}/{address}"
const val ROUTE_COMPOSE = "compose?initialBody={initialBody}"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_MEDIA_VIEWER = "media_viewer?uri={uri}"

class MainActivity : ComponentActivity() {

    private val conversationListViewModel: ConversationListViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            LaunchedEffect(settings.flagSecureEnabled) {
                com.example.telephony.SecurityHelper.applyWindowSecurity(this@MainActivity, settings.flagSecureEnabled)
            }

            SalimTheme(settings = settings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    SalimNavApp(
                        conversationListViewModel = conversationListViewModel,
                        settingsViewModel = settingsViewModel,
                        initialIntent = intent
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        conversationListViewModel.refreshStatus()
        settingsViewModel.refreshDefaultSmsStatus()
    }
}

@Composable
fun SalimNavApp(
    conversationListViewModel: ConversationListViewModel,
    settingsViewModel: SettingsViewModel,
    initialIntent: Intent?
) {
    val navController = rememberNavController()

    // Handle incoming intent (notification tap, sms: URI, or ACTION_SEND share)
    LaunchedEffect(initialIntent) {
        if (initialIntent != null) {
            val threadId = initialIntent.getLongExtra(NotificationHelper.EXTRA_THREAD_ID, -1L)
            val addressExtra = initialIntent.getStringExtra(NotificationHelper.EXTRA_ADDRESS)

            if (threadId > 0 && !addressExtra.isNullOrBlank()) {
                val encodedAddress = URLEncoder.encode(addressExtra, "UTF-8")
                navController.navigate("conversation/$threadId/$encodedAddress")
                return@LaunchedEffect
            }

            val action = initialIntent.action
            val data = initialIntent.data
            if (Intent.ACTION_SENDTO == action || Intent.ACTION_VIEW == action) {
                val recipient = data?.schemeSpecificPart?.substringBefore('?')
                if (!recipient.isNullOrBlank()) {
                    val encoded = URLEncoder.encode(recipient, "UTF-8")
                    navController.navigate("conversation/0/$encoded")
                }
            } else if (Intent.ACTION_SEND == action) {
                val text = initialIntent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                val encodedText = URLEncoder.encode(text, "UTF-8")
                navController.navigate("compose?initialBody=$encodedText")
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_CONVERSATIONS,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(280)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(280)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(280)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(280)
            )
        }
    ) {
        // Conversation List Screen
        composable(ROUTE_CONVERSATIONS) {
            ConversationListScreen(
                viewModel = conversationListViewModel,
                onNavigateToConversation = { threadId, address ->
                    val encoded = URLEncoder.encode(address, "UTF-8")
                    navController.navigate("conversation/$threadId/$encoded")
                },
                onNavigateToCompose = {
                    navController.navigate("compose")
                },
                onNavigateToSettings = {
                    navController.navigate(ROUTE_SETTINGS)
                }
            )
        }

        // Conversation Detail Screen
        composable(
            route = ROUTE_CONVERSATION_DETAIL,
            arguments = listOf(
                navArgument("threadId") { type = NavType.LongType },
                navArgument("address") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getLong("threadId") ?: -1L
            val rawAddress = backStackEntry.arguments?.getString("address") ?: ""
            val decodedAddress = try {
                URLDecoder.decode(rawAddress, "UTF-8")
            } catch (e: Exception) {
                rawAddress
            }

            val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SalimApplication
            val detailViewModel = androidx.lifecycle.viewmodel.compose.viewModel<ConversationDetailViewModel>(
                key = "detail_${threadId}_$decodedAddress",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ConversationDetailViewModel(app, threadId, decodedAddress) as T
                    }
                }
            )

            ConversationDetailScreen(
                viewModel = detailViewModel,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { uri ->
                    val encodedUri = URLEncoder.encode(uri.toString(), "UTF-8")
                    navController.navigate("media_viewer?uri=$encodedUri")
                },
                onForwardMessage = { text ->
                    val encodedText = URLEncoder.encode(text, "UTF-8")
                    navController.navigate("compose?initialBody=$encodedText")
                }
            )
        }

        // New Message (Compose) Screen
        composable(
            route = ROUTE_COMPOSE,
            arguments = listOf(
                navArgument("initialBody") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SalimApplication
            val rawBody = backStackEntry.arguments?.getString("initialBody") ?: ""
            val initialBody = try {
                URLDecoder.decode(rawBody, "UTF-8")
            } catch (e: Exception) {
                rawBody
            }

            val composeViewModel = androidx.lifecycle.viewmodel.compose.viewModel<NewMessageViewModel>(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return NewMessageViewModel(app) as T
                    }
                }
            )

            NewMessageScreen(
                viewModel = composeViewModel,
                onCancel = { navController.popBackStack() },
                onMessageSent = { threadId, address ->
                    val encoded = URLEncoder.encode(address, "UTF-8")
                    navController.navigate("conversation/$threadId/$encoded") {
                        popUpTo(ROUTE_CONVERSATIONS)
                    }
                },
                initialBody = initialBody
            )
        }

        // Settings Screen
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Media Viewer Screen
        composable(
            route = ROUTE_MEDIA_VIEWER,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawUri = backStackEntry.arguments?.getString("uri") ?: ""
            val decodedUri = try {
                URLDecoder.decode(rawUri, "UTF-8")
            } catch (e: Exception) {
                rawUri
            }
            val uri = Uri.parse(decodedUri)

            MediaViewerScreen(
                mediaUri = uri,
                onClose = { navController.popBackStack() }
            )
        }
    }
}
