package com.example.pooja.auxilio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.TaskApiCall;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static java.security.AccessController.getContext;

import com.kairos.*;

import org.json.JSONException;

public class AddContactActivity extends AppCompatActivity {

    private Button tapCameraBtn;
    private ProgressBar progressBar ;
    private static final int CAMERA_REQUEST_CODE = 1;

    private StorageReference storageReference;

    private DatabaseReference mCount;
    public int photoCounter;

    private Kairos myKairos = new Kairos();

    //MediaPlayer mediaPlayer = null;


    SharedPreferences spForUploadCounter;

    //permission variables
    private String [] permissions = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //another request
    public static final int RequestPermissionCode = 1;

    String uniquefilename;

    private static final String LOG_TAG = "Record_log";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

       // mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.taptoopencamera);
       // mediaPlayer.start();

        //initializing spForUploadCounter sharedpreference with key_name = uploadCounter
        spForUploadCounter = getSharedPreferences("uploadCounter", 0);

        //referencing the storage directory
        tapCameraBtn = (Button) findViewById(R.id.tapCameraBtn);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        storageReference = FirebaseStorage.getInstance().getReference();


        tapCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

              //  mediaPlayer.stop();

                if(!checkPermission()){
                    requestPermission();
                }else{
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, CAMERA_REQUEST_CODE);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data!= null){
           // mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.waitwhilepicisuploaded);
           // mediaPlayer.start();

            final Bitmap photo = (Bitmap) data.getExtras().get("data");
            //the tap to open camera button disappears
            tapCameraBtn.setVisibility(Button.GONE);

            //setting the color of progress bar to white
            progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN );

            //and now we make the progress bar visible instead of the button
            progressBar.setVisibility(ProgressBar.VISIBLE);

            mCount = FirebaseDatabase.getInstance().getReference().child(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber().toString()).child("count");

            uploadPhoto(mCount, photo);
        }

    }

    public void uploadPhoto(DatabaseReference mCount, Bitmap photo){

        final Uri uri = getImageUri(getApplicationContext(), photo);
        final String userPhoneNumber = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        uniquefilename = userPhoneNumber.toString();

        mCount.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                photoCounter = dataSnapshot.getValue(Integer.class);

                //uploading image captured to firebase
                uploadPhotoToFirebase(uri, userPhoneNumber, uniquefilename, photoCounter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("The read failed: ", "FAILED");
            }
        });
    }

    public void uploadPhotoToFirebase(Uri uri, final String userPhoneNumber, String uniquefilename, int photoCounter){

        final StorageReference filepath = storageReference.child("/" + uniquefilename + "/photos/" + "photo_" + photoCounter);

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                //mediaPlayer.stop();
               // mediaPlayer.release();

                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        Toast.makeText(getApplicationContext(), uri.toString(), Toast.LENGTH_SHORT).show();
                        uploadPhotoToKairos(uri,userPhoneNumber);

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        Toast.makeText(getApplicationContext(), "URI not found", Toast.LENGTH_SHORT).show();
                    }
                });

                Toast.makeText(AddContactActivity.this, "Uploading finished!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(AddContactActivity.this, RecordAudioActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void uploadPhotoToKairos(Uri uri, String userPhoneNumber){
        // Create an instance of the KairosListener
        KairosListener listener = createKairosListener();

        try {
            Kairos myKairos = new Kairos();
            // set authentication
            String app_id = "b44ea952";
            String api_key = "9d5cec3afba947522606cbfa90defd5c";
            myKairos.setAuthentication(getApplicationContext(), app_id, api_key);
            String image = uri.toString();
            String subjectId = "photo_" + photoCounter;
            String galleryId = userPhoneNumber;
            myKairos.enroll(image, subjectId, galleryId, null, null, null, listener);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public KairosListener createKairosListener(){
        return new KairosListener() {

            @Override
            public void onSuccess(String response) {
                // your code here!
                Log.d("SUCCESSFUL KAIROS", response);
                Toast.makeText(getApplicationContext(), "Uploaded to Kairos", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(String response) {
                // your code here!
                Log.d("ERROR KAIROS", response);
                Toast.makeText(getApplicationContext(), "Kairos Upload ERROR", Toast.LENGTH_SHORT).show();
            }
        };
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(AddContactActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO, CAMERA}, RequestPermissionCode);
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
       // mediaPlayer.stop();
       // mediaPlayer.release();
    }


    @Override
    public void onStop()
    {
        super.onStop();
       /* try{
           /* if(mediaPlayer !=null && mediaPlayer.isPlaying()){
                Log.d("TAG------->", "player is running");
                mediaPlayer.stop();
                Log.d("Tag------->", "player is stopped");
                mediaPlayer.release();
                Log.d("TAG------->", "player is released");
            }
        }catch(IllegalStateException e){
            Log.d("IllegalStateException" , "it occurs");
        }*/
    }

}
