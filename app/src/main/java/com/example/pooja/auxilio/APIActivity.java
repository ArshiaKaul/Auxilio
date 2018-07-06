package com.example.pooja.auxilio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.example.pooja.auxilio.AuthActivity.KEY_IP;
import static com.example.pooja.auxilio.AuthActivity.ip_address_entered;
import static com.example.pooja.auxilio.AuthActivity.sp;
import static java.security.AccessController.getContext;

import com.kairos.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class APIActivity extends AppCompatActivity {

    private Button tapCameraBtn;
    private ProgressBar progressBar;
    private ImageView checkSign;
    private TextView goToHomescreen;
    private TextView deleteButton;

    private String app_id = "XXXXXXX"; //app id
    private String api_key = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";    //api key

    //private static final int CAMERA_REQUEST_CODE = 1;

    private StorageReference storageReference;

    //private DatabaseReference mCount;
    //public int audioCounter;

    MediaPlayer mediaPlayer = null;

    SharedPreferences spForUploadCounter;

    //permission variables
    private String[] permissions = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //another request
    public static final int RequestPermissionCode = 1;

    String uniquefilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.taponscreen);
        mediaPlayer.start();

        //initializing spForUploadCounter sharedpreference with key_name = uploadCounter
        spForUploadCounter = getSharedPreferences("uploadCounter", 0);


        //referencing the storage directory
        tapCameraBtn = (Button) findViewById(R.id.tapCameraBtn);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        checkSign = (ImageView) findViewById(R.id.check_sign);
        goToHomescreen = (TextView) findViewById(R.id.goto_homescreen);
        deleteButton = (TextView) findViewById(R.id.delete_button);

        //setting visibilities
        progressBar.setVisibility(View.GONE);
        checkSign.setVisibility(View.GONE);
        goToHomescreen.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);

        storageReference = FirebaseStorage.getInstance().getReference();

        if(!checkPermission())
            requestPermission();

        tapCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                }

                if (sp.contains(KEY_IP)) {
                    ip_address_entered = sp.getString(KEY_IP, "");
                }

                String finalIPReceived = "http://" + ip_address_entered  + "/photoaf.jpg";
                new LoadImage().execute(finalIPReceived);
                Log.d("Final IP" , finalIPReceived);

            }
        });

        checkSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                Intent intent = new Intent(APIActivity.this, GestureActivity.class);
                startActivity(intent);
                finish();
            }
        });

        goToHomescreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                Intent intent = new Intent(APIActivity.this, GestureActivity.class);
                startActivity(intent);
                finish();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }

                deleteGalleryKairos();
                //deleteGalleryFirebase();

                Intent intent = new Intent(APIActivity.this, GestureActivity.class);
                startActivity(intent);
                finish();
            }
        });

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

    public void uploadPhoto(Bitmap photo) {

        final Uri uri = getImageUri(getApplicationContext(), photo);
        final String userPhoneNumber = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        uniquefilename = userPhoneNumber.toString();

        uploadPhotoToFirebase(uri, userPhoneNumber, uniquefilename);
    }

    public void uploadPhotoToFirebase(Uri uri, final String userPhoneNumber, String uniquefilename) {

        final StorageReference filepath = storageReference.child("/" + uniquefilename + "/photos/" + "test_photo");

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

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

                Toast.makeText(APIActivity.this, "test_photo uploaded", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void recognizePhotoKairos(Uri uri, String userPhoneNumber) {

        KairosListener listener = createKairosListener();

        try {
            Kairos myKairos = new Kairos();
            // set authentication
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

    public KairosListener createKairosListener() {
        return new KairosListener() {

            @Override
            public void onSuccess(String response) {
                // your code here!
                Log.d("SUCCESSFUL KAIROS", response);
                Toast.makeText(getApplicationContext(), "Almost there", Toast.LENGTH_LONG).show();

                boolean flag = isJSONValid(response);
                if (flag)
                    Log.d("JSON", "VALID JSON");
                else
                    Log.d("JSON", "INVALID JSON");

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray images = jsonObject.getJSONArray("images");
                    JSONObject zero = images.getJSONObject(Integer.parseInt("0"));

                    if (zero.has("candidates")) {
                        JSONArray candidates = zero.getJSONArray("candidates");
                        JSONObject candidate_zero = candidates.getJSONObject(Integer.parseInt("0"));
                        String name = candidate_zero.getString("subject_id");
                        Double confidence = candidate_zero.getDouble("confidence");


                        Toast.makeText(APIActivity.this, name + " " + confidence, Toast.LENGTH_SHORT).show();
                        String photoNum = name.split("_")[1];
                        Toast.makeText(APIActivity.this, "audio_" + photoNum + ".3gpp", Toast.LENGTH_SHORT).show();
                        storageReference = FirebaseStorage.getInstance().getReference()
                                .child(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber().toString()).child("audios").child("audio_" + photoNum);


                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.personnameis);
                        mediaPlayer.start();

                        tapCameraBtn.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        checkSign.setVisibility(View.VISIBLE);
                        goToHomescreen.setVisibility(View.VISIBLE);
                        deleteButton.setVisibility(View.VISIBLE);

                    /*if(confidence < 0.6){
                        if(mediaPlayer.isPlaying()){
                            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.personnotincontacts);
                            mediaPlayer.start();
                        }

                    }*/


                    /*if (confidence >= 0.6) {
                        Toast.makeText(APIActivity.this, name + " " + confidence, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(APIActivity.this, "No match found", Toast.LENGTH_SHORT).show();
                    }*/

                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri audioUri) {
                                try {


                                    while(mediaPlayer.isPlaying());
                                    MediaPlayer mediaPlayer = new MediaPlayer();
                                    mediaPlayer.setDataSource(audioUri.toString());
                                    mediaPlayer.prepare(); // might take long! (for buffering, etc)
                                    mediaPlayer.start();


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(APIActivity.this, "Cannot find audio", Toast.LENGTH_SHORT).show();
                            }
                        });




                        while (mediaPlayer.isPlaying()) ;
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.taptogotohomescreen);
                                mediaPlayer.start();

                            }
                        }, 4000);

                    } else {

                        tapCameraBtn.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        checkSign.setVisibility(View.VISIBLE);
                        goToHomescreen.setVisibility(View.VISIBLE);
                        deleteButton.setVisibility(View.VISIBLE);

                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.personnotincontacts);
                        mediaPlayer.start();


                        while(mediaPlayer.isPlaying());
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.taptogotohomescreen);
                        mediaPlayer.start();

                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
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

    public void deleteGalleryKairos() {
        // List out all subjects in a given gallery
        String galleryId = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();

        // create kairos listener
        KairosListener listener = new KairosListener() {

            @Override
            public void onSuccess(String response) {
                // your code here!
                Log.d("GALLERY DELETED", response);
                Toast.makeText(getApplicationContext(), "GALLERY DELETED", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(String response) {
                // your code here!
                Log.d("ERROR DELETING GALLERY", response);
                Toast.makeText(getApplicationContext(), "GALLERY NOT DELETED", Toast.LENGTH_SHORT).show();
            }
        };

        Kairos myKairos = new Kairos();

        myKairos.setAuthentication(getApplicationContext(), app_id, api_key);

        try {
            myKairos.deleteGallery(galleryId, listener);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    class LoadImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute(){
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                return downloadBitmap(params[0]);
            } catch (Exception e) {
                // log error
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Toast.makeText(APIActivity.this, "Analyzing photo", Toast.LENGTH_LONG).show();
            uploadPhoto(bitmap);
        }

        private Bitmap downloadBitmap(String url) {
            HttpURLConnection urlConnection = null;
            try {
                URL uri = new URL(url);
                urlConnection = (HttpURLConnection) uri.openConnection();
                int statusCode = urlConnection.getResponseCode();
                if (statusCode != urlConnection.HTTP_OK) {
                    return null;
                }

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;
                }
            } catch (Exception e) {
                urlConnection.disconnect();
                Log.w("ImageDownloader", "Error downloading image from " + url);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return null;
        }

    }

    @Override
    public void onStop()
    {
        super.onStop();
        try{
            if(mediaPlayer !=null && mediaPlayer.isPlaying()){
                Log.d("TAG------->", "player is running");
                mediaPlayer.stop();
                Log.d("Tag------->", "player is stopped");
                mediaPlayer.release();
                Log.d("TAG------->", "player is released");
            }
        }catch(IllegalStateException e){
            Log.d("IllegalStateException" , "it occurs");
        }
    }
}

