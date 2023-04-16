package com.example.newbeacon;

public class ModelUser {

    String name, username, email, search, image, uid, location, atype;

    public ModelUser() {

    }

    public ModelUser(String name, String username, String email, String search, String image, String uid, String location, String atype) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.search = search;
        this.image = image;
        this.uid = uid;
        this.location = location;
        this.atype = atype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAtype() {
        return atype;
    }

    public void setAtype(String atype) {
        this.atype = atype;
    }
}