package com.example.duviservicios

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Model.UserModel
import com.example.duviservicios.Remote.ICloudFunctions
import com.example.duviservicios.Remote.RetrofitCloudClient
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class MainActivity : AppCompatActivity() {


    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var dialog: android.app.AlertDialog
    private val compositeDisposable = CompositeDisposable()
    private lateinit var cloudFunction: ICloudFunctions

    private var providers:List<AuthUI.IdpConfig>? =null

    private lateinit var userRef: DatabaseReference

    companion object {
        private val APP_REQUEST_CODE = 7174
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {

        providers = Arrays.asList<AuthUI.IdpConfig>(AuthUI.IdpConfig.PhoneBuilder().build())

        userRef = FirebaseDatabase.getInstance().getReference(Commun.USER_REFERENCE)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        cloudFunction = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)
        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {

                checkUserFromFirebase(user!!)

            } else {

                phoneLogin()
            }
        }

        //NO BORRAR

        Dexter.withActivity(this@MainActivity)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener{
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                    val user = firebaseAuth!!.currentUser
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(this@MainActivity,"Debes aceptar este permiso para usar la aplicación",Toast.LENGTH_SHORT).show()
                }

            }).check()
    }


        private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog.show()
        userRef.child(user!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@MainActivity, "" + p0!!.message, Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        val userModel = p0.getValue(UserModel::class.java)
                        goToHomeActivity(userModel)
                    } else {
                        showRegisterDialog(user!!)
                    }
                    dialog.dismiss()
                }

            })
    }

    private fun showRegisterDialog(user:FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Registrar")
        builder.setMessage("Por favor ingrese toda la información")

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)
        val edt_name = itemView.findViewById<View>(R.id.edt_name) as TextView
        val edt_phone = itemView.findViewById<View>(R.id.edt_phone) as TextView

        edt_phone.setText(user!!.phoneNumber)

        builder.setNegativeButton("CANCELAR", { dialogInterface, _ -> dialogInterface.dismiss() })
            .setPositiveButton("REGISTRAR", { _, _ ->
                if (TextUtils.isEmpty(edt_name.text)) {
                    Toast.makeText(this@MainActivity, "Por favor ingrese su nombre", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val serverUserModel = UserModel()
                serverUserModel.uid = user.uid
                serverUserModel.name = edt_name.text.toString()
                serverUserModel.phone = edt_phone.text.toString()
                serverUserModel.address = "Sin especificar"

                dialog!!.show()
                userRef!!.child(user!!.uid)
                    .setValue(serverUserModel)
                    .addOnFailureListener { e ->
                        dialog!!.dismiss()
                        Toast.makeText(this@MainActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener { _ ->
                        dialog!!.dismiss()
                        Toast.makeText(this@MainActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        goToHomeActivity(serverUserModel)
                    }
            })

        builder.setView(itemView)

        val registerDialog = builder.create()
        registerDialog.show()
    }

    private fun goToHomeActivity(userModel: UserModel?) {

        FirebaseInstanceId.getInstance()
            .instanceId
            .addOnFailureListener{ e-> Toast.makeText(this@MainActivity,""+e.message,Toast.LENGTH_SHORT).show()
                Commun.currentUser = userModel!!
                startActivity(Intent(this@MainActivity,HomeActivity::class.java))
                finish()
            }
            .addOnCompleteListener { task->
                if(task.isSuccessful)
                {
                    Commun.currentUser = userModel!!
                    Commun.updateToken(this@MainActivity,task.result!!.token)
                    startActivity(Intent (this@MainActivity,HomeActivity::class.java))
                    finish()
                }
            }


    }

    private fun phoneLogin() {

        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers!!).build(), APP_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE)
        {
            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK)
            {
                val user = FirebaseAuth.getInstance().currentUser
            }
            else
            {
                Toast.makeText(this,"Fallo al iniciar sesión",Toast.LENGTH_SHORT).show()
            }
        }
    }


}