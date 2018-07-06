package com.example.pooja.auxilio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    private ImageView checkSign;
    private TextView goToHomescreen;

    private MediaRecorder mRecorder;
    private  String mFileName = null;

    private boolean isRecording = false;

    private StorageReference storageReference;
    private DatabaseReference mCount;
    public int audioCounter;

    private MediaPlayer mediaPlayer = null;


    String uniquefilename;

    private static final String LOG_TAG = "Record_log";

     SharedPreferences spForUploadCounter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio);

        spForUploadCounter = getSharedPreferences("uploadCounter", 0);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.tapandholdtorecord);
        mediaPlayer.start();

        storageReference = FirebaseStorage.getInstance().getReference();


        tapToRecordBtn = (Button)findViewById(R.id.tapToRecordBtn);
        record_progressBar = (ProgressBar)findViewById(R.id.record_progressBar);
        checkSign = (ImageView)findViewById(R.id.check_sign);
        goToHomescreen = (TextView)findViewById(R.id.goto_homescreen);
        checkSign.setVisibility(View.GONE);
        goToHomescreen.setVisibility(View.GONE);

        record_progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN );


        checkSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                Intent intent = new Intent(RecordAudioActivity.this, GestureActivity.class);
                startActivity(intent);
                finish();
            }
        });

        goToHomescreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                Intent intent = new Intent(RecordAudioActivity.this, GestureActivity.class);
                startActivity(intent);
                finish();
            }
        });


        storeAudioToFirebase();

    }


    private void storeAudioToFirebase(){


        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/recorded_audio.mp3";

        tapToRecordBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                mediaPlayer.stop();

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

        //mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);


        mRecorder.setOutputFile(mFileName);

        //mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

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

                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.waitwhilerecisuploaded);
                mediaPlayer.start();

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

                StorageReference filepath = storageReference.child( "/" + uniquefilename + "/audios/" + "audio_" + audioCounter);

                filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        mediaPlayer.stop();
                        record_progressBar.setVisibility(ProgressBar.GONE);
                        checkSign.setVisibility(View.VISIBLE);
                        goToHomescreen.setVisibility(View.VISIBLE);
                        Toast.makeText(RecordAudioActivity.this, "Audio Uploaded!", Toast.LENGTH_SHORT).show();

                      /*  SharedPreferences.Editor editor = spForUploadCounter.edit();
                        editor.putInt("uploadCounter" , counter);
                        editor.commit();*/

                        if(!mediaPlayer.isPlaying()){
                            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.taptogotohomescreen);
                            mediaPlayer.start();
                        }

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mediaPlayer.stop();
        mediaPlayer.release();
    }


}
