package com.example.pooja.auxilio;

import android.content.Intent;
import android.os.Bundle;
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

public class GestureActivity extends AppCompatActivity {

    private TextView justExampletextView ;
    private GestureDetectorCompat mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);


        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        justExampletextView = (TextView) findViewById(R.id.textViewAddContact);

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
            Log.d(DEBUG_TAG,"onDown: " + event.toString());
            return true;
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
                    return true;

                case 3:
                    Log.d(DEBUG_TAG, "down");
                    randomMethird();
                    return true;

                case 4:
                    Log.d(DEBUG_TAG, "right");
                    return true;
            }
            return false;
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
    }

    public void randomMethird(){
        // Create an Intent to start the second activity
        Intent randomIntent = new Intent(this, AddContactActivity.class);
        Log.d("AddContactActivity", "INTENT part 1");

        // Start the new activity.
        startActivity(randomIntent);
        Log.d("AddContactActivity", "INTENT part 2");
    }
}
