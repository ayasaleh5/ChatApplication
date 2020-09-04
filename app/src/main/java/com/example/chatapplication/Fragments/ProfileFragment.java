package com.example.chatapplication.Fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.example.chatapplication.Model.UsersData;
import com.example.chatapplication.R;
import com.example.chatapplication.SettingActivity;
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

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;


public class ProfileFragment extends Fragment {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvname = view.findViewById(R.id.tvname);
        profileImageView = view.findViewById(R.id.profile_image6);

        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("appusers").child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                UsersData user = datasnapshot.getValue(UsersData.class);
                tvname.setText(user.getUsername());
                if (user.getImageUrl().equals("default")) {
                    profileImageView.setImageResource(R.mipmap.ic_launcher);

                } else {
                    //vip about profile fragment
                    if(getContext()!=null)
                    Glide.with(getActivity()).load(user.getImageUrl()).into(profileImageView);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });
        return view;
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,IMAGE_REQUEST);
    }
    private String getFileExtention(Uri uri){
        ContentResolver contentResolver = getContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }
    private void uploadImage(){

        //checking if file is available
        if (imageUri !=null){
        //displaying progress dialog while image is uploading
        final ProgressDialog pd = new ProgressDialog(getContext());
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
                        reference.updateChildren(map);
                        pd.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Faild", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });

        } else {
            Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data !=null && data.getData() !=null){
            imageUri = data.getData();
            if (uploadTask !=null && uploadTask.isInProgress()){
                Toast.makeText(getContext(), "Upload is in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }

        }

    }

}
