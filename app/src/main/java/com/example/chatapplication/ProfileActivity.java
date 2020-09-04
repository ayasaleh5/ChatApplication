package com.example.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.example.chatapplication.Fragments.ChatFragment;
import com.example.chatapplication.Fragments.ProfileFragment;
import com.example.chatapplication.Fragments.UsersFragment;
import com.example.chatapplication.Model.AppUser;
import com.example.chatapplication.Model.UsersData;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.ContentValues.TAG;

public class ProfileActivity extends AppCompatActivity {

    TextView userName;
    CircleImageView profile_image;
    DatabaseReference reference;
    String currentUserId;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth firebaseAuth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");


        userName = findViewById(R.id.tvusername);
        profile_image = findViewById(R.id.profile_image);


        firebaseAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    reference = FirebaseDatabase.getInstance().getReference("appusers").child(user.getUid());
                    reference.addValueEventListener(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            UsersData userData = snapshot.getValue(UsersData.class);
                                                            userName.setText(userData.getUsername());
                                                            String name = userData.getUsername();
                                                            if (userData.getImageUrl().equals("default")){
                                                                profile_image.setImageResource(R.mipmap.ic_launcher);
                                                            } else {
                                                                if (!(ProfileActivity.this).isFinishing()) {
                                                                    Glide.with(ProfileActivity.this).load(userData.getImageUrl()).into(profile_image);
                                                                }
                                                            }

                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            Toast.makeText(ProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

                                                        }

                                                    });
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    Intent intent = new Intent(ProfileActivity.this, StartActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                // ...
            }
        };

       TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.view_pager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new ChatFragment(), "Chats");
        viewPagerAdapter.addFragment(new UsersFragment(), "Users");
        viewPagerAdapter.addFragment(new ProfileFragment(), "Profile");

        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(ProfileActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                return true;
        }
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(ProfileActivity.this, ProfileFragment.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                return true;
        }

        return false;
    }
   class ViewPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> fragments;
        private ArrayList <String> titles;

         ViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
        public void addFragment(Fragment fragment, String title){
             fragments.add(fragment);
             titles.add(title);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }


}


    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
    }
    private void status(String status){
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        reference = FirebaseDatabase.getInstance().getReference("appusers").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status",status);
        reference.updateChildren(hashMap);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");

    }
}