package com.example.chatapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapplication.Adapter.MessageAdapter;
import com.example.chatapplication.Fragments.ApiService;
import com.example.chatapplication.Model.Chat;
import com.example.chatapplication.Model.UsersData;
import com.example.chatapplication.Notifications.Client;
import com.example.chatapplication.Notifications.Data;
import com.example.chatapplication.Notifications.MyResponse;
import com.example.chatapplication.Notifications.Sender;
import com.example.chatapplication.Notifications.Token;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.calling.CallListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;
    DatabaseReference reference;
    FirebaseUser fuser;
    Intent intent;
    ImageButton btn_send, btn_attach, btn_call;
    EditText text_send;
    MessageAdapter messageAdapter;
    List<Chat> mChat;
    RecyclerView recyclerView;
    ValueEventListener seenListener;
    private static final int GALLERY_PICK = 1;
    String userid;
    ImageView imageIv;
    ApiService apiService;
    boolean notify  = false;
    //permission constants
    private static final int CAMERA_REQUEST_COD=100;
    private static final int STORAGE_REQUEST_COD=200;

    private static final int IMAGE_PICK_CAMERA_COD=300;
    private static final int IMAGE_PICK_GALLERY_COD=400;
    //permissions array
    String[] cameraPermissions;
    String[] storagePermissions;
    String[] audioPermissions;

    Uri image_uri= null;
    com.sinch.android.rtc.calling.Call call;

    SinchClient sinchClient;
    ArrayList<UsersData> usersDataArrayList;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        imageIv = findViewById(R.id.messageIv);
        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> startActivity(new Intent(MessageActivity.this, ProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        apiService = Client.getClient("https://fcm.googleapis.com/").create(ApiService.class);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        usersDataArrayList = new ArrayList<>();
        profile_image = findViewById(R.id.profile_image4);
        username = findViewById(R.id.tvUsername4);
        btn_send = findViewById(R.id.btn_send);
        btn_attach = findViewById(R.id.btn_attach);
        text_send = findViewById(R.id.text_send);
        //init permissions array
        cameraPermissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        audioPermissions = new String[]{Manifest.permission.RECORD_AUDIO};
        intent = getIntent();
        final String userid = intent.getStringExtra("userid");
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        sinchClient = Sinch.getSinchClientBuilder()
                .context(this)
                .applicationKey("de3b3bf6-fff6-4d40-9c25-ba9b848861e2")
                .applicationSecret("B7u+d2DqnEuUU5JNjN2uJw==")
                .environmentHost("clientapi.sinch.com")
                .userId(fuser.getUid())
                .build();
        sinchClient.setSupportActiveConnectionInBackground(true);
        sinchClient.startListeningOnActiveConnection();
        sinchClient.setSupportCalling(true);
        sinchClient.getCallClient().addCallClientListener(new sinchCallClientListener()
        {


        });
        sinchClient.start();

        btn_attach.setOnClickListener(v -> showImagePickDialog());

        btn_send.setOnClickListener(v -> {
            notify = true;
            String msg = text_send.getText().toString();
            if (!msg.equals("")){
                sendMessage(fuser.getUid(),userid,msg);
            }else {
                Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
            }
            text_send.setText("");
        });


        assert userid != null;
        reference = FirebaseDatabase.getInstance().getReference("appusers").child(userid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UsersData usersData = snapshot.getValue(UsersData.class);
                assert usersData != null;
                username.setText(usersData.getUsername());
                if(usersData.getImageUrl().equals("default")){
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                }else {
                    if (!(MessageActivity.this).isFinishing()) {
                        Glide.with(MessageActivity.this).load(usersData.getImageUrl()).into(profile_image);

                    }
                }

                readMessage(fuser.getUid(), userid, usersData.getImageUrl());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        seenMessage(userid);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_menu_bar,menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.call_button:
                callUser();
                break;
            case R.id.video_call:
                //openVideoCallingActivity();
                break;
        }
        return true;
    }


    private void callUser()
    {
        if (call==null){
            final String userid = intent.getStringExtra("userid");
             call =sinchClient.getCallClient().callUser(userid);
             call.addCallListener(new SinchCallListener());
            openCallerDialog(call);
        }
    }
    private void openCallerDialog(final com.sinch.android.rtc.calling.Call call){
        AlertDialog alertDialogCall = new AlertDialog.Builder(MessageActivity.this).create();
        alertDialogCall.setTitle("Alert");
        alertDialogCall.setMessage("Calling");
        alertDialogCall.setButton(AlertDialog.BUTTON_NEUTRAL, "Hang up", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                call.hangup();
            }
        });
        alertDialogCall.show();

    }

   /*private void openVideoCallingActivity()
    {
        call = getSinchServiceInterface().callUserVideo(userid);
        String callId = call.getCallId();
        Intent jumptocall= new Intent(MessageActivity.this,CallingActivityVideo.class);
        jumptocall.putExtra(SinchService.CALL_ID, callId);
        jumptocall.putExtra("UserId", userid);
        startActivity(jumptocall);
    }
       public class Videocall implements VideoCallListener {

        @Override
        public void onVideoTrackAdded(com.sinch.android.rtc.calling.Call call) {
            VideoController vc = sinchClient.getVideoController();
            View myPreview = vc.getLocalView();
            View remoteView = vc.getRemoteView();
        }

        @Override
        public void onVideoTrackPaused(com.sinch.android.rtc.calling.Call call) {

        }

        @Override
        public void onVideoTrackResumed(com.sinch.android.rtc.calling.Call call) {

        }

        @Override
        public void onCallProgressing(com.sinch.android.rtc.calling.Call call) {

        }

        @Override
        public void onCallEstablished(com.sinch.android.rtc.calling.Call call) {

        }

        @Override
        public void onCallEnded(com.sinch.android.rtc.calling.Call call) {

        }

       @Override
        public void onShouldSendPushNotification(com.sinch.android.rtc.calling.Call call, List<PushPair> list) {

        }
  }*/
    private void seenMessage(final String userId){
        reference = FirebaseDatabase.getInstance().getReference("chats");

        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat  = snapshot.getValue(Chat.class);

                    assert chat != null;
                    if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(userId)) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        snapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(String sender, final String receiver, String message){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender",sender);
        hashMap.put("receiver",receiver);
        hashMap.put("message",message);
        hashMap.put("type","text");

        reference.child("chats").push().setValue(hashMap);

        final String userid = intent.getStringExtra("userid");
        //Add user to chat fragment

        assert userid != null;
        final DatabaseReference chatref = FirebaseDatabase.getInstance().getReference("chatlist")
                .child(fuser.getUid())
                .child(userid);
        chatref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatref.child("id").setValue(userid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //notifications

        final String msg = message;

        reference = FirebaseDatabase.getInstance().getReference("appusers").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UsersData user = dataSnapshot.getValue(UsersData.class);
                if (notify) {
                    sendNotifiaction(receiver, user.getUsername(), msg);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void sendNotifiaction(String receiver, final String username, final String message){
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(fuser.getUid(), R.drawable.chatlogo, username+": "+message, "New Message",
                            userid);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().succes != 1){
                                           // Toast.makeText(MessageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void currentUser(String userid){
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userid);
        editor.apply();
    }

    private void readMessage(final String myid, final String userid, final String imageurl){
        mChat = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                mChat.clear();
                for (DataSnapshot snapshot : datasnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(myid)){
                        mChat.add(chat);
                    }
                    messageAdapter = new MessageAdapter(MessageActivity.this,mChat,imageurl);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void status(String status){
        reference = FirebaseDatabase.getInstance().getReference("appusers").child(fuser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status",status);
        reference.updateChildren(hashMap);
    }

    private void showImagePickDialog(){
        String [] options = {"Camera","Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("choose image from");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which==0){
                    //camera clicked
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }else {
                        pickFromCamera();
                    }
                }
                if (which==1){
                    //gallery clicked
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }else {
                        pickFromGallery();

                    }
                }
            }
        });
        builder.create().show();
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_COD);

    }

    private void pickFromCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE,"Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"Temp Descr");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(intent,IMAGE_PICK_CAMERA_COD);


    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;
    }
    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_COD);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);

        return result && result1;


    }
    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_COD);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        status("online");
        currentUser(userid);

    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
        currentUser("none");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_REQUEST_COD:{
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1]==PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted&&storageAccepted){
                        pickFromCamera();
                    }else {
                        Toast.makeText(this, "Camera & Storage permission both are necessary ", Toast.LENGTH_SHORT).show();
                    }
                }
                else {

                }
            }
            break;
            case STORAGE_REQUEST_COD:{
                if (grantResults.length>0){
                    boolean storageAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted){
                        pickFromGallery();
                    }else {
                        Toast.makeText(this, "Storage permission necessary ", Toast.LENGTH_SHORT).show();
                    }
                }
                else {

                }

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode== RESULT_OK){
            if (requestCode==IMAGE_PICK_GALLERY_COD){
                image_uri = data.getData();
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (requestCode == IMAGE_PICK_CAMERA_COD){
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                    }
        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendImageMessage(Uri image_uri) throws IOException{
        new Thread(new Runnable() {
        @Override
        public void run() { runOnUiThread(new Runnable() {
                @Override
                public void run() { notify= true;
        final ProgressDialog progressDialog = new ProgressDialog(MessageActivity.this);
        progressDialog.setMessage("Sending image...");
        progressDialog.show();
        String timeStamp = ""+ System.currentTimeMillis();
        String fileNamePath = "ChatImages/"+"post_"+timeStamp;
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(MessageActivity.this.getContentResolver(),image_uri);
                    } catch (IOException e) { e.printStackTrace(); }  //your code or your request that you want to run on uiThread
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
                        byte[] data = baos.toByteArray();
                        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNamePath);
                        ref.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        //image uploaded
                                        progressDialog.dismiss();
                                        //get url to upload image
                                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                        while (!uriTask.isSuccessful());
                                        String downloadUri = uriTask.getResult().toString();
                                        if (uriTask.isSuccessful()){

                                            final String userid1 = intent.getStringExtra("userid");
                                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                                            HashMap<String, Object> hashMap2 = new HashMap<>();
                                            hashMap2.put("sender",fuser.getUid());
                                            hashMap2.put("receiver",userid1);
                                            hashMap2.put("message",downloadUri);
                                            hashMap2.put("type","image");

                                            databaseReference.child("chats").push().setValue(hashMap2);


                                            DatabaseReference database = FirebaseDatabase.getInstance().getReference("appusers").child(fuser.getUid());
                                            database.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                                                    UsersData usersData = datasnapshot.getValue(UsersData.class);
                                                    if (notify){
                                                        sendNotifiaction(userid1,usersData.getUsername(),"Sent you a photo");
                                                    }
                                                    notify = false;
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });


                                        }

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                    }
                                });
                    }
                });
            }
        }).start();

    }

    private class SinchCallListener implements CallListener{

        @Override
        public void onCallProgressing(com.sinch.android.rtc.calling.Call call) {
            Toast.makeText(MessageActivity.this, "Call Progressing", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onCallEstablished(com.sinch.android.rtc.calling.Call call) {
            Toast.makeText(MessageActivity.this, "Call Established", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEnded(com.sinch.android.rtc.calling.Call endedcall) {
            Toast.makeText(MessageActivity.this, "Call Ended", Toast.LENGTH_SHORT).show();
            call = null;
            endedcall.hangup();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);



        }

        @Override
        public void onShouldSendPushNotification(com.sinch.android.rtc.calling.Call call, List<PushPair> list) {

        }
    }
    private class sinchCallClientListener implements CallClientListener {


        @Override
        public void onIncomingCall(CallClient callClient, final com.sinch.android.rtc.calling.Call incomingcall) {
            AlertDialog alertDialog = new AlertDialog.Builder(MessageActivity.this).create();
            alertDialog.setTitle("Calling");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "REJECT", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    call.hangup();
                }
            });

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Pick", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    call = incomingcall;
                    call.answer();
                    call.addCallListener(new SinchCallListener());
                    Toast.makeText(getApplicationContext(), "Call is Started", Toast.LENGTH_SHORT).show();

                }
            });
            alertDialog.show();

        }
    }
}

