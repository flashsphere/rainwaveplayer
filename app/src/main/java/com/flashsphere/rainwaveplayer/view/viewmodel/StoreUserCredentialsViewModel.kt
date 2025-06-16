package com.flashsphere.rainwaveplayer.view.viewmodel

import androidx.lifecycle.ViewModel
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.UserCredentials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StoreUserCredentialsViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _userCredentialsSaved = MutableStateFlow<Boolean?>(null)
    val userCredentialsSaved = _userCredentialsSaved.asStateFlow()

    fun saveUserCredentials(userId: Int?, apiKey: String?) {
        if (userId == null || apiKey.isNullOrEmpty()) {
            _userCredentialsSaved.value = false
            return
        }

        val currentUserCredentials = userRepository.getCredentials()
        val newUserCredentials = UserCredentials(userId, apiKey)

        if (currentUserCredentials != newUserCredentials) {
            userRepository.login(newUserCredentials)
        }

        _userCredentialsSaved.value = true
    }
}
