package apps.startup.test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import apps.startup.test.Constants.*
import com.google.android.gms.tasks.TaskExecutors
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference.child(DATABASE_NAME)
    var phone: String = ""
    private var mAuth: FirebaseAuth? = null
    private var mVerificationId = ""
    val user_details= Model_User()
    var startedOnce:Boolean=false

    private val mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(p0: PhoneAuthCredential?) {
        }


        override fun onVerificationFailed(e: FirebaseException) {
            Toaster.makeSnackBarFromActivity(this@MainActivity, e.message.toString())
        }

        override fun onCodeSent(s: String, forceResendingToken: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(s, forceResendingToken)
            //progressBar.setVisibility(View.INVISIBLE)
            mVerificationId = s
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        login.setOnClickListener {
            phone = phone_number.text.toString()
            //Phone number validation
            if (BasicUtils.isValidMobile(phone)) {
                isUserNew(phone)

            } else {
                Toaster.makeSnackBarFromActivity(this, "Please put a proper phone number")
            }
        }
        verify.setOnClickListener {
            val code:String=otp_view.text.toString()
            if(code.length==6)
                verifyVerificationCode(code)
            else
                Toaster.makeSnackBarFromActivity(this, "invalid code")
        }

        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
            &&
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE),
                RESULT_CODE)

        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        Log.e("tagb ", "onRequestPermissionsResult: ")
        if (requestCode == RESULT_CODE) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != 0) {
                    allPermissionsGranted = false
                }
            }

            if (!allPermissionsGranted) {
                finish()
            } else {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //This function check if the user is first time or not
    fun isUserNew(phone: String) {
        var firstTime: Boolean = true;
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (d in dataSnapshot.children) {
                        if (d!=null && d.getValue(Model_User::class.java)!!.phone_no!!.equals(phone)) {
                            Toaster.makeSnackBarFromActivity(this@MainActivity,d.key.toString());
                            //If the user is old
                            firstTime=false
                            if(!startedOnce) {
                                val intent = Intent(this@MainActivity, UploadPhotoActivity::class.java)
                                intent.putExtra(USER_KEY, d.key.toString())
                                startActivity(intent)
                                startedOnce=true
                            }
                            break
                    }
                }
                // if not then otp section appers to authenticate means it is first time
                if(firstTime){
                    sendVerificationCode(phone)
                    opt_layout.visibility = View.VISIBLE;
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toaster.makeSnackBarFromActivity(this@MainActivity, "Some error occured!Please try again")

            }
        })
    }

    //This function send the verifcation code
    private fun sendVerificationCode(mobile: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            "+91$mobile",
            30,
            TimeUnit.SECONDS,
            TaskExecutors.MAIN_THREAD,
            mCallbacks
        )
    }

    //This is used to create the credential and then calls other function to verify the code
    private fun verifyVerificationCode(otp: String) {
        //creating the credential
        if (mVerificationId != "") {
            val credential = PhoneAuthProvider.getCredential(mVerificationId, otp)
            //signing the user
            signInWithPhoneAuthCredential(credential)
        } else {
            Toaster.makeSnackBarFromActivity(this, "No verification Link Sent")
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth = FirebaseAuth.getInstance()
        mAuth?.signInWithCredential(credential)?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toaster.makeSnackBarFromActivity(this@MainActivity, "DONE")
                user_details.phone_no=phone
                user_details.count=0
                user_details.photo_url="null"
                databaseReference.push().setValue(user_details).addOnCompleteListener {
                    if(!startedOnce) {
                        val intent = Intent(this@MainActivity, UploadPhotoActivity::class.java)
                        intent.putExtra(USER_KEY, databaseReference.key.toString())
                        startActivity(intent)
                        startedOnce=true
                    }
                }
            } else {
                var message = "Something wend wrong"
                if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    message = "Invalid code"
                }
                Toaster.makeSnackBarFromActivity(this@MainActivity, message)
            }
        }!!.addOnCanceledListener {
        }
    }


}
