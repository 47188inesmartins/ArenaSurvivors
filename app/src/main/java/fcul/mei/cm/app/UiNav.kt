package fcul.mei.cm.app

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.android.gms.maps.model.LatLng
import fcul.mei.cm.app.screens.AddUserScreen
import fcul.mei.cm.app.screens.Home
import fcul.mei.cm.app.screens.alliances.AlliancesList
import fcul.mei.cm.app.screens.alliances.AlliancesScreen
import fcul.mei.cm.app.screens.alliances.ChatTemplate
import fcul.mei.cm.app.screens.alliances.CreateAllianceTemplate
import fcul.mei.cm.app.screens.alliances.ManageAlliancesScreen
import fcul.mei.cm.app.screens.alliances.ManageJoinRequestsScreen
import fcul.mei.cm.app.screens.sensors.SensorsDisplayUi
import fcul.mei.cm.app.screens.map.ArenaMapUi
import fcul.mei.cm.app.screens.map.SpotsListUi
import fcul.mei.cm.app.utils.Routes
import fcul.mei.cm.app.utils.UserSharedPreferences
import fcul.mei.cm.app.viewmodel.AlliancesViewModel
import fcul.mei.cm.app.viewmodel.ArenaMapViewModel
import fcul.mei.cm.app.viewmodel.FitnessViewModel
import fcul.mei.cm.app.viewmodel.SensorsViewModel
import fcul.mei.cm.app.viewmodel.UserViewModel
import fcul.mei.cm.app.screens.UserInfo
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun UiNav(
    navController: NavHostController,
    sensorManager: SensorManager,
    accelerometer: Sensor?,
    fitnessViewModel: FitnessViewModel,
    sensorsViewModel: SensorsViewModel,
    userViewModel: UserViewModel,
    modifier: Modifier = Modifier

) {
    val chatViewModel = AlliancesViewModel(UserViewModel(LocalContext.current))

    Surface(
        modifier = modifier
            .background(
                color = Color(0xFF90daee))
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = Routes.HOME.name) {
            composable(route = Routes.HOME.name) {
                HomeScreen(
                    navController = navController
                )
            }
            composable(route = Routes.CHAT.name) {
//                ChatScreen(
//                    chatViewModel = chatViewModel
//                )
                val context = LocalContext.current
                val userId = UserSharedPreferences(context = context).getUserId()
                AlliancesList(
                    userId, context,
                    onNavigateToChat = { chatName ->
                        navController.navigate("chat/$chatName")
                    }
                )
            }
            composable(
                "chat/{chatName}",
                arguments = listOf(navArgument("chatName") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatName = backStackEntry.arguments?.getString("chatName")
                if (chatName != null) {
                    ChatScreen(
                        chatViewModel = AlliancesViewModel(userViewModel),
                        modifier = Modifier,
                        chatName = chatName
                    )
                }
            }
            composable(route = Routes.ALLIANCES_LIST.name) {
                AlliancesListScreen(
                    chatViewModel = chatViewModel,
                    navController = navController
                )
            }
            composable(route = Routes.CREATE_ALLIANCE.name) {
                CreateAllianceScreen(
                    navController = navController
                )
            }

            composable(route = Routes.FITNESS.name) {
                FitnessScreen(
                    sensorManager = sensorManager,
                    accelerometer = accelerometer,
                    fitnessViewModel = fitnessViewModel,
                    SensorsViewModel = sensorsViewModel,
                )
            }
            composable(route = Routes.CREATE_USER.name) {
                CreateUserScreen(
                    navController = navController
                )
            }
            composable(Routes.USER_INFO.name) {
                UserInfo()
            }
            composable(
                route = "spots/{latitude}/{longitude}", // Use the full route with placeholders
                arguments = listOf(
                    navArgument("latitude") { type = NavType.StringType },
                    navArgument("longitude") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull()
                val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull()

                if (latitude != null && longitude != null) {
                    SpotsListUi(
                        userLocation = LatLng(latitude, longitude),
                        coordinatesMap = mapOf(
                            Pair(Pair(38.7558055, -9.1525834), "Arena Center"),
                            Pair(Pair(38.7520789, -9.1502785), "Supplies"),
                            Pair(Pair(38.754697, -9.1562628), "Mystery"),
                            Pair(Pair(38.7533382, -9.1564647), "Supplies"),
                            Pair(Pair(38.7560844, -9.1576351), "Supplies")
                        )
                    )
                }
            }
            val viewModel = AlliancesViewModel(userViewModel)
            composable("manageAlliances") {
                val userId = UserSharedPreferences(LocalContext.current).getUserId()

                 // Replace with actual user ID retrieval logic
                if (userId != null) {
                    ManageAlliancesScreen(
                        viewModel = viewModel,
                        userId = userId,
                        onAllianceSelected = { chatName ->
                            navController.navigate("manageAlliance/$chatName")
                        }
                    )
                }
            }
            composable("manageAlliance/{chatName}") { backStackEntry ->
                val chatName = backStackEntry.arguments?.getString("chatName") ?: return@composable
                ManageJoinRequestsScreen(chatName = chatName, viewModel = viewModel) {
                    navController.popBackStack()
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun HomeScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    Scaffold { _ ->
        Column(
            modifier = modifier
                .fillMaxSize()

        ) {
            Home(
                onClickChatButton = { navController.navigate(Routes.CHAT.name) },
                onClickHealthButton = { navController.navigate(Routes.FITNESS.name) },
                onClickAlliancesList = { navController.navigate(Routes.ALLIANCES_LIST.name) },
                onClickUserButton = { navController.navigate(Routes.CREATE_USER.name) },
                onClickUserInfoButton = { navController.navigate(Routes.USER_INFO.name) }
            )
            ArenaMapUi(
                modifier = Modifier.fillMaxSize()
                    ,
                navController = navController,
                ArenaMapViewModel(LocalContext.current)
            )
        }
    }
}
@Composable
private fun ChatScreen(modifier: Modifier = Modifier, chatViewModel: AlliancesViewModel,chatName:String) {
    Column(
        modifier = Modifier.background(
            color = Color(0xFF90daee))
            .fillMaxSize()
            .padding(start = 8.dp, end = 8.dp)
    ) {
        ChatTemplate(
            modifier = modifier,
            viewModel = chatViewModel,
            chatName = chatName
        )
    }
}
@Composable
private fun AlliancesListScreen(
    modifier: Modifier = Modifier,
    chatViewModel: AlliancesViewModel,
    navController: NavHostController
) {
        Column(
            modifier = Modifier.background(
                color = Color(0xFF90daee))
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp)
        ) {
            AlliancesScreen(
                modifier = modifier,
                viewModel = chatViewModel,
                onCreateAllianceClick = { navController.navigate(Routes.CREATE_ALLIANCE.name) },
                onManageAlliancesClick = {
                    navController.navigate("manageAlliances")
                }
            )
    }
}

@Composable
private fun CreateAllianceScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val context = LocalContext.current
        Column(
            modifier = Modifier.background(
                color = Color(0xFF90daee))
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp)
        ) {
            CreateAllianceTemplate(
                modifier = modifier,
                viewModel = AlliancesViewModel(UserViewModel(LocalContext.current)),
                onClickCreateAlliance = { success ->
                    if (success) {
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context,"Creation failed, there might be another Alliance with the same name.",  Toast.LENGTH_LONG).show()
                    }
                }
            )
    }
}

@Composable
private fun FitnessScreen(
    sensorManager: SensorManager,
    accelerometer: Sensor?,
    fitnessViewModel: FitnessViewModel,
    SensorsViewModel: SensorsViewModel
) {
        Column(
            modifier = Modifier.background(
                    color = Color(0xFF90daee)
            )
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp)
        ) {
            SensorsDisplayUi(SensorsViewModel)
        }
}

@Composable
private fun CreateUserScreen(navController: NavHostController) {
        Column(
            modifier = Modifier.background(
                    color = Color(0xFF90daee))
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp)
        ) {
            AddUserScreen(
                userViewModel = UserViewModel(LocalContext.current),
                onDone = { navController.navigate(Routes.HOME.name) }
            )
    }
}
