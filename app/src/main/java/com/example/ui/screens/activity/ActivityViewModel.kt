package com.example.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityCategory
import com.example.data.model.UserActivityRecord
import com.example.data.repository.ActivityRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    private val activityRepository: ActivityRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(ActivityCategory.ALL)
    val selectedCategory: StateFlow<ActivityCategory> = _selectedCategory.asStateFlow()

    val currentUserId: StateFlow<String> = authRepository.authState
        .map { (it as? AuthState.Authenticated)?.user?.userId ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUserId ?: "")

    val activities: StateFlow<List<UserActivityRecord>> = combine(
        currentUserId,
        _selectedCategory
    ) { uid, category ->
        Pair(uid, category)
    }.flatMapLatest { (uid, category) ->
        if (uid.isBlank()) flowOf(emptyList())
        else activityRepository.observeActivitiesByCategory(uid, category)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: ActivityCategory) {
        _selectedCategory.value = category
    }

    fun deleteActivity(activityId: String) {
        val uid = currentUserId.value
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                activityRepository.deleteActivity(activityId, uid)
            }
        }
    }

    companion object {
        fun provideFactory(
            activityRepository: ActivityRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ActivityViewModel(activityRepository, authRepository) as T
            }
        }
    }
}
