package com.viasofts.mygcs;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLink.common.msg_battery_status.*;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.List;

public class MainActivity extends AppCompatActivity implements DroneListener, TowerListener, LinkListener, OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();
    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;
    private Spinner modeSelector;
    //private Spinner modeSelector;
    NaverMap mNaverMap;
    Marker marker = new Marker();
    Handler mainHandler;
    Toolbar myToolbar;
    double altitudeState =3.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);


        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);


        final Button button = (Button) findViewById(R.id.connectButton);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (drone.isConnected()) {
                    drone.disconnect();
                    button.setText("CONNECT");
                } else {
                    ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
                    drone.connect(connectionParams);
                    button.setText("DISCONNECT");
                }

            }
        });


        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        mainHandler = new Handler(getApplicationContext().getMainLooper());

        //드론 시동 및 이착륙 버튼
        final Button DroneStartButton = (Button) findViewById(R.id.DroneStartButton);
        DroneStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                State vehicleState = drone.getAttribute(AttributeType.STATE);

                if(vehicleState.isFlying()){
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                        @Override
                        public void onError(int executionError) {
                            alertUser("Unable to land the vehicle.");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Unable to land the vehicle.");
                        }
                    });
                    DroneStartButton.setText("Take_OFF");
                }

                else if(vehicleState.isArmed()) {
                    ControlApi.getApi(drone).takeoff(altitudeState, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("Taking off...");
                    }

                    @Override
                    public void onError(int i) {
                        alertUser("Unable to take off.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Unable to take off.");
                    }
                    });
                    DroneStartButton.setText("Land");
                }


                else if(vehicleState.isConnected()) {
                    VehicleApi.getApi(drone).arm(true, false, new SimpleCommandListener() {
                        @Override
                        public void onError(int executionError) {
                            alertUser("Unable to arm vehicle.");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Arming operation timed out.");
                        }
                    });
                    DroneStartButton.setText("ARM");

                }
            }

        });
        landAltitude();
        altitudePlus();
        altitudeSubtract();

    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            //updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
/*
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
 */
                checkSoloState();

                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
/*
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
 */
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
/*
                updateArmButton();
 */
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);

                }
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateVoltage();
                break;
            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.HOME_UPDATED:
//                updateDistanceFromHome();
                break;
            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;
            case AttributeEvent.GPS_COUNT:
                updateGps();
                break;
            case AttributeEvent.GPS_POSITION:
                updateDronePosition();
                break;
            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {

    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    // Helper methods
    // ==========================================================

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;
        updateVoltage();
        updatFlyingType();
        updateAltitude();
        updateSpeed();
        updateYaw();
        updateGps();

    }

    //전압 txet출력
    protected void updateVoltage() {
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        TextView TextViewVoltage = (TextView) findViewById(R.id.TextViewVoltage);
        TextViewVoltage.setText("전압: " + droneBattery.getBatteryVoltage());
    }

    //비행모드 변경
    protected void updatFlyingType() {
        TextView TextViewFlyingType = (TextView) findViewById(R.id.TextViewFlyingType);
        TextViewFlyingType.setText("비행모드: ");
    }

    // 고도 받아오는곳
    protected void updateAltitude() {
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        TextView TextViewAltitude = (TextView) findViewById(R.id.TextViewAltitude);
        TextViewAltitude.setText(" 고도: " + String.format("%3.1f", droneAltitude.getAltitude()) + "m");

    }

    //속도 받아오는곳
    protected void updateSpeed() {
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        TextView TextViewSpeed = (TextView) findViewById(R.id.TextViewSpeed);
        TextViewSpeed.setText(" 속도: " + String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    //YAW 받아오는곳
    protected void updateYaw() {
        Attitude droneAttribute = this.drone.getAttribute(AttributeType.ATTITUDE);
        TextView TextViewYaw = (TextView) findViewById(R.id.TextViewYaw);
        double Yaw = droneAttribute.getYaw() + 180;
        TextViewYaw.setText(" YAW: °" + String.format("%3.1f", Yaw));

    }

    //위성 개수 확인
    protected void updateGps() {
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);

        TextView TextViewGps = (TextView) findViewById(R.id.TextViewGps);
        TextViewGps.setText(" 위성: " + String.format("%d", droneGps.getSatellitesCount()) + "개");
    }


    //비행모드
    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new
                AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("Vehicle mode change successful.");
                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("Vehicle mode change failed: " + executionError);
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Vehicle mode change timed out.");
                    }
                });
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    //드론의 위치 표시
    //조건 기체로부터 현재 위치를 위경도 좌표를 받아와 그위칭 드론 아이콘을 표시 (방향성이 있는 이미지를 받아온다)
    //기체의 YAW에 따라 아이콘의 각도 변경 ,기체의 헤드방향을 이미지와 같이 점선 표시

    protected void updateDronePosition(){
        Gps location  = this.drone.getAttribute(AttributeType.GPS);

        double latitude =  location.getPosition().getLatitude();
        double longitude= location.getPosition().getLongitude();

        marker.setPosition(new LatLng(latitude, longitude));
        marker.setMap(mNaverMap);

        Attitude droneAttribute = this.drone.getAttribute(AttributeType.ATTITUDE);
        TextView TextViewYaw = (TextView) findViewById(R.id.TextViewYaw);
        double Yaw = droneAttribute.getYaw() + 180;
        marker.setAngle((int)Yaw);

    }

    /*7.2.3.2 조건
         * 모터 가동(ARM), 이륙(TAKE-OFF), 자동착륙(LAND) 버튼을 왼쪽 아래에 만드세요.
         * 모터 가동, 이륙, 자동착륙 버튼은 하나의 버튼이 기체 상태에 따라 TEXT가 변경되도록
        하세요.
         - 대기 중에는 모터 가동 상태
         - 시동 후에는 이륙 상태
         - 이륙 후에는 자동착륙 상태
         - 착륙 후에는 다시 모터 상태
         * 각 상태에 맞게 버튼의 기능도 변경하세요.
         * 이륙 고도를 설정할 수 있는 인터페이스를 오른쪽 위에 만드세요.
         * 이륙고도 설정 버튼을 클릭하면 +0.5, -0.5 버튼이 나타나고 다시 클릭하면 사라지게
        하세요.
         * 이륙 고도는 범위는 3m~10m로 설정하고, 0.5씩 증감되도록 하세요.
         * 이륙고도 인터페이스에서 설정한 값을 기체 이륙 시에 파라미터로 사용하세요*/


    private void landAltitude(){

        Button landAltitudeButton = (Button) findViewById(R.id.landAltitudeButton);
        landAltitudeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button AltitudePlusButton = (Button) findViewById(R.id.AltitudePlusButton);
                Button AltitudeSubtractButton = (Button) findViewById(R.id.AltitudeSubtractButton);
                State droneState = drone.getAttribute(AttributeType.STATE);
                    if(AltitudePlusButton.getVisibility()==view.GONE){
                        AltitudePlusButton.setVisibility(view.VISIBLE);
                        AltitudeSubtractButton.setVisibility(view.VISIBLE);
                    }
                    else{
                        AltitudePlusButton.setVisibility(view.GONE);
                        AltitudeSubtractButton.setVisibility(view.GONE);
                    }
            }
        });
    }

    private void altitudePlus(){

        Button AltitudePlusButton = (Button) findViewById(R.id.AltitudePlusButton);
        AltitudePlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button landAltitudeButton = (Button) findViewById(R.id.landAltitudeButton);
                if(altitudeState<10){
                    altitudeState+=0.5;
                    landAltitudeButton.setText(String.valueOf(altitudeState));
                }

            }
        });
    }

    private void altitudeSubtract(){
        Button AltitudeSubtractButton = (Button) findViewById(R.id.AltitudeSubtractButton);
        AltitudeSubtractButton.setOnClickListener(new View.OnClickListener() {
            Button landAltitudeButton = (Button) findViewById(R.id.landAltitudeButton);
            @Override
            public void onClick(View view) {
                if(altitudeState>3){
                    altitudeState-=0.5;
                    landAltitudeButton.setText(String.valueOf(altitudeState));
                }

            }
        });
    }















}
