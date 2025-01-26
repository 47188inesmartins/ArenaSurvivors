package fcul.mei.cm.app.screens.health

import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PermissionsViewModel(val healthConnectManager: HealthConnectManager) : ViewModel() {
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    suspend fun checkPermissions(permissions: Set<String>) {
        val granted = healthConnectManager.hasAllPermissions(permissions)
        Log.d("PermissionsViewModel", "Permissions check result: $granted")
        _permissionsGranted.value = granted
    }

    fun requestPermissions(
        permissions: Set<String>,
        requestPermissionsLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>
    ) {
        Log.d("PermissionsViewModel", "Requesting permissions: $permissions")
        requestPermissionsLauncher.launch(permissions)
    }
}