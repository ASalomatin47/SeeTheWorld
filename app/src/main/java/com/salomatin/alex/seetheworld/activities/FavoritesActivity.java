package com.salomatin.alex.seetheworld.activities;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.salomatin.alex.seetheworld.R;
import com.salomatin.alex.seetheworld.models.PlaceModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;

import static com.salomatin.alex.seetheworld.SearchFragment.PREFS_FILE_NAME;


public class FavoritesActivity extends AppCompatActivity {

    // This activity shows list of favorite places

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle(R.string.favorites_activity_name);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.favorites_recycler_view);

        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(true);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);


        Gson gson = new Gson();
        String jsonString = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getString("FAVORITES_PLACES", "");
        ArrayList<PlaceModel> favoritesList;
        if (("").equals(jsonString)) {
            favoritesList = new ArrayList<>();

        } else {
            favoritesList = new ArrayList<>(Arrays.asList(gson.fromJson(jsonString, PlaceModel[].class)));
        }

        Rv_adapter placesAdapter = new Rv_adapter(this, favoritesList);
        recyclerView.setAdapter(placesAdapter);
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
                holder.place_phone.setText("N/A");
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

        public class MyViewHolder extends RecyclerView.ViewHolder {

            TextView place_name;
            TextView place_address;
            TextView place_phone;
            TextView place_distance;
            ImageView place_image;

            int view_type;

            public MyViewHolder(final View itemView, final int viewType) {
                super(itemView);
                view_type = 1;
                this.place_name = itemView.findViewById(R.id.name);
                this.place_address = itemView.findViewById(R.id.address);
                this.place_phone = itemView.findViewById(R.id.phone);
                this.place_distance = itemView.findViewById(R.id.distance);
                this.place_image = itemView.findViewById(R.id.loc_image);
            }
        }
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
}
