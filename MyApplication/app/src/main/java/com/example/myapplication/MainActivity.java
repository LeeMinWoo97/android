package com.example.myapplication;


import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
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
import android.widget.Toast;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import javax.net.ssl.HttpsURLConnection;



public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NaverMap.OnMapClickListener {

    private FusedLocationSource mLocationSource;
    CheckBox checkBox;
    private NaverMap mNaverMap;
    ArrayList <LatLng> pointList =new ArrayList<LatLng>();
    ArrayList <Marker> markerList =new ArrayList<Marker>();
    private PolygonOverlay polygonOverlay;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private HttpsURLConnection http;
    private URL url;
    private InputStreamReader in;

    public class thread extends AsyncTask<LatLng, String, String> {

        @Override
        protected String doInBackground(LatLng... latLngs) {
            String strCoord = String.valueOf(latLngs[0].longitude) + "," + String.valueOf(latLngs[0].latitude);
            StringBuilder sb = new StringBuilder();

            String api ="https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?coords="+strCoord+"&sourcecrs=epsg:4326&orders=addr&output=json";
            try{
                url = new URL(api.toString());
                http= (HttpsURLConnection)url.openConnection();
                http.setRequestMethod("GET");
                http.setRequestProperty("Content-Type","application/josn");
                http.setRequestProperty("X-NCP-APIGW-API-KEY-ID","hruw818xqa");
                http.setRequestProperty("X-NCP-APIGW-API-KEY","BxzEOvfUCUfMLnPUiggy1iXke77Q9pIgalmqLa8R");


                BufferedReader rd;
                if(http.getResponseCode() >= 200 && http.getResponseCode() <= 300) {
                    rd = new BufferedReader(new InputStreamReader(http.getInputStream()));
                } else {
                    rd = new BufferedReader(new InputStreamReader(http.getErrorStream()));
                }
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
                http.disconnect();
            }
            catch(Exception e){
                return null;
            }
            return sb.toString();
        }
        protected void onPostExecute(String jsonStr,LatLng... latLngs){
            JsonParser jsonParser = new JsonParser();

            JsonObject jsonObj = (JsonObject) jsonParser.parse(jsonStr);
            JsonArray jsonArray = (JsonArray) jsonObj.get("results");
            jsonObj = (JsonObject) jsonArray.get(0);
            jsonObj = (JsonObject) jsonObj.get("code");
            String pnu = jsonObj.get("id").getAsString();

            jsonObj = (JsonObject) jsonParser.parse(jsonStr);
            jsonArray = (JsonArray) jsonObj.get("results");
            jsonObj = (JsonObject) jsonArray.get(0);
            jsonObj = (JsonObject) jsonObj.get("land");
            pnu = pnu + jsonObj.get("type").getAsString();
            String number1 = jsonObj.get("number1").getAsString();
            String number2 = jsonObj.get("number2").getAsString();
            pnu = pnu + makeStringNum(number1) + makeStringNum(number2);

            Marker markers = new Marker();
            markers.setPosition(new LatLng(latLngs[0].latitude, latLngs[0].longitude));
            markers.setMap(mNaverMap);


            InfoWindow infoWindow = new InfoWindow();
            infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getApplicationContext()) {
                @NonNull
                @Override
                public CharSequence getText(@NonNull InfoWindow infoWindow) {
                    return "";
                }
            });
            infoWindow.setPosition(new LatLng(latLngs[0].latitude, latLngs[0].longitude));
            infoWindow.open(markers);

        }
        private String makeStringNum(String number) {
            String strNum="";
            for (int i=0; i<4-number.length(); i++) {
                strNum = strNum + "0";
            }
            strNum=strNum+number;
            return strNum;
        }
    }

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

        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

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

        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setVisible(true);


        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);


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

        mNaverMap.setOnMapLongClickListener((point, coord) ->{
            new thread().execute(coord);
        });
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

}





