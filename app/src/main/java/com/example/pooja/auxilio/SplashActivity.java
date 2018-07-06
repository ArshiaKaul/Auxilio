package com.example.pooja.auxilio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Space;

import  com.example.pooja.auxilio.AuthActivity ;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class SplashActivity extends AppCompatActivity {

    private ImageView Logo_text_app;
   // private boolean answer = (boolean) AuthActivity.isLoggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //final SharedPreferences sp = this.getSharedPreferences("MyPref" , Context.MODE_PRIVATE);



        Logo_text_app = (ImageView)findViewById(R.id.Logo_text_app);

        Animation myanim = AnimationUtils.loadAnimation(this, R.anim.mtransition);
        Logo_text_app.startAnimation(myanim);

        final Intent intent = new Intent(this, AuthActivity.class);
        Thread timer = new Thread(){

            public void run(){
                try {
                    sleep(2000);
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
                finally {


                    /*
                    if(sp.getBoolean("key_name", false ) ==  true){

                            Intent intent2 = new Intent(SplashActivity.this, GestureActivity.class);
                            startActivity(intent2);
                            finish();
                        }

                        else {

                        */
                        startActivity(intent);
                        finish();
                    /*
                    }

                    */

                }
            }
        };

        timer.start();

    }

}


