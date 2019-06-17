package com.salomatin.alex.seetheworld.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.salomatin.alex.seetheworld.R;

import static com.salomatin.alex.seetheworld.SearchFragment.PREFS_FILE_NAME;

public class SettingsActivity extends AppCompatActivity {

    // Shows app settings

    boolean isKm;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle("Settings");
        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
        isKm = sharedPreferences.getBoolean("KM_OR_MILES", true);
        Log.d("Test", "is km " + isKm);
        if (isKm) {
            ((RadioButton) findViewById(R.id.radio_button_km)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radio_button_miles)).setChecked(true);
        }

        findViewById(R.id.delete_favorites).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferences.edit().putString("FAVORITES_PLACES", "").commit();
                Toast.makeText(SettingsActivity.this, R.string.favorites_erased_toast, Toast.LENGTH_SHORT).show();
            }
        });

        ((RadioGroup) findViewById(R.id.setting_radio_group)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_button_km:
                        isKm = true;
                        Toast.makeText(SettingsActivity.this, R.string.km_selected_toast, Toast.LENGTH_SHORT).show();
                        Log.d("Test", "is km true");
                        break;
                    case R.id.radio_button_miles:
                        isKm = false;
                        Toast.makeText(SettingsActivity.this, R.string.mi_selected_toast, Toast.LENGTH_SHORT).show();
                        Log.d("Test", "is km false");
                        break;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.back_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get back to Main Activity
        switch (item.getItemId()) {
            case R.id.action_back:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.edit().putBoolean("KM_OR_MILES", isKm).commit();
        Log.d("Test", "Saved units: " + isKm);
    }
}
