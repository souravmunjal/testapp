package apps.startup.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import apps.startup.test.Constants.*
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_upload_photo.*
import android.widget.Toast
import android.app.Activity
import android.app.ProgressDialog
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.camerakit.CameraKitView
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import id.zelory.compressor.Compressor
import java.io.File
import java.io.FileOutputStream


class UploadPhotoActivity : AppCompatActivity() {
    val databaseReference:DatabaseReference= FirebaseDatabase.getInstance().reference;
    var userdetails=Model_User()
    var key:String=""
    lateinit var cameraKitView : CameraKitView
    var increasedOnce:Boolean=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_photo)
        key = intent.getStringExtra(USER_KEY)

        cameraKitView= findViewById(R.id.camera);
        cameraKitView.requestPermissions(this)

        //get the user details with the key
        getUserDetails()

        upload_image.setOnClickListener {
            cameraKitView.captureImage(object :CameraKitView.ImageCallback{
                override fun onImage(p0: CameraKitView?, p1: ByteArray?) {
                    var savedPhoto= File(Environment.getExternalStorageDirectory(),"image.jpg")
                    var outputStream= FileOutputStream(savedPhoto.path)
                    outputStream.write(p1)
                    outputStream.close()
                    //The image is compressed
                    Compressor(baseContext).compressToBitmap(savedPhoto)
                    uploadPhoto( FileProvider.getUriForFile(baseContext, "apps.startup.test", savedPhoto).toString())
                }

            })
        }

        photourl.setOnClickListener {
            val url = photourl.text.toString()
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }


    }

    private fun getUserDetails() {
        databaseReference.child(DATABASE_NAME).child(key).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(!increasedOnce) {
                    userdetails = dataSnapshot.getValue(Model_User::class.java)!!
                    //When the data is retrieved then the details are loaded into attributes
                    increaseTheCount()
                    Toaster.makeSnackBarFromActivity(this@UploadPhotoActivity, userdetails.phone_no)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }

        })
    }

    private fun increaseTheCount() {
        //Increase the count number
        if(!increasedOnce) {
            increasedOnce=true
            userdetails.count = userdetails.count + 1
            databaseReference.child(DATABASE_NAME).child(key).setValue(userdetails)
        }
        displayDetails()
    }

    private fun displayDetails(){
        counter.setText(""+userdetails.count)
        photourl.setText(""+userdetails.photo_url)
    }



    private fun uploadPhoto(uri:String) {
        progressBar.visibility= View.VISIBLE;


        val storageReference: StorageReference =  FirebaseStorage.getInstance().getReference().child(DATABASE_NAME).child(key)
        val temp = storageReference.child("image")
        val uploadTask:UploadTask = temp.putFile(uri.toUri())
        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                throw task.getException()!!
                }
            // Continue with the task to get the download URL
            temp.getDownloadUrl()
        }).addOnCompleteListener(OnCompleteListener<Uri> { task ->
            if (task.isSuccessful) {
                userdetails.photo_url=task.result!!.toString()
                databaseReference.child(DATABASE_NAME).child(key).setValue(userdetails)
                //After updating the details we have to update the details
                displayDetails()
                Toaster.makeSnackBarFromActivity(this,"Uploaded")
            }
            else {
            }
        })
    }


    override fun onStart() {
        super.onStart()
        cameraKitView.onStart()
    }

    override fun onResume() {
        super.onResume()
        cameraKitView.onResume()
    }

    override fun onPause() {
        cameraKitView.onPause()
        super.onPause()
    }

    override fun onStop() {
        cameraKitView.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
