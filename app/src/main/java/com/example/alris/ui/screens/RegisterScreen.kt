package com.example.alris.ui.screens

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.alris.data.RegisterRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterScreen()
        }
    }
}
@Composable
fun RegisterScreen() {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })

        Spacer(modifier = Modifier.height(16.dp))



                Button(onClick = {
                    val request = RegisterRequest(name, email, password, phone.ifEmpty { null })
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val api = ApiClient.createUserApi(context)
                            Log.d("DEBUG_API", "Calling registerUser()...")

                            val response = api.registerUser(request)

                            Log.d("DEBUG_API", "Request sent! Raw response = ${response.raw()}")
                            Log.d("DEBUG_API", "Response code = ${response.code()}")
                            Log.d("DEBUG_API", "Response message = ${response.message()}")
                            Log.d("DEBUG_API", "Success? = ${response.isSuccessful}")
                            if (response.isSuccessful) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, "Registered successfully!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text("Register")
                }

    }
}

