package com.example.alris.higher_authority

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alris.data.RegisterLowerAuthorityRequest
import com.example.alris.data.RegisterLowerAuthorityResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ApprovalUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val tempPassword: String? = null,
    val errorMessage: String? = null
)

class ApprovalViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ApprovalViewModel(
            ApprovalRepository(context)
        ) as T
    }
}

class ApprovalRepository(context: Context) {

    private val api = ApiClient.createUserApi(context)

    suspend fun registerLowerAuthority(email: String)
            : RegisterLowerAuthorityResponse {
        return api.registerLowerAuthority(
            RegisterLowerAuthorityRequest(email)
        )
    }
}
class ApprovalViewModel(
    private val repository: ApprovalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApprovalUiState())
    val uiState: StateFlow<ApprovalUiState> = _uiState

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun registerLowerAuthority() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val response =
                    repository.registerLowerAuthority(_uiState.value.email)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = response.message,
                    tempPassword = response.tempPassword
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Something went wrong"
                )
            }
        }
    }
}

@Composable
fun ApprovalScreen() {
    val context = LocalContext.current

    val viewModel: ApprovalViewModel = viewModel(
        factory = ApprovalViewModelFactory(context)
    )

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Register Lower Authority",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Lower Authority Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )



        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.registerLowerAuthority() },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Approve & Register")
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.successMessage != null -> {
                Text(uiState.successMessage!!)
                Text("Temporary Password: ${uiState.tempPassword}")
            }
            uiState.errorMessage != null -> {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

