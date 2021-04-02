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
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
    private static final String SYMBOL_ICON_ID = "SYMBOL_ICON_ID";
    private static final String PERSON_ICON_ID = "PERSON_ICON_ID";
    private static final String MARKER_SOURCE_ID = "MARKER_SOURCE_ID";
    private static final String PERSON_SOURCE_ID = "PERSON_SOURCE_ID";
    private static final String DASHED_DIRECTIONS_LINE_LAYER_SOURCE_ID = "DASHED_DIRECTIONS_LINE_LAYER_SOURCE_ID";
    private static final String LAYER_ID = "LAYER_ID";
    private static final String PERSON_LAYER_ID = "PERSON_LAYER_ID";
    private static final String DASHED_DIRECTIONS_LINE_LAYER_ID = "DASHED_DIRECTIONS_LINE_LAYER_ID";

    private static final Point directionsOriginPoint = Point.fromLngLat(100.48730850219725,
            13.737217333153827);
    private static final  LatLng[] possibleDestinations = new LatLng[]{
            new LatLng(13.773399508046145, 100.51116943359375),
            new LatLng(13.743387039520751, 100.45074462890625),
            new LatLng(13.732715012486663, 100.5523681640625),
            new LatLng(13.665336643848484, 100.45486450195312),
            new LatLng(13.7153719325982, 100.49263000488281),
            new LatLng(13.742053062720384, 100.51288604736328),
            new LatLng(13.77773432408578, 100.4806137084961),
            new LatLng(13.784736549340208, 100.55580139160156),
            new LatLng(13.71670606117596, 100.45520782470703)
    };

    public final List<DirectionsRoute> directionsRouteList = new ArrayList<>();

    public MapView mapView;
    public MapboxMap mbMap;

    private Button buttonNavigation;

    public PermissionsManager permissionsManager;
    private DirectionsRoute currentRoute;
    private NavigationMapRoute navigationMapRoute;
    private FeatureCollection dashedLineDirectionsFeatureCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                mbMap = mapboxMap;
                addMarkers();

                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        localization(mbMap,style);

                        addDestinationIconSymbolLayer(style);
                        enableLocationComponent(style);
                        initMapStuff(style);

                        mapboxMap.addOnMapClickListener(MainActivity.this);

                        startWay();

                        getRoutesToAllPoints();
                        initRecyclerView();
                    }
                });
            }
        });
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rv_on_top_of_map);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(),
                LinearLayoutManager.HORIZONTAL, true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new LocationRecyclerViewAdapter(this,
                createRecyclerViewLocations(), mbMap));
        new LinearSnapHelper().attachToRecyclerView(recyclerView);
    }

    private List<SingleRecyclerViewLocation> createRecyclerViewLocations() {
        ArrayList<SingleRecyclerViewLocation> locationList = new ArrayList<>();
        for (int x = 0; x < possibleDestinations.length; x++) {
            SingleRecyclerViewLocation singleLocation = new SingleRecyclerViewLocation();
            singleLocation.setName(String.format("Миса", x));
            singleLocation.setAvailableTables(String.format("ошка",
                    new Random().nextInt(possibleDestinations.length)));
            locationList.add(singleLocation);
        }
        return locationList;
    }

    private void getRoutesToAllPoints() {
        for (LatLng singleLatLng : possibleDestinations) {
            getRoute2(Point.fromLngLat(singleLatLng.getLongitude(), singleLatLng.getLatitude()));
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void getRoute2(Point destination) {
        MapboxDirections client = MapboxDirections.builder()
                .origin(directionsOriginPoint)
                .destination(destination)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(getString(R.string.mapbox_access_token))
                .build();
        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.body() == null) {
                    Log.d(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Log.d(TAG, "No routes found");
                    return;
                }
// Add the route to the list.
                directionsRouteList.add(response.body().routes().get(0));
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.d(TAG, "Error: " + throwable.getMessage());
                if (!throwable.getMessage().equals("Coordinate is invalid: 0,0")) {
                    Toast.makeText(MainActivity.this,
                            "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void drawNavigationPolylineRoute(final DirectionsRoute route) {
        if (mbMap != null) {
            mbMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    List<Feature> directionsRouteFeatureList = new ArrayList<>();
                    LineString lineString = LineString.fromPolyline(route.geometry(), PRECISION_6);
                    List<Point> lineStringCoordinates = lineString.coordinates();
                    for (int i = 0; i < lineStringCoordinates.size(); i++) {
                        directionsRouteFeatureList.add(Feature.fromGeometry(
                                LineString.fromLngLats(lineStringCoordinates)));
                    }
                    dashedLineDirectionsFeatureCollection =
                            FeatureCollection.fromFeatures(directionsRouteFeatureList);
                    GeoJsonSource source = style.getSourceAs(DASHED_DIRECTIONS_LINE_LAYER_SOURCE_ID);
                    if (source != null) {
                        source.setGeoJson(dashedLineDirectionsFeatureCollection);
                    }
                }
            });
        }
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

    //генерация ресурсов для кастомизации маркеров
    public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id, @ColorInt int colorRes) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
//        DrawableCompat.setTint(vectorDrawable, colorRes);
        vectorDrawable.draw(canvas);
        return IconFactory.getInstance(context).fromBitmap(bitmap);
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

        getRoute(originPoint, destinationPoint);

        buttonNavigation.setEnabled(true);
        buttonNavigation.setBackgroundResource(R.color.purple_200);

        return true;
    }

    //Построение пути от пользователя до маркера
    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
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
    public void addMarkers(){
        mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(52.596357, 38.924705))
                .title("Галичья гора")
                .setIcon(drawableToIcon(getBaseContext(), R.drawable.ic_baseline_place_24, R.color.black))
                .snippet("Заповедник в Задонском районе"));

        mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(52.959332, 38.924705))
                .title("Кураповские скалы")
                .snippet("Заповедник в Задонском районе"));

        mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(52.573145, 38.352376))
                .title("Воргольские скалы")
                .snippet("Уникальное место для семейного отдыха и экстремального спорта"));
    }

    public void localization(MapboxMap mapboxMap,Style style){
        LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap, style);
        try {
            localizationPlugin.matchMapLanguageWithDeviceDefault();
        } catch (RuntimeException ignored) {
        }
    }

    //Начала пути по навигатору
    public void startWay(){
        buttonNavigation = findViewById(R.id.startButton);
        buttonNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean simulateRoute = true;

                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(simulateRoute)
                        .build();

                NavigationLauncher.startNavigation(MainActivity.this, options);
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