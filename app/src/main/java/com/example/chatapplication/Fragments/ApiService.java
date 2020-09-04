package com.example.chatapplication.Fragments;

import com.example.chatapplication.Notifications.MyResponse;
import com.example.chatapplication.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers(
            {
                   "Content-Type:application/json",
            "Authorization:Key=AAAATRQO5TQ:APA91bFsLrQHDUmDtPKUhJfd4nsVKYuz6ShSk3sRPlo77nQ0v3cPjPnZTIQFWB8mQqYpzK_zQz39SS-OLff58-hBhsjPgBeJnKZpAcRowVD9Oluyg-q3iswmdp_c7GdlAliVTplq3IYU"
            }
    )
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
