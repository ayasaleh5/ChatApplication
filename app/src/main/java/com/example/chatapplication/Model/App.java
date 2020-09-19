package com.example.chatapplication.Model;

import android.app.Application;
import android.content.Intent;

import com.example.chatapplication.CallActivity;
import com.example.chatapplication.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchClientListener;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;

public class App extends Application {
    private static App instance = null;

    public SinchClient sinchClient;
    public Call call;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    public static synchronized App getInstance() {
        return instance;
    }

    public void startSinchService(){
         if(FirebaseAuth.getInstance().getCurrentUser() !=null) {
             sinchClient = Sinch.getSinchClientBuilder().context(this)
                    .applicationKey(Constants.SINCH_APPLICATION_KEY)
                    .applicationSecret(Constants.SINCH__SECRET_KEY)
                    .environmentHost("sandbox.sinch.com")
                    .userId(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid()))
                    .build();
            sinchClient.setSupportCalling(true);

            sinchClient.addSinchClientListener(new SinchClientListener() {
                public void onClientStarted(SinchClient client) {
                    System.out.println("onClientStarted");

                }

                public void onClientStopped(SinchClient client) {
                    System.out.println("onClientStopped");

                }

                public void onClientFailed(SinchClient client, SinchError error) {
                    System.out.println("onClientFailed "+error.getMessage());


                }

                public void onRegistrationCredentialsRequired(SinchClient client, ClientRegistration registrationCallback) {
                    System.out.println("onRegistrationCredentialsRequired");

                }

                public void onLogMessage(int level, String area, String message) {
                    System.out.println("onLogMessage "+message);

                }
            });
            sinchClient.startListeningOnActiveConnection();
            sinchClient.start();
            sinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());



        }
    }

    private class SinchCallClientListener implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call incomingCall) {
            //Pick up the call!

            Intent call_intent = new Intent(App.this, CallActivity.class);
            call = incomingCall;

            System.out.println("INCOMING_CALL " + call.getRemoteUserId()+" is_video?="+call.getDetails().isVideoOffered());

            if (call.getDetails().isVideoOffered()) {
                call_intent.putExtra("is_video", true);
            }

            call_intent.putExtra("INCOMING_CALL", call.getRemoteUserId());
            call_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(call_intent);

        }
    }

}
