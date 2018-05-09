package com.elano.logindemo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.twitter.sdk.android.core.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Twitter.initialize(this)
        setContentView(R.layout.activity_main)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        mAuth = FirebaseAuth.getInstance()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        btnTwitterLogin.callback = object : Callback<TwitterSession>() {
            override fun success(result: Result<TwitterSession>?) {
                Log.d(TAG, "Login success.")
                handleTwitterSession(result?.data)
            }

            override fun failure(exception: TwitterException?) {
                Log.d(TAG, "Login failure.", exception)
                updateUI(null, null)
            }
        }
        btnGoogleSignIn.setOnClickListener {
            when (it.id) {
                R.id.btnGoogleSignIn -> signIn()
            }
        }
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient?.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
        else
            btnTwitterLogin.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleTwitterSession(session: TwitterSession?) {
        Log.d(TAG, "handleTwitterSession: $session")
        val credential =  TwitterAuthProvider.getCredential(session!!.authToken.token, session.authToken.secret)

        mAuth?.signInWithCredential(credential)?.addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Log.d(TAG, "signInWithCredential: success")
                val user = mAuth?.currentUser
                updateUI(user, null)
                toast("Authentication succeeded.")
            }
            else {
                Log.d(TAG, "signInWithCredential: failure", it.exception)
                toast("Authentication failed.")
            }
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.d(TAG, "signInResult: failed code = ${e.statusCode}")
            updateUI(null, null)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        Log.d(TAG, "firebaseAuthWithGoogle: ${account?.id}")
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        mAuth?.signInWithCredential(credential)?.addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Log.d(TAG, "signInWithCredential: success")
                val user = mAuth?.currentUser
                updateUI(user, null)
            }
            else {
                Log.d(TAG, "signInWithCredential: failure", it.exception)
                Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                updateUI(null, null)
            }
        }
    }

    private fun updateUI(user: FirebaseUser?, account: GoogleSignInAccount?) {}

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val RC_SIGN_IN = 100
    }
}
