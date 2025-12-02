package com.example.alris

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

object Constants {
    const val SUPABASE_URL = "https://sjjkllslqadxmmudkpqt.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNqamtsbHNscWFkeG1tdWRrcHF0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTE3ODQ0NTAsImV4cCI6MjA2NzM2MDQ1MH0.RkJDmXPVMj2wcZ4_YyNmlwc7uTddWGwR5_UjbQnAyJ8"
    const val REDIRECT_URI = "https://sjjkllslqadxmmudkpqt.supabase.co/auth/v1/callback"
    var selectedDepartment: String = ""

    // Make sure you have this somewhere globally accessible (e.g., Constants.kt)
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("379087277976-35hnmmq1bdv3oipd61e2rtm5ev30c36n.apps.googleusercontent.com")
        .requestEmail()
        .build()


    fun logoutAndGoToLogin(context: Context) {
        FirebaseAuth.getInstance().signOut()
        val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

}
