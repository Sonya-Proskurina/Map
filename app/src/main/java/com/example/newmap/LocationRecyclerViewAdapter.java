package com.example.newmap;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.lang.ref.WeakReference;
import java.util.List;

public class LocationRecyclerViewAdapter extends
        RecyclerView.Adapter<LocationRecyclerViewAdapter.MyViewHolder> {
    private List<Place> list;
    Context context;


    public LocationRecyclerViewAdapter(List<Place> list,Context context) {
        this.list=list;
        this.context=context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_directions_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.name.setText(list.get(position).getTitle());
        holder.imageView.setBackgroundResource(list.get(position).getImageCart());
        holder.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.add.setBackgroundColor(Color.GRAY);
                holder.add.setText("Убрать из маршрута");
                MainActivity.points.add(Point.fromLngLat(list.get(position).y,list.get(position).x));
                LocationComponent locationComponent = MainActivity.mbMap.getLocationComponent();
                Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                        locationComponent.getLastKnownLocation().getLatitude());
                MainActivity.way(Point.fromLngLat(list.get(position).y,list.get(position).x),context);
                MainActivity.points.add(Point.fromLngLat(list.get(position).y,list.get(position).x));
            }
        });
//        SingleRecyclerViewLocation singleRecyclerViewLocation = locationList.get(position);
//        holder.name.setText(singleRecyclerViewLocation.getName());
//        holder.numOfAvailableTables.setText(singleRecyclerViewLocation.getAvailableTables());
//        holder.setClickListener(new ItemClickListener() {
//            @Override
//            public void onClick(View view, int position) {
////                weakReference.get()
////                        .drawNavigationPolylineRoute(weakReference.get().directionsRouteList.get(position));
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView name;
        Button add;
//        LinearLayout singleCard;
//        ItemClickListener clickListener;
//        TextView numOfAvailableTables;

        MyViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.prev);
            name= view.findViewById(R.id.title);
            add = view.findViewById(R.id.add);
//            name = view.findViewById(R.id.location_title_tv);
//            singleCard = view.findViewById(R.id.single_location_cardview);
//            numOfAvailableTables = view.findViewById(R.id.location_num_of_beds_tv);
//            singleCard.setOnClickListener(this);
        }

//        public void setClickListener(ItemClickListener itemClickListener) {
//            this.clickListener = itemClickListener;
//        }

//        @Override
//        public void onClick(View view) {
//            clickListener.onClick(view, getLayoutPosition());
//        }
//    }

        public interface ItemClickListener {
            void onClick(View view, int position);
        }

    }

}