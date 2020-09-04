package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapplication.Model.UsersData;
import com.example.chatapplication.SinchCall.BaseActivity;
import com.example.chatapplication.SinchCall.SinchCallListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;

public class CallingActivity  extends  BaseActivity{
    Toolbar toolbar;
    SinchClient sinchClient;
    com.sinch.android.rtc.calling.Call call;
    FirebaseUser fuser;

    SinchCallListener sinchCallListener;
    Thread thread;
    Intent intent;

    ImageButton callendbtn;
    TextView username;
    TextView callstatus;

    DatabaseReference reference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        /**
         * Initialization of Variables
         * TOOLBAR, USERNAME, STATUS
         */

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        toolbar = findViewById(R.id.toolbar);
        callendbtn = findViewById(R.id.btn_end_call);
        username = findViewById(R.id.username);
        callstatus = findViewById(R.id.status);


        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Chat Bot call");
        getSupportActionBar().setHomeButtonEnabled(true);



        callstatus.setText("Loading");
        intent = getIntent();
        final String userid = intent.getStringExtra("Userid");

        reference = FirebaseDatabase.getInstance().getReference("appusers").child(userid);

        /**
         * Adding Listener
         */
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UsersData user= dataSnapshot.getValue(UsersData.class);
                username.setText(user.getUsername());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
       });

        sinchClient = Sinch.getSinchClientBuilder()
                .context(this)
                .userId(fuser.getUid())
                .applicationKey("de3b3bf6-fff6-4d40-9c25-ba9b848861e2")
                .applicationSecret("B7u+d2DqnEuUU5JNjN2uJw==")
                .environmentHost("clientapi.sinch.com")
                .build();

        sinchClient.setSupportCalling(true);
        sinchClient.startListeningOnActiveConnection();
        sinchClient.start();
        sinchClient.isStarted();
        sinchCallListener= new SinchCallListener(CallingActivity.this);

        /**
         * Because sinchClient takes time to initiate
         */

        callendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(call!=null)
                {
                    call.hangup();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    },1000);
                }
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(sinchClient.isStarted())
                {
                    callstatus.setText("Connecting");
                    callUser(userid);
                }
                else
                {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(sinchClient.isStarted()) {
                                callstatus.setText("Connecting");
                                callUser(userid);
                            }
                            else
                            {
                                callstatus.setText("Error");
                                Toast.makeText(CallingActivity.this,"Connection error",Toast.LENGTH_SHORT).show();
                            }
                        }
                    },2000);
                }
            }
        }, 1000);

        thread = new Thread(){
            int times=0;
            @Override
            public void run() {
                while (times<60)
                {
                    try {
                        Thread.sleep(1000);
                        final int status = sinchCallListener.getStatus();

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                updatestatus(status);
                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                super.run();
            }
        };
    }

    public void updatestatus(int status)
    {
        if(status==100)
        {
            callstatus.setText("Ringing");
        }
        else if(status==200)
        {
            callstatus.setText("Established");
        }

    }


    public void callUser(String userid) {
        if (call == null) {
            call = sinchClient.getCallClient().callUser(userid);
            call.addCallListener(sinchCallListener);
            thread.start();

        }
    }


}
