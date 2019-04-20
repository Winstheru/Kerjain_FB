package com.example.kerjain;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class VerifyActivity extends AppCompatActivity {
    //These are the objects needed
    //It is the verification id that will be sent to the user
    private String mVerificationId;

    //The edittext to input the code
    private EditText otp;

    //firebase auth object
    private FirebaseAuth mAuth;

    private DatabaseReference mDatabase;
    private String name, alamat, email, pos, ktp, provinsi, kotakabupaten, kecamatan, mobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);



        //initializing objects
        mAuth = FirebaseAuth.getInstance();
        otp = findViewById(R.id.otp);



        //getting mobile number from the previous activity
        //and sending the verification code to the number
        Intent intent = getIntent();
        mobile = intent.getStringExtra("mobile");
        name = intent.getStringExtra("name");
        email = intent.getStringExtra("email");
        ktp = intent.getStringExtra("ktp");
        pos = intent.getStringExtra("pos");
        alamat = intent.getStringExtra("alamat");
        provinsi = intent.getStringExtra("provinsi");
        kotakabupaten = intent.getStringExtra("kotakabupaten");
        kecamatan = intent.getStringExtra("kecamatan");
        sendVerificationCode(mobile);
        //if the automatic sms detection did not work, user can also enter the code manually
        //so adding a click listener to the button
        findViewById(R.id.buttonSignIn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = otp.getText().toString().trim();
                if (code.isEmpty() || code.length() < 6) {
                    otp.setError("Enter valid code");
                    otp.requestFocus();
                    return;
                }

                //verifying the code entered manually
                verifyVerificationCode(code);
            }
        });
    }
    //the method is sending verification code
    //the country id is concatenated
    //you can take the country id as user input as well
    private void sendVerificationCode(String mobile) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+62" + mobile,
                60,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                mCallbacks);
    }

    //the callback to detect the verification status
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

            //Getting the code sent by SMS
            String code = phoneAuthCredential.getSmsCode();

            //sometime the code is not detected automatically
            //in this case the code will be null
            //so user has to manually enter the code
            if (code != null) {
                otp.setText(code);
                //verifying the code
                verifyVerificationCode(code);
            }
        }
        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(VerifyActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);

            //storing the verification id that is sent to the user
            mVerificationId = s;
        }
    };
    private void verifyVerificationCode(String code) {
        //creating the credential
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        //signing the user
        signInWithPhoneAuthCredential(credential);
    }
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(VerifyActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            final FirebaseUser user = mAuth.getInstance().getCurrentUser();
                            DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
                            DatabaseReference mConditionRef = mRootRef.child("users").child("pekerja").child(user.getUid());
                            DatabaseReference mRegistered = mRootRef.child("registered").child("pekerja");
                            mConditionRef.child("nama").setValue(name);
                            mConditionRef.child("nomor_telepon").setValue(mobile);
                            mConditionRef.child("email").setValue(email);
                            mConditionRef.child("nomor_ktp").setValue(ktp);
                            mConditionRef.child("kode_pos").setValue(pos);
                            mConditionRef.child("alamat").setValue(alamat);
                            mConditionRef.child("provinsi").setValue(provinsi);
                            mConditionRef.child("kota/kabupaten").setValue(kotakabupaten);
                            mConditionRef.child("kecamatan").setValue(kecamatan);
                            mRegistered.child(mobile).setValue(mobile);
                            //verification successful we will start the profile activity
                            Intent intent = new Intent(VerifyActivity.this, BottomNavigationView.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        } else {

                            //verification unsuccessful.. display an error message

                            String message = "Somthing is wrong, we will fix it soon...";

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                message = "Invalid code entered...";
                            }

                            Snackbar snackbar = Snackbar.make(findViewById(R.id.parent), message, Snackbar.LENGTH_LONG);
                            snackbar.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            });
                            snackbar.show();
                        }
                    }
                });
    }

}
