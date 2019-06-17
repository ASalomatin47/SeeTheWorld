package com.salomatin.alex.seetheworld.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.salomatin.alex.seetheworld.MapFragment;
import com.salomatin.alex.seetheworld.R;
import com.salomatin.alex.seetheworld.SearchFragment;
import com.salomatin.alex.seetheworld.models.PlaceModel;
import com.google.android.gms.common.ConnectionResult;

public class MainActivity extends AppCompatActivity implements SearchFragment.OnPlaceSelectedListener {

    // This activity shows relevant fragment (both if tablet) + toolbar

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int ERROR_DIALOG_REQUEST = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isServicesOK()) {
            // Set toolbar
            Toolbar toolbar = findViewById(R.id.my_toolbar);
            toolbar.setTitle(R.string.app_name);
            setSupportActionBar(toolbar);
            init();
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each selected option
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.open_favorites:
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;

            case R.id.exit_app:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof SearchFragment) {
            SearchFragment searchFragment = (SearchFragment) fragment;
            searchFragment.setOnPlaceSelectedListener(this);
        }
    }

    // Initialize activity layout type based on device (mobile or tablet)
    private void init() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FrameLayout container = findViewById(R.id.fragments_container);
        if (container != null) {
            fragmentManager.beginTransaction().replace(R.id.fragments_container, new SearchFragment()).addToBackStack("SearchFragment").commit();
        } else {
            fragmentManager.beginTransaction().replace(R.id.fragmentA_container, new SearchFragment()).commit();
            fragmentManager.beginTransaction().replace(R.id.fragmentB_container, new MapFragment()).commit();
        }
    }

    // Checking Google Services version
    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking Google Services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "isServicesOK: Google Play Services are working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d(TAG, "isServicesOK: an error occurred but it can be resolved");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, R.string.error_play_services, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    // Set UI type (for mobile or tablet)
    @Override
    public void onPlaceSelected(PlaceModel selectedPlaceInfo) {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentB_container);

        if (mapFragment != null) {
            mapFragment.updateMapView(selectedPlaceInfo.address);
        } else {
            MapFragment newMapFragment = new MapFragment();
            Bundle args = new Bundle();
            args.putString(MapFragment.ADDRESS_KEY, selectedPlaceInfo.address);
            newMapFragment.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragments_container, newMapFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }
    }
}
