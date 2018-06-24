package com.example.pooja.auxilio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.pooja.auxilio.R.id.progressBar;
import static com.example.pooja.auxilio.R.id.tapCameraBtn;

public class RecordAudioActivity extends AppCompatActivity {

    private Button tapToRecordBtn;
    private ProgressBar record_progressBar;

    private MediaRecorder mRecorder;
    private  String mFileName = null;

    private boolean isRecording = false;

    private StorageReference storageReference;
    private DatabaseReference mCount;
    public int audioCounter;



    String uniquefilename;

    private static final String LOG_TAG = "Record_log";

     SharedPreferences spForUploadCounter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio);

        spForUploadCounter = getSharedPreferences("uploadCounter", 0);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        storageReference = FirebaseStorage.getInstance().getReference();


        tapToRecordBtn = (Button)findViewById(R.id.tapToRecordBtn);
        record_progressBar = (ProgressBar)findViewById(R.id.record_progressBar);

        record_progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN );

        storeAudioToFirebase();



    }


    private void storeAudioToFirebase(){


        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/recorded_audio.mp3";

        tapToRecordBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN){

                    /*final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 1000ms
                            startRecording();
                        }
                    }, 1000);*/

                    startRecording();

                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    stopRecording();
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

        isRecording = true;
        mRecorder.start();



    }

    private void stopRecording() {
        try {

            if (mRecorder != null && isRecording) {

                mRecorder.stop();


                //the tap to open camera button disappears
                tapToRecordBtn.setVisibility(Button.GONE);

                //and now we make the progress bar visible instead of the button

                record_progressBar.setVisibility(ProgressBar.VISIBLE);

                mRecorder.release();
                mRecorder = null;

                Toast.makeText(this, "Uploading Audio...", Toast.LENGTH_SHORT).show();
                uploadAudio();

            }
            else
            {
                recreate();
               //write code here
            }


        }
        catch (Exception e){
            //handle exception if any

        }

    }

     int counter;
    private void uploadAudio(){

        final Uri uri = Uri.fromFile(new File(mFileName));
        final String userPhoneNumber = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        uniquefilename = userPhoneNumber.toString();

        //counter = spForUploadCounter.getInt("uploadCounter" , 0);
        //counter++;

        mCount = FirebaseDatabase.getInstance().getReference().child(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber().toString()).child("count");


        mCount.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                audioCounter = dataSnapshot.getValue(Integer.class);

                StorageReference filepath = storageReference.child( "/" + uniquefilename + "/audios/" + "audio" + audioCounter);

                filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Toast.makeText(RecordAudioActivity.this, "Audio Uploaded!", Toast.LENGTH_SHORT).show();

                        SharedPreferences.Editor editor = spForUploadCounter.edit();
                        editor.putInt("uploadCounter" , counter);
                        editor.commit();

                        Intent intent = new Intent(RecordAudioActivity.this, GestureActivity.class);
                        startActivity(intent);
                        finish();


                    }
                });

                audioCounter++;
                mCount.setValue(audioCounter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("The read failed: ", "FAILED");
            }
        });

    }


}
