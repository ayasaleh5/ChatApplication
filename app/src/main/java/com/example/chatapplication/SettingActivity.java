package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.squareup.picasso.Picasso;

import java.util.HashMap;

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

        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("appusers").child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                UsersData user = datasnapshot.getValue(UsersData.class);
                tvname.setText(user.getUsername());
                if (user.getImageUrl().equals("default")){
                    profileImageView.setImageResource(R.mipmap.ic_launcher);
                }else {
                    Glide.with(SettingActivity.this).load(user.getImageUrl()).into(profileImageView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });


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
        final ProgressDialog pd = new ProgressDialog(getApplicationContext());
        pd.setMessage("Uploading");
        pd.show();
        if (imageUri !=null){
            final StorageReference fileReferance = storageReference.child(System.currentTimeMillis()
            + "."+getFileExtention(imageUri));

            uploadTask = fileReferance.getFile(imageUri);
            uploadTask.continueWith(new Continuation <UploadTask.TaskSnapshot,Task<Uri>> () {
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
                        HashMap<String,Object>map = new HashMap<>();
                        map.put("imageUrl",mUri);
                        reference.updateChildren(map);
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
        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
        && data !=null && data.getData() !=null){
            imageUri = data.getData();


        }

    }
}



