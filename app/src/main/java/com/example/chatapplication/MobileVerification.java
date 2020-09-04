package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class MobileVerification extends AppCompatActivity {

    private Button et_otpcode;
    private String mVerificationId;
    private EditText editTextCode;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);


        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("appusers");


        editTextCode = findViewById(R.id.editTextCode);


        Intent intent = getIntent();
        final String mobile = intent.getStringExtra("phonenumber");

        //check if mobile number is already in DB or not with following code.

     /*   databaseReference.orderByChild("usermobilenumber").equalTo(mobile).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Toast.makeText(MobileVerification.this, "Mobile Number wih this user is already Exist", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                sendVerificationCode(mobile);

            }
        });*/


        sendVerificationCode(mobile);




        et_otpcode = findViewById(R.id.buttonSignIn);
        et_otpcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = editTextCode.getText().toString().trim();
                if (code.isEmpty() || code.length() < 6) {
                    editTextCode.setError("Enter valid code");
                    editTextCode.requestFocus();
                    return;
                }


                verifyVerificationCode(code);
            }
        });
    }

    private void sendVerificationCode(String mobile) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                 mobile,
                50,
                TimeUnit.SECONDS,
                this,
                mCallbacks);
    }


    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {


            String code = phoneAuthCredential.getSmsCode();


            if (code != null) {
                editTextCode.setText(code);

                verifyVerificationCode(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(MobileVerification.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);


            mVerificationId = s;
        }
    };


    private void verifyVerificationCode(String code) {

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);


        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(MobileVerification.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //verification successful we will start the profile activity
                            Intent intent = new Intent(MobileVerification.this, YourContactDetailsActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        } else {


                            String message = "Somthing is wrong, we will fix it soon...";

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                message = "Invalid code entered...";
                            }


                        }
                    }
                });
    }
}
