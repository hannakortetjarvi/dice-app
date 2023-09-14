package com.example.gameapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var userButton : Button
    private lateinit var registerButton : Button
    private lateinit var anonButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get auth instance for authenticating into firebase
        auth = FirebaseAuth.getInstance()

        // Initialize sign in button
        userButton = findViewById(R.id.userButton)
        userButton.setBackgroundColor(Color.rgb(222, 147, 159))
        userButton.setOnClickListener { userLogin() }

        // Initialize register button
        registerButton = findViewById(R.id.registerButton)
        registerButton.setBackgroundColor(Color.rgb(222, 147, 159))
        registerButton.setOnClickListener { createAccount() }

        // Initialize anonymous login button
        anonButton = findViewById(R.id.anonButton)
        anonButton.setBackgroundColor(Color.rgb(222, 147, 159))
        anonButton.setOnClickListener { anonLogin() }
    }

    public override fun onStart() {
        super.onStart()

        // Check if the user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }

    /**
     * Change intent if user is logged in
     */
    private fun updateUI(user: FirebaseUser?) {
        val intent = Intent(this, DiceActivity::class.java)
        intent.putExtra("userId", user?.uid)
        startActivity(intent)
        finish()
    }

    /**
     * Sign in the current user into firebase using email + password login
     */
    private fun userLogin() {
        if (findViewById<TextView>(R.id.editTextEmailAddress).text.toString() == "") {
            Toast.makeText(baseContext, "Email required", Toast.LENGTH_SHORT).show()
        }
        else if (findViewById<TextView>(R.id.editTextPassword).text.toString() == "") {
            Toast.makeText(baseContext, "Password required", Toast.LENGTH_SHORT).show()
        }
        else {
            auth.signInWithEmailAndPassword(findViewById<TextView>(R.id.editTextEmailAddress).text.toString(), findViewById<TextView>(R.id.editTextPassword).text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Check if email has been verified
                        checkIfEmailVerified()
                    } else {
                        Toast.makeText(baseContext, "Sign in failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    /**
     * Check if user has verified their email
     */
    private fun checkIfEmailVerified() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            if (user.isEmailVerified) {
                val currentUser = auth.currentUser
                updateUI(currentUser)
            } else {
                auth.signOut()
                Toast.makeText(baseContext, "Email not verified.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sign in the user as an anonymous account
     */
    private fun anonLogin() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Toast.makeText(baseContext, "Sign in failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Register a new user into firebase
     */
    private fun createAccount() {
        if (findViewById<TextView>(R.id.editTextEmailAddress).text.toString() == "") {
            Toast.makeText(baseContext, "Email required", Toast.LENGTH_SHORT).show()
        }
        else if (findViewById<TextView>(R.id.editTextPassword).text.toString() == "") {
            Toast.makeText(baseContext, "Password required", Toast.LENGTH_SHORT).show()
        }

        else {
            auth.createUserWithEmailAndPassword(findViewById<TextView>(R.id.editTextEmailAddress).text.toString(), findViewById<TextView>(R.id.editTextPassword).text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Send a verification email to the used email address, user has to verify it to be able to sign in
                        sendVerificationEmail()
                    } else {
                        Toast.makeText(baseContext, "Account creation failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    /**
     * Send verification email to the user's email address
     */
    private fun sendVerificationEmail() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(baseContext, "Verification email sent.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(baseContext, "Error sending verification email.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}