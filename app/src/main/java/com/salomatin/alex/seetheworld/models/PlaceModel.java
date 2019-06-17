package com.salomatin.alex.seetheworld.models;

public class PlaceModel {

    public String address, phone_no, distance, name, photourl;
    public float rating;


    public PlaceModel(String address, String phone_no, String distance, String name, String photurl) {
        this.address = address;
        this.phone_no = phone_no;
        this.rating = rating;
        this.distance = distance;
        this.name = name;
        this.photourl = photurl;
    }

}
