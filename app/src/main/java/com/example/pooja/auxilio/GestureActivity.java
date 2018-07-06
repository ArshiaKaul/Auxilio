package com.example.pooja.auxilio;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GestureActivity extends AppCompatActivity {

    private TextView justExampletextView ;
    private GestureDetectorCompat mDetector;
    private MediaPlayer mediaPlayer = null;

    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);


            authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser == null) {
                    Intent intent = new Intent(GestureActivity.this, AuthActivity.class);
                    startActivity(intent);
                }
            }
        };

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.swipeupdown);
        //mediaPlayer.start();


        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        justExampletextView = (TextView) findViewById(R.id.textViewAddContact);

    }


    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";


        @Override
        public boolean onDown(MotionEvent event) {
            mediaPlayer.stop();
            mediaPlayer.release();
            Log.d(DEBUG_TAG,"onDown: " + event.toString());
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            refresh();
        }

        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
            refresh();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            refresh();
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {

            switch (getSlope(e1.getX(), e1.getY(), e2.getX(), e2.getY())) {
                case 1:
                    Log.d(DEBUG_TAG, "top");
                    randomMesecond();
                    return true;

                case 2:
                    Log.d(DEBUG_TAG, "left");
                    refresh();
                    return true;

                case 3:
                    Log.d(DEBUG_TAG, "down");
                    randomMethird();
                    return true;

                case 4:
                    Log.d(DEBUG_TAG, "right");
                    refresh();
                    return true;

                default:
                    refresh();
                    return true;
            }

        }


        private int getSlope(float x1, float y1, float x2, float y2) {
            Double angle = Math.toDegrees(Math.atan2(y1 - y2, x2 - x1));
            if (angle > 45 && angle <= 135)
                // top
                return 1;
            if (angle >= 135 && angle < 180 || angle < -135 && angle > -180)
                // left
                return 2;
            if (angle < -45 && angle>= -135)
                // down
                return 3;
            if (angle > -45 && angle <= 45)
                // right
                return 4;
            return 0;
        }
    }

    public void randomMesecond(){
        // Create an Intent to start the second activity
        Intent randomIntent = new Intent(this, APIActivity.class);

        // Start the new activity.
        startActivity(randomIntent);
        finish();
    }

    public void randomMethird(){
        // Create an Intent to start the second activity
        Intent randomIntent = new Intent(this, AddContactActivity.class);
        Log.d("AddContactActivity", "INTENT part 1");

        // Start the new activity.
        startActivity(randomIntent);
        Log.d("AddContactActivity", "INTENT part 2");
        finish();
    }

    public void refresh() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.swipeupdown);
        mediaPlayer.start();
    }
/*
    @Override
    public void onPause()
    {
        super.onPause();
        mediaPlayer.stop();
        mediaPlayer.release();
    }
*/

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
