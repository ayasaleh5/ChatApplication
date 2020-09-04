package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapplication.Model.AppUser;
import com.example.chatapplication.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class YourContactDetailsActivity extends AppCompatActivity {

    private EditText user_name, user_email;
    private TextView user_mobile, tv_updateuserdetail;
    ProgressBar progressbar;
    FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_contact_details);


        firebaseAuth = FirebaseAuth.getInstance();

        user_name = findViewById(R.id.user_name);
        user_mobile = findViewById(R.id.user_mobile);
        user_email = findViewById(R.id.user_email);
        progressbar = findViewById(R.id.progressbar);
        tv_updateuserdetail = findViewById(R.id.tv_updateuserdetail);

        tv_updateuserdetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = user_name.getText().toString().trim();
                String useremail = user_email.getText().toString().trim();
                String usermobilenumber = user_mobile.getText().toString().trim();

                //checking if email and passwords are empty
                if (TextUtils.isEmpty(username)) {
                    Toast.makeText(YourContactDetailsActivity.this, "Please enter name", Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(useremail)) {
                    Toast.makeText(YourContactDetailsActivity.this, "Please enter password", Toast.LENGTH_LONG).show();
                    return;
                }

                //if the email and password are not empty
                //displaying a progress dialog

                progressbar.setVisibility(View.VISIBLE);

                String image = "default";
                String status = "offline";
                String userId =FirebaseAuth.getInstance().getCurrentUser().getUid();
                AppUser appUser = new AppUser(username, useremail, usermobilenumber,image,userId,status);

                FirebaseDatabase.getInstance()
                        .getReference("appusers")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(appUser)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if (task.isSuccessful()) {
                                    progressbar.setVisibility(View.GONE);
                                    Toast.makeText(YourContactDetailsActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(YourContactDetailsActivity.this, ProfileActivity.class));
                                    finish();
                                } else {
                                    progressbar.setVisibility(View.GONE);
                                    Toast.makeText(YourContactDetailsActivity.this, "Registration failed , Something went wrong ", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });


    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            // No user is signed in
        } else {
            user_mobile.setText(currentUser.getPhoneNumber());
        }
    }
}
