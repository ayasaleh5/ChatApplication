package com.example.chatapplication;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.chatapplication.Model.UsersData;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingActivity extends AppCompatActivity {
private Button btSave;
private TextView tvname;
private CircleImageView profileImageView;

 FirebaseUser fuser;
 DatabaseReference reference;
 StorageReference storageReference;
 private static final int IMAGE_REQUEST = 1;
 private Uri imageUri;
 private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        tvname = findViewById(R.id.tvname);
        profileImageView = findViewById(R.id.profile_image6);
        Toolbar toolbar = findViewById(R.id.toolBar3);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> startActivity(new Intent(SettingActivity.this, ProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("appusers").child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                UsersData user = datasnapshot.getValue(UsersData.class);
                assert user != null;
                tvname.setText(user.getUsername());
                if (user.getImageUrl().equals("default")) {
                    profileImageView.setImageResource(R.mipmap.ic_launcher);

                } else {
                    //vip about profile fragment
                    if(!(SettingActivity.this).isFinishing())
                        Glide.with(SettingActivity.this).load(user.getImageUrl()).into(profileImageView);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        profileImageView.setOnClickListener(v -> new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                openImage();

            }
        },2000));

    }


    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,IMAGE_REQUEST);
    }
    private String getFileExtention(Uri uri){
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }


    private void uploadImage(){
        new Handler().postDelayed(() -> {


    //checking if file is available
    if (imageUri !=null){
        //displaying progress dialog while image is uploading
        final ProgressDialog pd = new ProgressDialog(SettingActivity.this);
        pd.setMessage("Uploading");
       pd.show();
        //getting the storage reference
        final StorageReference fileReferance = storageReference.child(System.currentTimeMillis()
                + "."+getFileExtention(imageUri));

        //adding the file to reference
        uploadTask = fileReferance.putFile(imageUri);
        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then (@NonNull Task <UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()){
                    throw task.getException();
                }

                return fileReferance.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()){
                    Uri downloadUri = task.getResult();
                    String mUri = downloadUri.toString();

                    reference = FirebaseDatabase.getInstance().getReference("appusers").child(fuser.getUid());
                    HashMap<String,Object> map = new HashMap<>();
                    map.put("imageUrl",mUri);
                    Task<Void> voidTask = reference.updateChildren(map);
                    pd.dismiss();
                } else {
                    Toast.makeText(getApplicationContext(), "Faild", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }
        });

    } else {
        Toast.makeText(getApplicationContext(), "No image selected", Toast.LENGTH_SHORT).show();
    }
        },2000);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data !=null && data.getData() !=null){
            imageUri = data.getData();
            if (uploadTask !=null && uploadTask.isInProgress()){
                Toast.makeText(getApplicationContext(), "Upload is in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }

        }

    }

}



