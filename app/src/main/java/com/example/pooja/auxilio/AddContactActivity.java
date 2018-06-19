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
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static java.security.AccessController.getContext;

public class AddContactActivity extends AppCompatActivity {

    private Button tapCameraBtn;
    private ProgressBar progressBar ;
    private static final int CAMERA_REQUEST_CODE = 1;

    private StorageReference storageReference;

    private MediaRecorder mRecorder;
    private  String mFileName = null;

    //permission variables
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String [] permissions = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};


    //camera request (used in dispatchPictureIntent)
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    //another request
    public static final int RequestPermissionCode = 1;


    String uniquefilename;

    private static final String LOG_TAG = "Record_log";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //referencing the storage directory
        tapCameraBtn = (Button) findViewById(R.id.tapCameraBtn);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        storageReference = FirebaseStorage.getInstance().getReference();


        tapCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            //the tap to open camera button disappears
            tapCameraBtn.setVisibility(Button.GONE);

            //setting the color of progress bar to white
            progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN );

            //and now we make the progress bar visible instead of the button
            progressBar.setVisibility(ProgressBar.VISIBLE);

            //Uri uri = data.getData();
            Uri uri = getImageUri(getApplicationContext(), photo);
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            uniquefilename = userId.toString();

            //creating another storage reference for filepath
            StorageReference filepath = storageReference.child("/" + uniquefilename + "/photos/" + "photo" + userId);

            //uploading image captured to firebase
            filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Toast.makeText(AddContactActivity.this, "Uploading finished!", Toast.LENGTH_LONG).show();
                    storeAudioToFirebase();
                }
            });


        }

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void storeAudioToFirebase(){


        progressBar.setVisibility(ProgressBar.GONE);
        tapCameraBtn.setVisibility(Button.VISIBLE);
        tapCameraBtn.setEnabled(true);
        tapCameraBtn.setText("Tap To Record Audio");

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/recorded_audio.mp3";

        tapCameraBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    startRecording();
                    Toast.makeText(AddContactActivity.this, "Recording started!", Toast.LENGTH_SHORT).show();
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    Toast.makeText(AddContactActivity.this, "Recording stopped!", Toast.LENGTH_SHORT).show();
                    stopRecording();
                }
                else{

                    storeAudioToFirebase();
                }
                return false;
            }
        });


    }


    private void startRecording() {

        mRecorder = new MediaRecorder();

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        mRecorder.setOutputFile(mFileName);

        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();

        tapCameraBtn.setVisibility(Button.VISIBLE);
        tapCameraBtn.setEnabled(false);
        tapCameraBtn.setText("");

        //the tap to open camera button disappears
        tapCameraBtn.setVisibility(Button.GONE);

        //and now we make the progress bar visible instead of the button
        progressBar.setVisibility(ProgressBar.VISIBLE);

        mRecorder.release();
        mRecorder = null;
        Toast.makeText(this, "Uploading Audio...", Toast.LENGTH_SHORT).show();

        uploadAudio();
    }

    private void uploadAudio(){

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        uniquefilename = userId.toString();
        StorageReference filepath = storageReference.child( "/" + uniquefilename + "/audios/" + "audio" + userId);

        Uri uri = Uri.fromFile(new File(mFileName));
        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                Toast.makeText(AddContactActivity.this, "Audio Uploaded!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(AddContactActivity.this, GestureActivity.class);
                startActivity(intent);
                finish();


            }
        });

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
}
