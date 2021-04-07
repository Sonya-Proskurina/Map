package com.example.newmap;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;


import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import android.util.Log;


// classes needed to launch navigation UI

import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;

public class MainActivity extends AppCompatActivity implements MapboxMap.OnMapClickListener {
    private static final String TAG = "DirectionsActivity";

    public final List<DirectionsRoute> directionsRouteList = new ArrayList<>();

    public static MapView mapView;
    public static MapboxMap mbMap;

    private static Button buttonNavigation;
    Dialog dialog;

    public PermissionsManager permissionsManager;
    private static DirectionsRoute currentRoute;
    private static NavigationMapRoute navigationMapRoute;
    RecyclerView recyclerView;

    public static List<Point> points = new ArrayList<>();
    public List<Place> places = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.rv_on_top_of_map);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        dialog = new Dialog(MainActivity.this);
        dialog.setTitle("Начать путешесвие?");
        dialog.setContentView(R.layout.dialog_view);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                mbMap = mapboxMap;
                addMarkers();

                LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this, RecyclerView.HORIZONTAL, false);
                recyclerView.setLayoutManager(layoutManager);
                LocationRecyclerViewAdapter adapter = new LocationRecyclerViewAdapter(places,MainActivity.this);
                recyclerView.setAdapter(adapter);


                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        localization(mbMap, style);

                        addDestinationIconSymbolLayer(style);
                        enableLocationComponent(style);
                        initMapStuff(style);

//                        mapboxMap.addOnMapClickListener(MainActivity.this);

                        startWay();
                    }
                });
            }
        });

    }

    //Добавление стилей для маркера по касанию
    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    //Обработка кнопки для возврата к местонахождению пользователя
    void initMapStuff(Style style) {
        Button FAB = findViewById(R.id.myLocationButton);
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (mbMap.getLocationComponent().getLastKnownLocation() != null) { // Check to ensure coordinates aren't null, probably a better way of doing this...
                    mbMap.animateCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(new LatLng(mbMap.getLocationComponent().getLastKnownLocation().getLatitude(), mbMap.getLocationComponent().getLastKnownLocation().getLongitude()), 16));
                }
            }
        });
    }

    //Показывать точку местопроложения пользователя
    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponent locationComponent = mbMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(new PermissionsListener() {
                @Override
                public void onExplanationNeeded(List<String> permissionsToExplain) {
                    Toast.makeText(MainActivity.this, "location not enabled", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onPermissionResult(boolean granted) {
                    if (granted) {
                        mbMap.getStyle(new Style.OnStyleLoaded() {
                            @Override
                            public void onStyleLoaded(@NonNull Style style) {
                                initMapStuff(style);
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Location services not allowed", Toast.LENGTH_LONG).show();
                    }
                }
            });
            permissionsManager.requestLocationPermissions(this);
        }
    }

    //Обработка нажатия на карту (добавление маркера+пути)
    @SuppressWarnings({"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        LocationComponent locationComponent = mbMap.getLocationComponent();

        Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mbMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }

        getRoute(originPoint, destinationPoint,this);

        buttonNavigation.setEnabled(true);
        buttonNavigation.setBackgroundResource(R.color.purple_200);
        return true;
    }

    public static void way(Point point,Context context){
        LocationComponent locationComponent = mbMap.getLocationComponent();
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        getRoute(originPoint,point,context);

        buttonNavigation.setEnabled(true);
        buttonNavigation.setBackgroundResource(R.color.purple_200);
    }

    //Построение пути от пользователя до маркера
    private static void getRoute(Point origin, Point destination,Context context) {
        NavigationRoute.Builder builder = NavigationRoute.builder(context)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .profile(DirectionsCriteria.PROFILE_DRIVING);
        for (int i = 0; i < points.size(); i++) {
            builder.addWaypoint(points.get(i));
        }
        builder.build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mbMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });

    }

    //Добавление статичных маркеров на карту
    @SuppressLint("ResourceAsColor")
    public void addMarkers() {
//        points.add(Point.fromLngLat(52.596357, 38.924705));
        places.add(new Place(52.596357, 38.924705, "Кудыкина гора", "Популярный сафари-парк, расположенный на берегу Дона", R.drawable.image2));
        places.add(new Place(52.573145, 38.352376, "Кураповские скалы", "Заповедник в Задонском районе", R.drawable.image3));
        places.add(new Place(52.959332, 39.029328, "Воргольские скалы", "Уникальное место для семейного отдыха и экстремального спорта", R.drawable.image1));
    }

    //Локализация
    public void localization(MapboxMap mapboxMap, Style style) {
        LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap, style);
        try {
            localizationPlugin.matchMapLanguageWithDeviceDefault();
        } catch (RuntimeException ignored) {
        }
    }

    //Начала пути по навигатору
    public void startWay() {
        buttonNavigation = findViewById(R.id.startButton);
        buttonNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean simulateRoute = true;
                Button button=dialog.findViewById(R.id.b);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                .directionsRoute(currentRoute)
                                .shouldSimulateRoute(simulateRoute)
                                .build();

                        NavigationLauncher.startNavigation(MainActivity.this, options);
                    }
                });
                dialog.show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}