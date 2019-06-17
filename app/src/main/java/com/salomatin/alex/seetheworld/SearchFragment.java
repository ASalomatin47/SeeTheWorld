package com.salomatin.alex.seetheworld;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.salomatin.alex.seetheworld.api.APIInterface;
import com.salomatin.alex.seetheworld.api.ApiClient;
import com.salomatin.alex.seetheworld.models.PlaceModel;
import com.salomatin.alex.seetheworld.responses.DistanceResponse;
import com.salomatin.alex.seetheworld.responses.PlaceResponse;
import com.salomatin.alex.seetheworld.models.Place_details;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.MODE_PRIVATE;

public class SearchFragment extends Fragment {

    // Contains search tabs (nearby + text search)

    public interface OnPlaceSelectedListener {
        public void onPlaceSelected(PlaceModel selectedPlaceInfo);
    }

    private OnPlaceSelectedListener mListener;

    public void setOnPlaceSelectedListener(OnPlaceSelectedListener listener) {
        mListener = listener;
    }


    private static final String TAG = SearchFragment.class.getSimpleName();
    private static final String SEARCH_MODE_KEY = "SEARCH_MODE_KEY";
    private static final int LOCATION_SEARCH_MODE = 0;
    private static final int TEXT_SEARCH_MODE = 1;
    public static final String PREFS_FILE_NAME = "sharedPreferences";

    private int mSearchMode;

    private RelativeLayout mSearchTextContainer;
    private AutoCompleteTextView mSearchText;
    private ProgressBar mProgressBar;
    private APIInterface mApiService;
    private RecyclerView recyclerView;

    private ArrayList<PlaceResponse.PlaceSearchRes> results;

    protected Location mLastLocation;

    public ArrayList<PlaceModel> details_model;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private static final int MY_PERMISION_CODE = 10;

    private boolean Permission_is_granted = false;
    public String mAddressOutput;

    public String latLngString;
    public double source_lat, source_long;

    private long radius = 3 * 1000;
    private TextView locationTextView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search_fragment, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Widgets
        locationTextView = view.findViewById(R.id.location_tv);

        mApiService = ApiClient.getClient().create(APIInterface.class);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

        mSearchTextContainer = view.findViewById(R.id.search_container);
        mSearchText = view.findViewById(R.id.input_search);

        final TextView locationTab = view.findViewById(R.id.button_location);
        final TextView searchTab = view.findViewById(R.id.button_text);

        mSearchText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            getUserLocation();
                            final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });


        locationTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationTab.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                searchTab.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                if (mSearchTextContainer.getVisibility() == View.VISIBLE) {
                    mSearchTextContainer.setVisibility(View.GONE);
                    mSearchText.setText("");
                }
                mSearchMode = LOCATION_SEARCH_MODE;
                getUserLocation();
            }
        });

        searchTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTab.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                locationTab.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                if (mSearchTextContainer.getVisibility() == View.GONE) {
                    mSearchTextContainer.setVisibility(View.VISIBLE);
                }
                mSearchMode = TEXT_SEARCH_MODE;
            }
        });

        mProgressBar = view.findViewById(R.id.progress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mProgressBar.setProgress(0, true);
        } else {
            mProgressBar.setProgress(0);
        }
        // Get selected Search mode (nearby or text search) from Shared Preferences
        mSearchMode = getContext().getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getInt(SEARCH_MODE_KEY, LOCATION_SEARCH_MODE);
        if (mSearchMode == LOCATION_SEARCH_MODE) {
            locationTab.callOnClick();
        } else {
            searchTab.callOnClick();
        }

        recyclerView = view.findViewById(R.id.recyclerView);

        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(true);


        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check network connection
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            getUserLocation();
            showSnack(true);
        } else {
            Log.d(TAG, "onResume: MapFragment: No network connection");
            mProgressBar.setVisibility(View.GONE);
            showSnack(false);
            if (getContext() != null) {
                String jsonLastHistory = getContext().getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getString("HISTORY_PLACES", "");
                Log.d(TAG, "onResume: History" + jsonLastHistory);
                if (!("").equals(jsonLastHistory)) {
                    Gson gson = new Gson();
                    ArrayList<PlaceModel> historyList = new ArrayList<>(Arrays.asList(gson.fromJson(jsonLastHistory, PlaceModel[].class)));
                    Rv_adapter placesAdapter = new Rv_adapter(getContext(), historyList);
                    recyclerView.setAdapter(placesAdapter);
                    placesAdapter.notifyDataSetChanged();
                }

            }
        }
    }

    // Get Text Search results
    private void fetchPlacesByTextSearch(String placeType) {
        if (getContext() != null) {
            if (mProgressBar.getVisibility() != View.VISIBLE) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
            Call<PlaceResponse.Root> call = mApiService.getPlacesFromTextSearch(placeType, ApiClient.GOOGLE_PLACE_API_KEY);
            call.enqueue(new Callback<PlaceResponse.Root>() {
                @Override
                public void onResponse(Call<PlaceResponse.Root> call, Response<PlaceResponse.Root> response) {
                    PlaceResponse.Root root = (PlaceResponse.Root) response.body();

                    if (response.isSuccessful()) {

                        switch (root.status) {
                            case "OK":

                                results = root.placeSearchRes;

                                details_model = new ArrayList<PlaceModel>();
                                String photourl;
                                Log.i(TAG, "Getting results from text search.");


                                for (int i = 0; i < results.size(); i++) {

                                    PlaceResponse.PlaceSearchRes info = (PlaceResponse.PlaceSearchRes) results.get(i);

                                    String place_id = results.get(i).place_id;


                                    if (results.get(i).photos != null) {

                                        String photo_reference = results.get(i).photos.get(0).photo_reference;

                                        photourl = ApiClient.base_url + "place/photo?maxwidth=100&photoreference=" + photo_reference +
                                                "&key=" + ApiClient.GOOGLE_PLACE_API_KEY;

                                    } else {
                                        photourl = "NA";
                                    }

                                    fetchDistance(info, place_id, photourl);


                                    Log.i("Coordinates  ", info.geometry.locationA.lat + " , " + info.geometry.locationA.lng);
                                    Log.i("Names  ", info.name);

                                }

                                break;
                            case "ZERO_RESULTS":
                                Toast.makeText(getContext(), R.string.error_no_places, Toast.LENGTH_SHORT).show();
                                mProgressBar.setVisibility(View.GONE);
                                break;
                            case "OVER_QUERY_LIMIT":
                                Toast.makeText(getContext(), R.string.error_request_limit, Toast.LENGTH_SHORT).show();
                                mProgressBar.setVisibility(View.GONE);
                                break;
                            default:
                                Toast.makeText(getContext(), R.string.error_general + response.code(), Toast.LENGTH_SHORT).show();
                                mProgressBar.setVisibility(View.GONE);
                                break;
                        }

                    } else if (response.code() != 200) {
                        Toast.makeText(getContext(), R.string.error_general + response.code(), Toast.LENGTH_SHORT).show();
                    }


                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    Toast.makeText(getContext(), R.string.error_getting_places, Toast.LENGTH_SHORT).show();
                    call.cancel();
                }
            });
        }
    }

    // Get nearby places from location
    private void getPlaces(String placeType) {

        if (getContext() != null) {
            Call<PlaceResponse.Root> call = mApiService.doPlaces(latLngString, radius, placeType, ApiClient.GOOGLE_PLACE_API_KEY);
            call.enqueue(new Callback<PlaceResponse.Root>() {
                @Override
                public void onResponse(Call<PlaceResponse.Root> call, Response<PlaceResponse.Root> response) {
                    PlaceResponse.Root root = (PlaceResponse.Root) response.body();


                    if (response.isSuccessful()) {

                        switch (root.status) {
                            case "OK":

                                results = root.placeSearchRes;

                                details_model = new ArrayList<PlaceModel>();
                                String photourl;
                                Log.i(TAG, "Getting nearby places from location.");


                                for (int i = 0; i < results.size(); i++) {

                                    PlaceResponse.PlaceSearchRes info = (PlaceResponse.PlaceSearchRes) results.get(i);

                                    String place_id = results.get(i).place_id;


                                    if (results.get(i).photos != null) {

                                        String photo_reference = results.get(i).photos.get(0).photo_reference;

                                        photourl = ApiClient.base_url + "place/photo?maxwidth=100&photoreference=" + photo_reference +
                                                "&key=" + ApiClient.GOOGLE_PLACE_API_KEY;

                                    } else {
                                        photourl = "NA";
                                    }

                                    fetchDistance(info, place_id, photourl);


                                    Log.i("Coordinates  ", info.geometry.locationA.lat + " , " + info.geometry.locationA.lng);
                                    Log.i("Names  ", info.name);

                                }

                                break;
                            case "ZERO_RESULTS":
                                Toast.makeText(getContext(), R.string.error_no_places, Toast.LENGTH_SHORT).show();
                                mProgressBar.setVisibility(View.GONE);
                                break;
                            case "OVER_QUERY_LIMIT":
                                Toast.makeText(getContext(), R.string.error_request_limit, Toast.LENGTH_SHORT).show();
                                mProgressBar.setVisibility(View.GONE);
                                break;
                            default:
                                mProgressBar.setVisibility(View.GONE);
                                break;
                        }

                    } else if (response.code() != 200) {
                        Toast.makeText(getContext(), R.string.error_general + response.code(), Toast.LENGTH_SHORT).show();
                    }


                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    Toast.makeText(getContext(), R.string.error_getting_places, Toast.LENGTH_SHORT).show();
                    call.cancel();
                }
            });
        }
    }

    // Set distance from user (for nearby info panel)
    private void fetchDistance(final PlaceResponse.PlaceSearchRes info, final String place_id, final String photourl) {

        if (getContext() != null) {
            Log.i(TAG, "Distance API call start");

            String units;
            if (getContext().getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean("KM_OR_MILES", true)) {
                units = "km";
            } else {
                units = "imperial";
            }

            Call<DistanceResponse> call = mApiService.getDistance(units, latLngString, info.geometry.locationA.lat + "," + info.geometry.locationA.lng, ApiClient.GOOGLE_PLACE_API_KEY);

            call.enqueue(new Callback<DistanceResponse>() {
                @Override
                public void onResponse(Call<DistanceResponse> call, Response<DistanceResponse> response) {

                    DistanceResponse resultDistance = (DistanceResponse) response.body();

                    if (response.isSuccessful()) {

                        Log.i(TAG, resultDistance.status);

                        if ("OK".equalsIgnoreCase(resultDistance.status)) {
                            DistanceResponse.InfoDistance row1 = resultDistance.rows.get(0);
                            DistanceResponse.InfoDistance.DistanceElement element1 = row1.elements.get(0);

                            if ("OK".equalsIgnoreCase(element1.status)) {

                                DistanceResponse.InfoDistance.ValueItem itemDistance = element1.distance;

                                String total_distance = itemDistance.text;

                                fetchPlace_details(info, place_id, total_distance, info.name, photourl);
                            }


                        }

                    } else if (response.code() != 200) {
                        Toast.makeText(getContext(), R.string.error_general + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    Toast.makeText(getContext(), R.string.error_getting_places, Toast.LENGTH_SHORT).show();
                    call.cancel();
                }
            });
        }
    }

    private void fetchPlace_details(final PlaceResponse.PlaceSearchRes info, final String place_id, final String totaldistance, final String name, final String photourl) {
        if (getContext() != null) {
            Call<Place_details> call = mApiService.getPlaceDetails(place_id, ApiClient.GOOGLE_PLACE_API_KEY);
            call.enqueue(new Callback<Place_details>() {
                @Override
                public void onResponse(Call<Place_details> call, Response<Place_details> response) {

                    Place_details details = (Place_details) response.body();

                    if ("OK".equalsIgnoreCase(details.status)) {

                        String address = details.result.formatted_adress;
                        String phone = details.result.international_phone_number;

                        details_model.add(new PlaceModel(address, phone, totaldistance, name, photourl));

                        Log.i("Place details: ", info.name + "  " + address);

                        if (details_model.size() == results.size()) {

                            Collections.sort(details_model, new Comparator<PlaceModel>() {
                                @Override
                                public int compare(PlaceModel lhs, PlaceModel rhs) {
                                    return lhs.distance.compareTo(rhs.distance);
                                }
                            });

                            mProgressBar.setVisibility(View.GONE);
                            Rv_adapter placesAdapter = new Rv_adapter(getContext(), details_model);
                            recyclerView.setAdapter(placesAdapter);
                            placesAdapter.notifyDataSetChanged();
                        }
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    call.cancel();
                }
            });
        }
    }

    // Get estimated user location address (to show in serch tab)
    private void fetchCurrentAddress(final String latLngString) {

        if (getContext() != null) {
            Call<Place_details> call = mApiService.getCurrentAddress(latLngString, ApiClient.GOOGLE_PLACE_API_KEY);
            call.enqueue(new Callback<Place_details>() {
                @Override
                public void onResponse(Call<Place_details> call, Response<Place_details> response) {

                    Place_details details = (Place_details) response.body();

                    if ("OK".equalsIgnoreCase(details.status)) {

                        mAddressOutput = details.results.get(0).formatted_adress;
                        locationTextView.setText(mAddressOutput);
                        Log.i("Current address: ", mAddressOutput + latLngString);
                    }

                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    call.cancel();
                }
            });
        }
    }

    // Get user location, show results based on location
    private void getUserLocation() {
        if (mProgressBar.getVisibility() != View.VISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        if (ActivityCompat.checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        ACCESS_COARSE_LOCATION)) {
                    showAlert();

                } else {

                    if (isFirstTimeAskingPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                        firstTimeAskingPermission(getContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION, false);
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                                MY_PERMISION_CODE);
                    } else {
                        Toast.makeText(getContext(), R.string.error_permission_denied, Toast.LENGTH_LONG).show();
                        mProgressBar.setVisibility(View.GONE);
                    }
                }


            } else Permission_is_granted = true;
        } else {
            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    if (location != null) {

                        mLastLocation = location;
                        source_lat = location.getLatitude();
                        source_long = location.getLongitude();
                        latLngString = location.getLatitude() + "," + location.getLongitude();
                        fetchCurrentAddress(latLngString);

                        Log.i(TAG, latLngString + "");

                        if (mSearchMode == LOCATION_SEARCH_MODE) {
                            getPlaces("restaurant, amusement_park, art_gallery, zoo, synagogue, stadium, shopping_mall, night_club, library");
                        } else {
                            String searchText = mSearchText.getText().toString();
                            if (TextUtils.isEmpty(searchText)) {
                                if (mProgressBar.getVisibility() == View.VISIBLE) {
                                    mProgressBar.setVisibility(View.GONE);
                                }
                                return;
                            }
                            fetchPlacesByTextSearch(searchText);
                        }
                    } else {
                        mProgressBar.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), R.string.error_getting_places, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // Ask service permissions
    public static void firstTimeAskingPermission(Context context, String permission, boolean isFirstTime) {
        SharedPreferences sharedPreference = context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
        sharedPreference.edit().putBoolean(permission, isFirstTime).apply();
    }

    public static boolean isFirstTimeAskingPermission(Context context, String permission) {
        return context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean(permission, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("On request permissions ", "executed");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case MY_PERMISION_CODE:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Permission_is_granted = true;
                    fetchData();
                } else {
                    showAlert();
                    Permission_is_granted = false;
                    Toast.makeText(getContext(), R.string.error_gps, Toast.LENGTH_LONG).show();
                    mProgressBar.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void fetchData() {
        if (mSearchMode == LOCATION_SEARCH_MODE) {
            getUserLocation();
        }
    }


    // Show dialog with GPS request
    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(R.string.dialog_location_title)
                .setMessage(R.string.dialog_location_message)
                .setPositiveButton(R.string.dialog_location_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                                MY_PERMISION_CODE);


                    }
                })
                .setNegativeButton(R.string.dialog_location_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    // Show snackbar (connected or disconnected)
    public void showSnack(boolean isConnected) {
        int message;
        int color;
        if (isConnected) {
            message = R.string.snack_network_connected;
            color = Color.WHITE;
            fetchData();
        } else {
            message = R.string.snack_network_disconnected;
            color = Color.RED;
        }

        Snackbar snackbar = Snackbar
                .make(getActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);

        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(color);
        snackbar.show();

    }

    // Set long-click dialog (with share intent & favorites)
    private AlertDialog dialog = null;

    public void showLongClickDialog(final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.list_item_long_click_dialog, null);
        dialogView.findViewById(R.id.dialog_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Geocoder geocoder = new Geocoder(getContext());
                String placeAddress = details_model.get(position).address;
                String name = details_model.get(position).name;
                List<Address> list = new ArrayList<>();
                try {
                    list = geocoder.getFromLocationName(placeAddress, 1);
                } catch (IOException e) {
                    Log.d(TAG, "geoLocate: IOException: " + e.getMessage());
                }

                if (list.size() > 0) {
                    Address address = list.get(0);
                    Log.d(TAG, "geoLocate: location found: " + address.toString());
                    createShareLocationIntent(new LatLng(address.getLatitude(), address.getLongitude()), name);
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialogView.findViewById(R.id.dialog_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save location to favorites (via Shared Preferences)
                Toast.makeText(getContext(), R.string.favorites_toast, Toast.LENGTH_SHORT).show();
                Gson gson = new Gson();
                PlaceModel placeInfo = details_model.get(position);
                String jsonString = getContext().getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getString("FAVORITES_PLACES", "");
                ArrayList<PlaceModel> favoritePlacesList;
                if (("").equals(jsonString)) {
                    favoritePlacesList = new ArrayList<>();

                } else {
                    favoritePlacesList = new ArrayList<>(Arrays.asList(gson.fromJson(jsonString, PlaceModel[].class)));
                }
                favoritePlacesList.add(placeInfo);
                jsonString = gson.toJson(favoritePlacesList);
                getContext().getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).edit().putString("FAVORITES_PLACES", jsonString).commit();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        builder.setView(dialogView);
        dialog = builder.create();
        dialog.show();
    }

    // Share location
    private void createShareLocationIntent(LatLng latLng, String description) {
        Double latitude = latLng.latitude;
        Double longitude = latLng.longitude;
        String uri = "http://maps.google.com/maps?daddr=" + latitude + "," + longitude;

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, description);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, uri);
        startActivity(Intent.createChooser(sharingIntent, "Share via..."));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            // Save Shared Preferences
            Log.d("Test", "Search Fragment paused.");
            getContext().getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).edit().putInt(SEARCH_MODE_KEY, mSearchMode).commit();
            if (details_model != null && details_model.size() > 0) {
                Gson gson = new Gson();
                String jsonString = gson.toJson(details_model);
                getContext().getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).edit().putString("HISTORY_PLACES", jsonString).commit();
            }
        }
    }

    // RecyclerView adapter
    public class Rv_adapter extends RecyclerView.Adapter<Rv_adapter.MyViewHolder> {

        private ArrayList<PlaceModel> placeModels;
        private Context context;

        public Rv_adapter(Context context, ArrayList<PlaceModel> placeModels) {

            this.context = context;
            this.placeModels = placeModels;
        }


        @Override
        public Rv_adapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_listitem, parent, false);

            return new Rv_adapter.MyViewHolder(itemView, viewType);
        }

        @Override
        public void onBindViewHolder(Rv_adapter.MyViewHolder holder, int position) {

            holder.place_name.setText(placeModels.get(holder.getAdapterPosition()).name);

            Picasso.with(context).load(placeModels.get(holder.getAdapterPosition()).photourl)
                    .placeholder(R.drawable.placeholder).into(holder.place_image);


            holder.place_address.setText(placeModels.get(holder.getAdapterPosition()).address);

            if (placeModels.get(holder.getAdapterPosition()).phone_no == null) {
                holder.place_phone.setText(R.string.general_na);
            } else
                holder.place_phone.setText(placeModels.get(holder.getAdapterPosition()).phone_no);


            holder.place_distance.setText(placeModels.get(holder.getAdapterPosition()).distance);

            Log.i("Adapter Details: ", placeModels.get(holder.getAdapterPosition()).name + "  " +
                    placeModels.get(holder.getAdapterPosition()).address +
                    "  " + placeModels.get(holder.getAdapterPosition()).distance);

        }

        @Override
        public int getItemCount() {

            return placeModels.size();

        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

            TextView place_name;
            TextView place_address;
            TextView place_phone;
            TextView place_distance;
            ImageView place_image;

            int view_type;

            public MyViewHolder(final View itemView, final int viewType) {
                super(itemView);

                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
                view_type = 1;
                this.place_name = itemView.findViewById(R.id.name);
                this.place_address = itemView.findViewById(R.id.address);
                this.place_phone = itemView.findViewById(R.id.phone);
                this.place_distance = itemView.findViewById(R.id.distance);
                this.place_image = itemView.findViewById(R.id.loc_image);
            }


            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: " + placeModels.get(getAdapterPosition()).address);
                if (view_type == 1) {
                    mListener.onPlaceSelected(placeModels.get(getAdapterPosition()));
                }
            }

            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "onLongClick: " + placeModels.get(getAdapterPosition()).address);
                showLongClickDialog(getAdapterPosition());
                return true;
            }
        }
    }
}