package com.example.myapplication;


import android.graphics.Color;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;



import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NaverMap.OnMapClickListener {

    private FusedLocationSource mLocationSource;
    CheckBox checkBox;
    private NaverMap mNaverMap;
    ArrayList <LatLng> pointList =new ArrayList<LatLng>();
    ArrayList <Marker> markerList =new ArrayList<Marker>();
    private PolygonOverlay polygonOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox) ;


        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
        MapView mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;

        polygonOverlay = new PolygonOverlay();

        CameraPosition cameraPosition = new CameraPosition(
                new LatLng(35.9470944, 126.6814342),  // 위치 지정35° 56′ 43″  126° 40′ 55.8
                10                       // 줌 레벨
        );
        naverMap.setCameraPosition(cameraPosition);

        //군산대 마커
        Marker kunsanUnivercityMarker = new Marker();
        kunsanUnivercityMarker.setPosition( new LatLng(35.9470944, 126.6814342));
        kunsanUnivercityMarker.setMap(naverMap);

        //군산시청 마커 35.9676262999961 126.736874999994
        Marker kunsanCityHallMarker = new Marker();
        kunsanCityHallMarker.setPosition( new LatLng(35.9676262999961, 126.736874999994));
        kunsanCityHallMarker.setMap(naverMap);

        //군산항 마커   35.9763594    126.6246742
        Marker kunsanHarborMarker = new Marker();
        kunsanHarborMarker.setPosition( new LatLng(35.9763594, 126.6246742));
        kunsanHarborMarker.setMap(naverMap);

        PathOverlay path = new PathOverlay();

        PolylineOverlay polyline = new PolylineOverlay();
        polyline.setCoords(Arrays.asList(
                new LatLng(35.9470944, 126.6814342),
                new LatLng(35.9676262999961, 126.736874999994),
                new LatLng(35.9763594, 126.6246742),
                new LatLng(35.9470944, 126.6814342)
        ));
        polyline.setMap(naverMap);

        PolygonOverlay polygon = new PolygonOverlay();
        polygon.setCoords(Arrays.asList(
                new LatLng(35.9470944, 126.6814342),
                new LatLng(35.9676262999961, 126.736874999994),
                new LatLng(35.9763594, 126.6246742)
        ));
        polygon.setMap(naverMap);
        // red #FF0000
        int red = 0x4DFF0000;
        polygon.setColor(red);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        //스피너에 입력될 ARRAY를 받아오는 역할
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.Spinner_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //스피너 모양 선택 레이아웃 출력
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(position==0)
                    naverMap.setMapType(NaverMap.MapType.Basic);
                if(position==1)
                    naverMap.setMapType(NaverMap.MapType.Hybrid);
                if(position==2)
                    naverMap.setMapType(NaverMap.MapType.Satellite);
                if(position==3)
                    naverMap.setMapType(NaverMap.MapType.Terrain);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //체크박스를 이용하여 클릭시 지적편집도 출력
        CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox) ;
        checkBox.setOnClickListener(new CheckBox.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkBox.isChecked()){
                    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                }
                else{
                    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                }
            }
        }) ;
        naverMap.setOnMapClickListener(this);



    }

    @Override
    public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
        //클릭시 클릭위치 경도 위도 표시
        /*Toast.makeText(this,
                "위도: " + latLng.latitude + ", 경도: " + latLng.longitude,
                Toast.LENGTH_SHORT).show();*/
        //클릭시 클릭위치에 마커 표시
        Marker makerNumber =new  Marker();
        markerList.add(makerNumber);


        makerNumber.setPosition( new LatLng(latLng.latitude ,  latLng.longitude));
        makerNumber.setMap(mNaverMap);
        pointList.add(latLng);


        compareTo(pointList);
        //정렬 compaerto 시계방향

        if (pointList.size() >= 3) {
            polygonOverlay.setCoords(pointList);
            polygonOverlay.setMap(mNaverMap);
        }
        int blue = 0x4D0000FF;
        polygonOverlay.setColor(blue);

        Button buttonDelete = (Button) findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pointList.clear();
                for(Marker m : markerList){
                    m.setMap(null);
                }
                markerList.clear();
                polygonOverlay.setMap(null);

            }
        });

        //폴리곤 값이 잘 들어가는지 확인하기 위해서 사용해봄
//        Toast.makeText(this,
//               "" +polygonList,
//                Toast.LENGTH_SHORT).show();
    }

    public ArrayList compareTo(ArrayList <LatLng> pointList) {
        float averageX = 0;
        float averageY = 0;
        for (LatLng latLng: pointList) {
            averageX += latLng.latitude;
            averageY += latLng.longitude;
        }
        final float finalAverageX = averageX / pointList.size();
         final float finalAverageY = averageY / pointList.size();
        Comparator<LatLng> comparator = (lhs, rhs) -> {
            double lhsAngle = Math.atan2(lhs.longitude - finalAverageY, lhs.latitude - finalAverageX);
            double rhsAngle = Math.atan2(rhs.longitude - finalAverageY, rhs.latitude - finalAverageX);
            // Depending on the coordinate system, you might need to reverse these two conditions
            if (lhsAngle < rhsAngle) return -1;
            if (lhsAngle > rhsAngle) return 1;
            return 0;
        };
        Collections.sort(pointList, comparator);

        return pointList;
    }

}





