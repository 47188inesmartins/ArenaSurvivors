package fcul.mei.cm.app.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fcul.mei.cm.app.database.AlliancesRepository
import fcul.mei.cm.app.database.CoordinatesRepository
import fcul.mei.cm.app.domain.Coordinates
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.UserSharedPreferences
import kotlinx.coroutines.launch

class ArenaMapViewModel(
     context: Context,
) : ViewModel() {

    private val coordinatesDatabase = CoordinatesRepository()

    private val alliancesDatabase = AlliancesRepository()

    private val userSharedPreferences = UserSharedPreferences(context)

    private val _coordinatesMap = MutableLiveData<Map<User, Coordinates?>>()
    val coordinatesMap: LiveData<Map<User, Coordinates?>> = _coordinatesMap

//    init {
//        observeCoordinates()
//    }
//
//    private fun observeCoordinates() {
//        val userId = userSharedPreferences.getUserId()
//            coordinatesDatabase.getCoordinates(1) { coordinates ->
//                Log.d("DISTRITO",_coordinatesMap.value.toString())
//                if(coordinates.isNotEmpty()){
//                    _coordinatesMap.value = coordinates.filter { it.value != null }
//                    Log.d("DISTRITO",_coordinatesMap.value.toString())
//                }
//        }
//    }

    fun saveCoordinates(coordinates: Coordinates) {
        viewModelScope.launch {
            coordinatesDatabase.saveCoordinates(
                "2",
                coordinates
            )
        }
    }
}