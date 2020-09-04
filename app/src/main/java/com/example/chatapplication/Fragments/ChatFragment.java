package com.example.chatapplication.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.chatapplication.Adapter.UserAdapter;
import com.example.chatapplication.Model.AppUser;
import com.example.chatapplication.Model.Chat;
import com.example.chatapplication.Model.ChatList;
import com.example.chatapplication.Model.UsersData;
import com.example.chatapplication.Notifications.Token;
import com.example.chatapplication.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;

    private UserAdapter userAdapter;
    private List<UsersData> mUser;
    FirebaseUser fuser;
    DatabaseReference reference;
    private List<ChatList> usersList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        usersList = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("chatlist").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                usersList.clear();
                for (DataSnapshot snapshot : datasnapshot.getChildren()) {
                    ChatList chatList = snapshot.getValue(ChatList.class);
                    usersList.add(chatList);
                }
                chatList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        updateToken(FirebaseInstanceId.getInstance().getToken());
        return view;
    }
    // notifications
    public void updateToken(String token){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(token);
        reference.child(fuser.getUid()).setValue(token1);
    }

    private void chatList() {
        mUser = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("appusers");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                mUser.clear();
                for (DataSnapshot snapshot : datasnapshot.getChildren()) {
                    UsersData user = snapshot.getValue(UsersData.class);
                    for (ChatList chatList:usersList){
                        if (user.getId().equals(chatList.getId())){
                            mUser.add(user);
                        }
                    }


                }
                userAdapter = new UserAdapter(getContext(),mUser,true);
                recyclerView.setAdapter(userAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
}
