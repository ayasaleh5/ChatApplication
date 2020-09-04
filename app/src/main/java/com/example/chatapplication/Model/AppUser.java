package com.example.chatapplication.Model;


import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class AppUser {

    String username , useremail , usermobilenumber,imageUrl,id,status ;

    public AppUser() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AppUser(String username, String useremail, String usermobilenumber, String imageUrl, String id,String status) {
        this.username = username;
        this.useremail = useremail;
        this.usermobilenumber = usermobilenumber;
        this.imageUrl=imageUrl;
        this.id = id;
        this.status = status;
    }
    public String getImageUrl() {
        return imageUrl;
    }


    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUseremail() {
        return useremail;
    }

    public void setUseremail(String useremail) {
        this.useremail = useremail;
    }

    public String getUsermobilenumber() {
        return usermobilenumber;
    }

    public void setUsermobilenumber(String usermobilenumber) {
        this.usermobilenumber = usermobilenumber;
    }
}
