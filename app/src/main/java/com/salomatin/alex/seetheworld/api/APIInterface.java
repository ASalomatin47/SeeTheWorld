package com.salomatin.alex.seetheworld.api;

import com.salomatin.alex.seetheworld.responses.DistanceResponse;
import com.salomatin.alex.seetheworld.responses.PlaceResponse;
import com.salomatin.alex.seetheworld.models.Place_details;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface APIInterface {

    @GET("place/nearbysearch/json?")
    Call<PlaceResponse.Root> doPlaces(@Query(value = "location", encoded = true) String location, @Query(value = "radius", encoded = true) long radius, @Query(value = "type", encoded = true) String type, @Query(value = "key", encoded = true) String key);


    @GET("distancematrix/json?")
    Call<DistanceResponse> getDistance(@Query(value = "units", encoded = true) String units, @Query(value = "origins", encoded = true) String origins, @Query(value = "destinations", encoded = true) String destinations, @Query(value = "key", encoded = true) String key);

    @GET("place/details/json?")
    Call<Place_details> getPlaceDetails(@Query(value = "placeid", encoded = true) String placeid, @Query(value = "key", encoded = true) String key);

    @GET("geocode/json?")
    Call<Place_details> getCurrentAddress(@Query(value = "latlng", encoded = true) String latlng, @Query(value = "key", encoded = true) String key);

    @GET("place/textsearch/json?")
    Call<PlaceResponse.Root> getPlacesFromTextSearch(@Query(value = "query", encoded = true) String textToSearch, @Query(value = "key", encoded = true) String key);
}
