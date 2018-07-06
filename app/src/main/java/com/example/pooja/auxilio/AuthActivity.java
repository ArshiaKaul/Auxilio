package com.example.pooja.auxilio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kairos.Kairos;
import com.kairos.KairosListener;

import java.util.concurrent.TimeUnit;

//phone number authentication

public class AuthActivity extends AppCompatActivity {

    // instantiate a new kairos instance
    Kairos myKairos = new Kairos();

    public static String ip_address_entered   = null;

    // set authentication
    String app_id = "XXXXXXXXX";   //app id
    String api_key = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"; //api key

    private EditText phone_et;
    private EditText otp_et;
    private Button register_btn;
    private Button proceed_btn;
    private TextView error_tv;
    private TextView error_invalid_phoneno;
    private TextView about_otp;
    private EditText ip_address_et;

    private int btnType = 0;

    static SharedPreferences sp;
    public static final String USER_PREF = "USER_PREF_IP_ADDR" ;
    public static final String KEY_IP = "KEY_NAME";

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    private FirebaseAuth mAuth;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseAuth.AuthStateListener authStateListener;

    private DatabaseReference mRoot;
    private DatabaseReference mUserPhoneNumber;
    private DatabaseReference mCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        sp = getSharedPreferences(USER_PREF, Context.MODE_PRIVATE);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Create an instance of the KairosListener
        KairosListener listener = new KairosListener() {

            @Override
            public void onSuccess(String response) {
                // your code here!
                Log.d("SUCCESSFUL ENROLL", response);
                Toast.makeText(getApplicationContext(), "SUCCESSFUL ENROLL", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(String response) {
                // your code here!
                Log.d("ENROLL FAILURE", response);
                Toast.makeText(getApplicationContext(), "ENROLL FAILURE", Toast.LENGTH_SHORT).show();
            }
        };

        ip_address_et = (EditText)findViewById(R.id.ip_address_et);

        phone_et = (EditText)findViewById(R.id.phone_et);
        otp_et = (EditText)findViewById(R.id.otp_et);

        error_tv = (TextView)findViewById(R.id.error_tv);
        error_invalid_phoneno = (TextView)findViewById(R.id.error_invalid_phoneno);
        about_otp = (TextView)findViewById(R.id.about_otp);

        register_btn = (Button)findViewById(R.id.register_btn);
        proceed_btn = (Button)findViewById(R.id.proceed_btn);

        myKairos.setAuthentication(this, app_id, api_key);

        if (sp.contains(KEY_IP)) {
            ip_address_entered = sp.getString(KEY_IP, "");
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null && ip_address_entered != null && !ip_address_entered.isEmpty() ) {
                    Intent intent = new Intent(AuthActivity.this, GestureActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        mAuth = FirebaseAuth.getInstance();

        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (btnType == 0) {

                    phone_et.setEnabled(false);
                    register_btn.setEnabled(false);

                    String phoneNumber = "+91"+ phone_et.getText().toString();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber,
                            60,
                            TimeUnit.SECONDS,
                            AuthActivity.this,
                            mCallbacks);

                }else{

                    register_btn.setEnabled(false);

                    String verificationCode = otp_et.getText().toString();
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);

                    signInWithPhoneAuthCredential(credential);

                }
            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

                //verification successful
                signInWithPhoneAuthCredential(phoneAuthCredential);

            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

                //display error that phone no is invalid
                error_invalid_phoneno.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                btnType = 1;
                register_btn.setEnabled(true);
                register_btn.setText("Register");

                otp_et.setVisibility(View.VISIBLE);
                // ...
            }
        };
    }

    public void callMeForIP(){
        final Intent intent = new Intent(AuthActivity.this, GestureActivity.class);

        otp_et.setVisibility(View.GONE);
        phone_et.setVisibility(View.GONE);
        about_otp.setVisibility(View.GONE);
        ip_address_et.setVisibility(View.VISIBLE);
        register_btn.setVisibility(View.GONE);
        proceed_btn.setVisibility(View.VISIBLE);

        proceed_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ip_address_entered = ip_address_et.getText().toString();
                Toast.makeText(AuthActivity.this, "Ip is " + ip_address_entered, Toast.LENGTH_LONG).show();


                SharedPreferences.Editor editor = sp.edit();
                editor.putString(KEY_IP, ip_address_entered);
                editor.commit();

                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }


    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull final Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information


                            FirebaseUser user = task.getResult().getUser();

                            mRoot = FirebaseDatabase.getInstance().getReference();
                            mUserPhoneNumber = mRoot.child(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber().toString());
                            mUserPhoneNumber.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!dataSnapshot.exists()){
                                        mCount = mUserPhoneNumber.child("count");
                                        mCount.setValue(0);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(getApplicationContext(), "Could not create account", Toast.LENGTH_SHORT).show();
                                }
                            });
                           // mCount = mUserPhoneNumber.child("count");

                           // mCount.setValue(0);

                            Toast.makeText(getApplicationContext(), "Database", Toast.LENGTH_SHORT).show();

                            /*
                            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("MyPref" , AuthActivity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("key_name" , true);
                            editor.commit();

                            */

                            callMeForIP();

                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            error_tv.setVisibility(View.VISIBLE);
                            Toast.makeText(getApplicationContext(), "OTP PROB", Toast.LENGTH_SHORT).show();

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }
}
