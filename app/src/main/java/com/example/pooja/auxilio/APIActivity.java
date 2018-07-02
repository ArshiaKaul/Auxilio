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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class APIActivity extends AppCompatActivity {

    private Button tapCameraBtn;
    private ProgressBar progressBar ;
    private ImageView checkSign;
    private TextView goToHomescreen;

    private static final int CAMERA_REQUEST_CODE = 1;

    private StorageReference storageReference;

    MediaPlayer mediaPlayer = null;

    SharedPreferences spForUploadCounter;

    //permission variables
    private String [] permissions = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //another request
    public static final int RequestPermissionCode = 1;

    String uniquefilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.taptoopencamera);
        mediaPlayer.start();

        //initializing spForUploadCounter sharedpreference with key_name = uploadCounter
        spForUploadCounter = getSharedPreferences("uploadCounter", 0);


        //referencing the storage directory
        tapCameraBtn = (Button) findViewById(R.id.tapCameraBtn);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        checkSign = (ImageView) findViewById(R.id.check_sign);
        goToHomescreen = (TextView) findViewById(R.id.goto_homescreen);

        //setting visibilities
        checkSign.setVisibility(View.GONE);
        goToHomescreen.setVisibility(View.GONE);

        storageReference = FirebaseStorage.getInstance().getReference();


        tapCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mediaPlayer.stop();

                if(!checkPermission()){
                    requestPermission();
                }else{
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, CAMERA_REQUEST_CODE);
                }
            }
        });

        checkSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                Intent intent = new Intent(APIActivity.this, GestureActivity.class);
                startActivity(intent);
                finish();
            }
        });

        goToHomescreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                Intent intent = new Intent(APIActivity.this, GestureActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data!= null){
            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.waitwhilepicisuploaded);
            mediaPlayer.start();

            final Bitmap photo = (Bitmap) data.getExtras().get("data");
            //the tap to open camera button disappears
            tapCameraBtn.setVisibility(Button.GONE);

            //setting the color of progress bar to white
            progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN );

            //and now we make the progress bar visible instead of the button
            progressBar.setVisibility(ProgressBar.VISIBLE);

            uploadPhoto(photo);
        }

    }

    public boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    public void uploadPhoto(Bitmap photo){

        final Uri uri = getImageUri(getApplicationContext(), photo);
        final String userPhoneNumber = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        uniquefilename = userPhoneNumber.toString();

        uploadPhotoToFirebase(uri, userPhoneNumber, uniquefilename);
    }

    public void uploadPhotoToFirebase(Uri uri, final String userPhoneNumber, String uniquefilename){

        final StorageReference filepath = storageReference.child("/" + uniquefilename + "/photos/" + "test_photo");

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                mediaPlayer.stop();
                mediaPlayer.release();

                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        Toast.makeText(getApplicationContext(), uri.toString(), Toast.LENGTH_SHORT).show();
                        recognizePhotoKairos(uri, userPhoneNumber);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        Toast.makeText(getApplicationContext(), "URI not found", Toast.LENGTH_SHORT).show();
                    }
                });

                Toast.makeText(APIActivity.this, "Uploading finished!", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void recognizePhotoKairos(Uri uri, String userPhoneNumber){

        KairosListener listener = createKairosListener();

        try {
            Kairos myKairos = new Kairos();
            // set authentication
            String app_id = "b44ea952";
            String api_key = "9d5cec3afba947522606cbfa90defd5c";
            myKairos.setAuthentication(getApplicationContext(), app_id, api_key);
            String image = uri.toString();
            String galleryId = userPhoneNumber;
            // reognize person
            myKairos.recognize(image, galleryId, null, null, null, null, listener);

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
                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                checkSign.setVisibility(View.VISIBLE);
                goToHomescreen.setVisibility(View.VISIBLE);
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.taptogotohomescreen);
                mediaPlayer.start();

                boolean flag = isJSONValid(response);
                if(flag)
                    Log.d("JSON", "VALID JSON");
                else
                    Log.d("JSON", "INVALID JSON");

                try{
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray images = jsonObject.getJSONArray("images");
                    JSONObject zero = images.getJSONObject(Integer.parseInt("0"));
                    JSONArray candidates = zero.getJSONArray("candidates");
                    JSONObject candidate_zero = candidates.getJSONObject(Integer.parseInt("0"));
                    String name = candidate_zero.getString("subject_id");
                    Double confidence = candidate_zero.getDouble("confidence");

                    Toast.makeText(APIActivity.this, name + " " + confidence, Toast.LENGTH_SHORT).show();
                    String photoNum = name.split("_")[1];

                    storageReference = FirebaseStorage.getInstance().getReference()
                            .child(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber().toString()).child("audio").child("audio_" + photoNum);

                    Toast.makeText(APIActivity.this, "audio_" + photoNum + ".3gpp", Toast.LENGTH_SHORT).show();

                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri audioUri) {
                            try{
                                mediaPlayer.setDataSource(getApplicationContext(), audioUri);
                                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {
                                        mp.start();
                                    }
                                });
                            }catch(IOException e){
                                Toast.makeText(APIActivity.this, "Cannot play audio", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(APIActivity.this, "Cannot find audio", Toast.LENGTH_SHORT).show();
                        }
                    });

                    if(confidence >= 0.6){
                        Toast.makeText(APIActivity.this, name + " " + confidence, Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(APIActivity.this, "No match found", Toast.LENGTH_SHORT).show();
                    }

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(String response) {
                // your code here!
                Log.d("ERROR KAIROS", response);
                Toast.makeText(getApplicationContext(), "Kairos Upload ERROR", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                checkSign.setVisibility(View.VISIBLE);
                goToHomescreen.setVisibility(View.VISIBLE);
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.taptogotohomescreen);
                mediaPlayer.start();
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
        ActivityCompat.requestPermissions(APIActivity.this, new
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
        mediaPlayer.stop();
        mediaPlayer.release();
    }

}

