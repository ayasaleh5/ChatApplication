package com.example.chatapplication;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.chatapplication.Model.App;
import com.example.chatapplication.utils.AudioPlayer;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.sinch.android.rtc.calling.CallState;
import com.sinch.android.rtc.video.VideoCallListener;
import com.sinch.android.rtc.video.VideoController;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.chatapplication.utils.Utils.hasPermissions;


public class CallActivity extends AppCompatActivity implements ServiceConnection {
    TextView tv_phone_number, tv_call_status;
    ImageButton btn_endcall, btn_startcall;
    ToggleButton btn_speaker;
    Call call;
    Timer t = new Timer();
    int time = 0;
    AudioManager audioManager;
    String text_number;
    CallClient callClient;
    AudioPlayer audioPlayer;
    boolean is_video = false;
    private boolean mAddedListener = false;
    private boolean mVideoViewsAdded = false;
    private long mCallStart = 0;
    String phoneNumber;
    double orginalSubscription = 0, subscription = 0;
    double orginalCredit = 0, credit = 0;
    double orginalGlobalCredit = 0, globalCredit = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO};

        if (!hasPermissions(this,PERMISSIONS )) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
finish();
        } else {
            btn_endcall = findViewById(R.id.btnEndCall);
            btn_startcall = findViewById(R.id.btnStartCall);
            tv_phone_number = findViewById(R.id.tvPhoneNumber);
            tv_call_status = findViewById(R.id.tvCallStatus);
            btn_speaker = findViewById(R.id.btnSpeaker);
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            callClient = App.getInstance().sinchClient.getCallClient();
            audioPlayer = new AudioPlayer(this);

            tv_call_status.setText("Connecting");

            btn_startcall.setVisibility(View.GONE);

            btn_speaker.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    audioManager.setSpeakerphoneOn(b);
                }
            });

//            ImageView imageView = (ImageView) findViewById(R.id.imageview_bg);
//            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_default_avatar);
//            Bitmap blurredBitmap = blur(bitmap);
//            imageView.setImageBitmap(blurredBitmap);

           if (getIntent().hasExtra("call_video")) {
                call = callClient.callUserVideo(getIntent().getStringExtra("call_video"));
                System.out.println("calling " + getIntent().getStringExtra("call_video"));
                text_number = getIntent().getStringExtra("call_video");
                is_video = true;
            } else if (getIntent().hasExtra("user_number")) {

                call = callClient.callUser(getIntent().getStringExtra("user_number"));
                System.out.println("calling " + getIntent().getStringExtra("user_number"));
            } else if (getIntent().hasExtra("INCOMING_CALL")) {
                if (getIntent().hasExtra("is_video"))
                    is_video = getIntent().getBooleanExtra("is_video", false);

                call = App.getInstance().call;
                tv_call_status.setText("INCOMING CALL");
                text_number = call.getHeaders().get("user_name") != null ? call.getHeaders().get("user_name") : "UNKNOWN";
                System.out.println("number= " + text_number);
                btn_startcall.setVisibility(View.VISIBLE);
                audioPlayer.playRingtone();

            }
            tv_phone_number.setText(text_number);

            btn_endcall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (call != null) {
                        audioPlayer.stopRingtone();


                        endCall();
                    }
                }
            });
            btn_startcall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    audioPlayer.stopRingtone();
                    call.answer();
                    btn_startcall.setVisibility(View.GONE);

                }
            });

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sp = getSharedPreferences("OURINFO", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", true);
        ed.commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences sp = getSharedPreferences("OURINFO", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", false);
        ed.commit();
        removeVideoViews();

    }


    private void addVideoViews() {


        if (mVideoViewsAdded) {
            System.out.println("mVideoViewsAdded=true");
            return; //early
        }
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                findViewById(R.id.header_layout).getLayoutParams();
        params.weight = 1.0f;
        findViewById(R.id.header_layout).setLayoutParams(params);

        findViewById(R.id.video_layout).setVisibility(View.VISIBLE);
        final VideoController vc = App.getInstance().sinchClient.getVideoController();
        if (vc != null) {
            RelativeLayout localView = (RelativeLayout) findViewById(R.id.localVideo);
            localView.addView(vc.getLocalView());

            localView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //this toggles the front camera to rear camera and vice versa
                    vc.toggleCaptureDevicePosition();
                }
            });

            LinearLayout view = (LinearLayout) findViewById(R.id.remoteVideo);
            view.addView(vc.getRemoteView());
            mVideoViewsAdded = true;
        }
    }

    //removes video feeds from the app once the call is terminated
    private void removeVideoViews() {
        if (App.getInstance().sinchClient.getVideoController() == null) {
            System.out.println("App.getInstance().sinchClient.getVideoController()=null");
            return; // early
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                findViewById(R.id.header_layout).getLayoutParams();
        params.weight = 3.0f;
        findViewById(R.id.header_layout).setLayoutParams(params);
        findViewById(R.id.video_layout).setVisibility(View.GONE);

        VideoController vc = App.getInstance().sinchClient.getVideoController();
        if (vc != null) {
            LinearLayout view = (LinearLayout) findViewById(R.id.remoteVideo);
            view.removeView(vc.getRemoteView());

            RelativeLayout localView = (RelativeLayout) findViewById(R.id.localVideo);
            localView.removeView(vc.getLocalView());
            mVideoViewsAdded = false;
        }
    }


    private void updateUI() {
        if (App.getInstance().sinchClient == null) {
            System.out.println("App.getInstance().sinchClient=null");
            return; // early
        }

        if (call != null) {
            tv_phone_number.setText(call.getRemoteUserId());
            tv_call_status.setText(call.getState().toString());
            if (call.getState() == CallState.ESTABLISHED) {
                //when the call is established, addVideoViews configures the video to  be shown
                addVideoViews();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
//        if (call != null) {
//            if (!mAddedListener) {
//                call.addCallListener(new SinchCallListener());
//                mAddedListener = true;
//            }
//        } else {
//            Log.e("call", "Started with invalid callId, aborting.");
//            finish();
//        }


    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    private class SinchCallListener implements VideoCallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d("video", "Call ended. Reason: " + cause.toString());
            audioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            String endMsg = "Call ended: " + call.getDetails().toString();
            System.out.println(endMsg);
//            Toast.makeText(CallActivity.this, endMsg, Toast.LENGTH_LONG).show();

            endCall();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d("video", "Call established");
            audioPlayer.stopProgressTone();
            tv_call_status.setText(call.getState().toString());
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            AudioController audioController = App.getInstance().sinchClient.getAudioController();
            audioController.enableSpeaker();
            mCallStart = System.currentTimeMillis();
            Log.d("video", "Call offered video: " + call.getDetails().isVideoOffered());
            updateUI();

        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d("video", "Call progressing");
            audioPlayer.playProgressTone();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }

        @Override
        public void onVideoTrackAdded(Call call) {
            Log.d("video", "Video track added");
            addVideoViews();
        }

        @Override
        public void onVideoTrackPaused(Call call) {

        }

        @Override
        public void onVideoTrackResumed(Call call) {

        }
    }

    private void endCall() {
        audioPlayer.stopProgressTone();
        if (call != null) {
            call.hangup();
            call = null;
        }
//        if (time > 0) {
//            String cost = "";
//            if (orginalGlobalCredit > 0)
//                cost = "$" + String.format("%.5f", orginalGlobalCredit - globalCredit);
//            else if (orginalSubscription > 0)
//                cost = String.valueOf(orginalSubscription - subscription) + " units";
//            else if (orginalCredit > 0)
//                cost = "$" + String.format("%.5f", orginalCredit - credit);
//
//
//            int seconds = time % 60;
//            int minutes = time / 60;
//            String msg = "Call cost: " + cost + "\n" + "Duration: " + (minutes > 9 ? minutes : "0" + minutes) + ":" + (seconds > 9 ? seconds : "0" + seconds);
//            AlertDialog alertDialog = new AlertDialog.Builder(CallActivity.this).create();
//            alertDialog.setTitle("Call ended");
//            alertDialog.setMessage(msg);
//            alertDialog.setCancelable(false);
//            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                            CallActivity.this.finish();
//                        }
//                    });
//            alertDialog.show();
//        } else
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    CallActivity.this.finish();
                }
            }, 3000);
    }

    private static final float BLUR_RADIUS = 25f;

    public Bitmap blur(Bitmap image) {
        if (null == image) return null;

        Bitmap outputBitmap = Bitmap.createBitmap(image);
        final RenderScript renderScript = RenderScript.create(this);
        Allocation tmpIn = Allocation.createFromBitmap(renderScript, image);
        Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);

        //Intrinsic Gausian blur filter
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }



    private void addCallListener() {
        if (!is_video)
            call.addCallListener(new CallListener() {
                @Override
                public void onCallProgressing(Call call) {
                    System.out.println("onCallProgressing");
                    tv_call_status.setText("Ringing");
                    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

                }

                @Override
                public void onCallEstablished(Call call) {
                    System.out.println("onCallEstablished");




                }

                @Override
                public void onCallEnded(Call call) {
                    System.out.println("onCallEnded");
                    t.cancel();
                    audioManager.setSpeakerphoneOn(false);
                    tv_call_status.setText("call ended");
                    setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

                    System.out.println("test call " + call.getDetails());
                    call.hangup();

                    endCall();

                }

                @Override
                public void onShouldSendPushNotification(Call call, List<PushPair> list) {
                    System.out.println("onShouldSendPushNotification");

                }
            });

        else {
            if (!mAddedListener) {
                call.addCallListener(new SinchCallListener());
                mAddedListener = true;
            }
        }
    }
}
