package com.example.newmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.lang.ref.WeakReference;
import java.util.List;

public class LocationRecyclerViewAdapter extends
        RecyclerView.Adapter<LocationRecyclerViewAdapter.MyViewHolder> {

    private List<SingleRecyclerViewLocation> locationList;
    private MapboxMap map;
    private WeakReference<MainActivity> weakReference;


    public LocationRecyclerViewAdapter(MainActivity activity,
                                       List<SingleRecyclerViewLocation> locationList,
                                       MapboxMap mapBoxMap) {
        this.locationList = locationList;
        this.map = mapBoxMap;
        this.weakReference = new WeakReference<>(activity);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_directions_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        SingleRecyclerViewLocation singleRecyclerViewLocation = locationList.get(position);
        holder.name.setText(singleRecyclerViewLocation.getName());
        holder.numOfAvailableTables.setText(singleRecyclerViewLocation.getAvailableTables());
        holder.setClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                weakReference.get()
                        .drawNavigationPolylineRoute(weakReference.get().directionsRouteList.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name;
        LinearLayout singleCard;
        ItemClickListener clickListener;
        TextView numOfAvailableTables;

        MyViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.location_title_tv);
            singleCard = view.findViewById(R.id.single_location_cardview);
            numOfAvailableTables = view.findViewById(R.id.location_num_of_beds_tv);
            singleCard.setOnClickListener(this);
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.clickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            clickListener.onClick(view, getLayoutPosition());
        }
    }

    public interface ItemClickListener {
        void onClick(View view, int position);
    }

}