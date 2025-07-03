package chaos.alice.pro.di

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.local.SettingsRepository
import chaos.alice.pro.data.network.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject

// Состояния остаются те же
sealed interface LicenseState {
    object Loading : LicenseState
    object Valid : LicenseState
    object NetworkError : LicenseState
    data class Invalid(val deviceId: String) : LicenseState
}

private const val CHECK_INTERVAL = 3 // Проверяем каждый 3-й раз

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _licenseState = MutableStateFlow<LicenseState>(LicenseState.Loading)
    val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

    fun checkLicense() {
        viewModelScope.launch {
            _licenseState.value = LicenseState.Loading
            val deviceId = licenseRepository.getDeviceId()
            Log.d("LicenseCheck", "--- Starting STRICT License Check for device: $deviceId ---")

            // Проверяем локальный флаг. Если уже помечен как пират, сразу блокируем.
            val isAlreadyMarkedAsPirate = settingsRepository.isMarkedAsPirate.first()
            if (isAlreadyMarkedAsPirate) {
                _licenseState.value = LicenseState.Invalid(deviceId) // <- Блокировка
                return@launch
            }

            // Проверяем, нужно ли делать сетевой запрос
            val launches = settingsRepository.launchesSinceLastCheck.first()
            val needsCheck = launches % CHECK_INTERVAL == 0
            Log.d("LicenseCheck", "Launches: $launches. Needs network check: $needsCheck")

            if (!needsCheck) {
                settingsRepository.incrementLaunchesSinceLastCheck()
                Log.i("LicenseCheck", "Network check skipped. Assuming VALID for this session.")
                _licenseState.value = LicenseState.Valid
                return@launch
            }

            // Если проверка нужна, выполняем сетевой запрос
            try {
                Log.d("LicenseCheck", "Performing network check for license file...")
                val response = licenseRepository.getLicenseResponse(deviceId)
                Log.d("LicenseCheck", "Server response code: ${response.status.value}")

                if (response.status.value == 200) {
                    // УСПЕХ: Файл найден, пользователь валидный
                    Log.i("LicenseCheck", "License file found (200 OK). User is VALID.")
                    // Сбрасываем счетчик для нового цикла проверок
                    settingsRepository.resetLaunchesSinceLastCheck()
                    settingsRepository.incrementLaunchesSinceLastCheck()
                    _licenseState.value = LicenseState.Valid
                } else {
                    // ПРОВАЛ: Файл не найден (404) или другая ошибка сервера
                    Log.e("LicenseCheck", "License file NOT found (Code: ${response.status.value}). User is INVALID.")
                    settingsRepository.markAsPirate() // Помечаем как пирата
                    _licenseState.value = LicenseState.Invalid(deviceId)
                }

            } catch (e: Exception) {
                // Если нет интернета, показываем ошибку сети
                if (e is UnknownHostException || e is ConnectException) {
                    Log.e("LicenseCheck", "NETWORK ERROR during check.", e)
                    _licenseState.value = LicenseState.NetworkError
                } else {
                    // Любая другая неожиданная ошибка.
                    // Чтобы быть строгими, считаем это тоже провалом проверки.
                    Log.e("LicenseCheck", "UNEXPECTED ERROR during check. Assuming INVALID.", e)
                    _licenseState.value = LicenseState.Invalid(deviceId)
                }
            }
        }
    }
}