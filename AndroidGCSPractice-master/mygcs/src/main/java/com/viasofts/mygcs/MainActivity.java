package com.viasofts.mygcs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLink.common.msg_battery_status.*;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;
import com.o3dr.services.android.lib.util.MathUtils;
import com.viasofts.mygcs.activites.helpers.BluetoothDevicesActivity;
import com.viasofts.mygcs.utils.TLogUtils;
import com.viasofts.mygcs.utils.prefs.DroidPlannerPrefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.o3dr.services.android.lib.util.MathUtils.getArcInRadians;

public class MainActivity extends AppCompatActivity implements DroneListener, TowerListener, LinkListener, OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    //직선의 거리 구하기
    private static final double RADIUS_OF_EARTH_IN_METERS = 6378137.0;
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
    Marker guideMarker = new Marker();
    Handler mainHandler;

    Marker pointA = new Marker();
    Marker pointB = new Marker();

    double altitudeState =3.0;
    double flightWidth = 3.0;
    int ABdistance = 30;

    ConnectionParameter connParams;

    boolean chacking=false;
    boolean Lockcheck=false;
    boolean cadastralcheck =false;
    boolean selectAB =false;

    String mission="";

    static LatLng mGuidedPoint; //가이드모드 목적지 저장
    ArrayList<LatLng> dronePointList =new ArrayList<LatLng>();
    ArrayList<LatLng> drawLien = new ArrayList<>();
    PolylineOverlay polyline = new PolylineOverlay();
    PolylineOverlay polylineAB = new PolylineOverlay();
    // recyclerView
    private ArrayList <String> recyclerlist = new ArrayList<>();
    Adapter adapter = new Adapter(recyclerlist);
    //AB위치 정보 저장
    LatLong coordPointA ;
    LatLong coordPointB ;





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

        final Button blueToothButton = (Button) findViewById(R.id.blueToothButton);
        blueToothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DroidPlannerPrefs mPrefs = DroidPlannerPrefs.getInstance(getApplicationContext());

                String btAddress = mPrefs.getBluetoothDeviceAddress();
                final @ConnectionType.Type int connectionType = mPrefs.getConnectionParameterType();

                Uri tlogLoggingUri = TLogUtils.getTLogLoggingUri(getApplicationContext(),
                        connectionType, System.currentTimeMillis());

                final long EVENTS_DISPATCHING_PERIOD = 200L; //MS

                if (TextUtils.isEmpty(btAddress)) {
                    connParams = null;
                    startActivity(new Intent(getApplicationContext(), BluetoothDevicesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    connParams = ConnectionParameter.newBluetoothConnection(btAddress,
                            tlogLoggingUri, EVENTS_DISPATCHING_PERIOD);
                }


            }
        });

        final Button button = (Button) findViewById(R.id.connectButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drone.isConnected()) {
                    drone.disconnect();
                    button.setText("CONNECT");
                } else {
                    AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
                    alt_bld.setMessage("드론 통신방식을 선택해 주세요").setCancelable(false).setPositiveButton("UDP", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
                            drone.connect(connectionParams);
                            button.setText("DISCONNECT");
                            recyclerlist.add(" 드론과 연결 되었습니다.");
                            arrayAdepter();
                        }
                    }).setNegativeButton("blueTooth", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            drone.connect(connParams);
                            button.setText("DISCONNECT");
                            recyclerlist.add(" 드론과 연결이 끊어졌습니다.");
                            arrayAdepter();
                        }
                    });
                    AlertDialog alert = alt_bld.create();
                    // Title for AlertDialog
                    alert.setTitle("Title");
                    // Icon for AlertDialog
                    alert.show();

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
                    DroneStartButton.setText("Land");
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                        @Override
                        public void onSuccess() {
                            recyclerlist.add("착륙 합니다.");
                            arrayAdepter();
                        }
                        @Override
                        public void onError(int executionError) {
                            alertUser("Unable to land the vehicle.");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Unable to land the vehicle.");
                        }
                    });
                }

                else if(vehicleState.isArmed()) {
                    DroneStartButton.setText("Take_OFF");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("지정한 이륙고도까지 기체를 상승합니다.\n안전거리를 유지하세요.").setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                            ControlApi.getApi(drone).takeoff(altitudeState, new AbstractCommandListener() {
                                @Override
                                public void onSuccess() {
                                    alertUser("Taking off...");
                                    recyclerlist.add("이륙 합니다.");
                                    arrayAdepter();
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

                        }
                    })
                            .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                }
                            });

                    builder.create().show();

                }

                else if(vehicleState.isConnected()) {
                    DroneStartButton.setText("ARM");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("모터를 가동합니다.\n모터가 고속으로 회전합니다.").setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                            VehicleApi.getApi(drone).arm(true, false, new SimpleCommandListener() {
                                @Override
                                public void onSuccess() {
                                    recyclerlist.add("모터를 가동합니다.");
                                    arrayAdepter();
                                }

                                @Override
                                public void onError(int executionError) {
                                    alertUser("Unable to arm vehicle.");
                                }

                                @Override
                                public void onTimeout() {
                                    alertUser("Arming operation timed out.");
                                }
                            });
                            }
                             })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                }
                            });

                    builder.create().show();

                }
            }

        });
        buttoncreate();
    }

    private void buttoncreate(){
        //초기 고도
        landAltitude();
        altitudePlus();
        altitudeSubtract();
        //맵 선택 영역
        selectMap();
        basicMapSelect();
        satelliteMapSelect();
        terrainMapSelect();

        //맵보는 영역
        mapState();
        mapMove();
        mapLock();
        //지적도
        cadastralMap();

        allClear();
        //임무 버튼 생성
        missionButton();
        clickButtonAB();
        clickButtonPolygone();
        clickCancel();

        //비행폭 버튼
        flightWidth();
        flightWidthPlus();
        flightWidthSubtract();

        //AB거리
        ABdistance();
        ABdistancePlus();
        ABdistanceSubtract();


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

            case AttributeEvent.MISSION_SENT:
                Button ABMissionButton = (Button) findViewById(R.id.ABMissionButton);
                recyclerlist.add("미션 보내기 완료");
                arrayAdepter();
                ABMissionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if("임무시작".equals(ABMissionButton.getText())){
                            recyclerlist.add("임무시작");
                            arrayAdepter();
                            VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_AUTO);
                            MissionApi.getApi(drone).startMission(true,true,null);
                            ABMissionButton.setText("임무중지");
                        }
                        else if("임무중지".equals(ABMissionButton.getText())){
                            recyclerlist.add("임무중지");
                            arrayAdepter();
                            VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LOITER);
                            ABMissionButton.setText("임무시작");
                        }
                    }
                });
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
                droneLine();
                if(Lockcheck){
                    mapLocking();
                }
                if(chacking){
                    CheckGoal(drone,mGuidedPoint);
                }
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
        mNaverMap.setOnMapLongClickListener((point,coord) -> {
            mGuidedPoint = coord;
            LatLong lpoint = new LatLong(coord.latitude,coord.longitude);
            DialogSimple(lpoint);
        });

        mNaverMap.setOnMapClickListener((point, coord) -> {
            Button ABMissionButton = (Button) findViewById(R.id.ABMissionButton);
            if(selectAB) {
                if ("A지점 설정".equals(ABMissionButton.getText())) {
                    pointB.setPosition(coord);
                    pointB.setIconTintColor(Color.BLUE);
                    pointB.setMap(mNaverMap);
                    coordPointB =new LatLong(coord.latitude,coord.longitude);
                    ABMissionButton.setText("B지점 설정");

                }
                else if ("B지점 설정".equals(ABMissionButton.getText())) {
                    pointA.setPosition(coord);
                    pointA.setIconTintColor(Color.RED);
                    pointA.setMap(mNaverMap);
                    coordPointA=new LatLong(coord.latitude,coord.longitude);
                    drawingAB();
                }
            }
        });


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
    private void onFlightModeSelected(View view) {
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

    private void landAltitude(){

        Button landAltitudeButton = (Button) findViewById(R.id.landAltitudeButton);
        landAltitudeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button AltitudePlusButton = (Button) findViewById(R.id.AltitudePlusButton);
                Button AltitudeSubtractButton = (Button) findViewById(R.id.AltitudeSubtractButton);
                    if(AltitudePlusButton.getVisibility()==view.INVISIBLE){
                        AltitudePlusButton.setVisibility(view.VISIBLE);
                        AltitudeSubtractButton.setVisibility(view.VISIBLE);
                    }
                    else{
                        AltitudePlusButton.setVisibility(view.INVISIBLE);
                        AltitudeSubtractButton.setVisibility(view.INVISIBLE);
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
                    landAltitudeButton.setText(String.valueOf(altitudeState)+"m");
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
                    landAltitudeButton.setText(String.valueOf(altitudeState)+"m");
                }

            }
        });
    }


    public void DialogSimple(LatLong point) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        if(vehicleState.getVehicleMode()==VehicleMode.COPTER_GUIDED){
            guideMarker.setPosition(new LatLng(mGuidedPoint.latitude, mGuidedPoint.longitude));
            guideMarker.setIconTintColor(Color.YELLOW);
            guideMarker.setMap(mNaverMap);
            ControlApi.getApi(drone).goTo(point, true, null);
            chacking=true;
            recyclerlist.add(mGuidedPoint.latitude+" "+mGuidedPoint.longitude+"위치로 이동합니다.");
            arrayAdepter();

        }
        else{
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
            alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Action for 'Yes' Button
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                            new AbstractCommandListener() {
                                @Override
                                public void onSuccess() {
                                    guideMarker.setPosition(new LatLng(mGuidedPoint.latitude, mGuidedPoint.longitude));
                                    guideMarker.setIconTintColor(Color.YELLOW);
                                    guideMarker.setMap(mNaverMap);
                                    ControlApi.getApi(drone).goTo(point, true, null);
                                    chacking=true;
                                    recyclerlist.add(mGuidedPoint.latitude+" "+mGuidedPoint.longitude+"위치로 이동합니다.");
                                    arrayAdepter();
                                }
                                @Override
                                public void onError(int i) {
                                }
                                @Override
                                public void onTimeout() {
                                }
                            });
                }
            }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alt_bld.create();
            // Title for AlertDialog
            alert.setTitle("Title");
            // Icon for AlertDialog
            alert.show();
        }
    }
    public static boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }

    private void droneLine(){
        Gps linepoint  = this.drone.getAttribute(AttributeType.GPS);
        double lineLatitude =  linepoint.getPosition().getLatitude();
        double lineLogitude = linepoint.getPosition().getLongitude();

        Collections.addAll(dronePointList,
                new LatLng(lineLatitude,lineLogitude)
        );
        polyline.setCoords(dronePointList);
        polyline.setMap(mNaverMap);
    }

    private void selectMap(){
        Button mapButton = (Button) findViewById(R.id.mapButton);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button basicMapButton = (Button) findViewById(R.id.basicMapButton);
                Button satelliteMapButton = (Button) findViewById(R.id.satelliteMapButton);
                Button terrainMapButton = (Button) findViewById(R.id.terrainMapButton);
                if(basicMapButton.getVisibility()==view.INVISIBLE){
                    basicMapButton.setVisibility(view.VISIBLE);
                    satelliteMapButton.setVisibility(view.VISIBLE);
                    terrainMapButton.setVisibility(view.VISIBLE);
                }
                else{
                    basicMapButton.setVisibility(view.INVISIBLE);
                    satelliteMapButton.setVisibility(view.INVISIBLE);
                    terrainMapButton.setVisibility(view.INVISIBLE);
                }
            }
        });
    }

    private void basicMapSelect(){
        Button basicMapButton = (Button) findViewById(R.id.basicMapButton);
        basicMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button mapButton = (Button) findViewById(R.id.mapButton);
                mapButton.setText("일반지도");
                mNaverMap.setMapType(NaverMap.MapType.Basic);
            }
        });
    }

    private void satelliteMapSelect(){
        Button satelliteMapButton = (Button) findViewById(R.id.satelliteMapButton);
        satelliteMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button mapButton = (Button) findViewById(R.id.mapButton);
                mapButton.setText("위성지도");
                mNaverMap.setMapType(NaverMap.MapType.Satellite);
            }
        });
    }

    private void terrainMapSelect(){
        Button terrainMapButton = (Button) findViewById(R.id.terrainMapButton);
        terrainMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button mapButton = (Button) findViewById(R.id.mapButton);
                mapButton.setText("지형도");
                mNaverMap.setMapType(NaverMap.MapType.Terrain);
            }
        });
    }

    private void mapState(){
        Button mapStateButton = (Button) findViewById(R.id.mapStateButton);
        mapStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button mapMoveButton = (Button) findViewById(R.id.mapMoveButton);
                Button mapLockButton = (Button) findViewById(R.id.mapLockButton);

                if(mapMoveButton.getVisibility()==view.INVISIBLE){
                    mapMoveButton.setVisibility(view.VISIBLE);
                    mapLockButton.setVisibility(view.VISIBLE);
                }
                else{
                    mapMoveButton.setVisibility(view.INVISIBLE);
                    mapLockButton.setVisibility(view.INVISIBLE);
                }
            }
        });
    }

    private void mapMove(){
        Button mapMoveButton = (Button) findViewById(R.id.mapMoveButton);
        mapMoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button mapStateButton = (Button) findViewById(R.id.mapStateButton);
                mapStateButton.setText("맵 이동");
                Lockcheck=false;
                recyclerlist.add("맵을 이동할 수 있는 상태입니다.");
                arrayAdepter();
            }
        });
    }

    private void mapLock(){
        Button mapLockButton = (Button) findViewById(R.id.mapLockButton);
        mapLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button mapStateButton = (Button) findViewById(R.id.mapStateButton);
                mapStateButton.setText("맵 잠금");
                Lockcheck=true;
                recyclerlist.add("맵을 이동할 수 없는 상태입니다.");
                arrayAdepter();
            }
        });
    }

    private void mapLocking(){
        Gps mapLocking  = this.drone.getAttribute(AttributeType.GPS);
        double lockingLatitude =  mapLocking.getPosition().getLatitude();
        double lockingLogitude = mapLocking.getPosition().getLongitude();

        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(lockingLatitude, lockingLogitude));
        mNaverMap.moveCamera(cameraUpdate);

    }

    private void cadastralMap(){
        Button cadastralMapButton = (Button) findViewById(R.id.cadastralMapButton);
        cadastralMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cadastralcheck){
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    cadastralMapButton.setText("지적도 OFF");
                    cadastralcheck=false;
                }

                else{
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    cadastralMapButton.setText("지적도 ON");
                    cadastralcheck=true;
                }
            }
        });
    }

    private void arrayAdepter(){
        // arrayadepter

        RecyclerView listView = findViewById(R.id.recyclerView);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

    }

    private void allClear(){
        Button clearAllButton = (Button) findViewById(R.id.clearAllButton);
        clearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recyclerlist.clear();
                arrayAdepter();
                polyline.setMap(null);
                Gps stopGps = drone.getAttribute(AttributeType.GPS);
                LatLong point=stopGps.getPosition();
                ControlApi.getApi(drone).goTo(point, true, null);
                chacking=false;
                guideMarker.setMap(null);
            }
        });
    }

    private void missionButton(){
        Button missionButton = (Button) findViewById(R.id.missionButton);
        missionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button lineABButton = (Button) findViewById(R.id.lineABButton);
                Button polygoneButton = (Button) findViewById(R.id.polygoneButton);
                Button cancelbutton = (Button) findViewById(R.id.cancelbutton);

                if(lineABButton.getVisibility()==view.INVISIBLE){
                    lineABButton.setVisibility(view.VISIBLE);
                    polygoneButton.setVisibility(view.VISIBLE);
                    cancelbutton.setVisibility(view.VISIBLE);
                }
                else{
                    lineABButton.setVisibility(view.INVISIBLE);
                    polygoneButton.setVisibility(view.INVISIBLE);
                    cancelbutton.setVisibility(view.INVISIBLE);
                }
            }
        });
    }

    private void clickButtonAB(){
        Button lineABButton = (Button) findViewById(R.id.lineABButton);
        lineABButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button missionButton = (Button) findViewById(R.id.missionButton);
                missionButton.setText("AB");
                Button ABMissionButton = (Button) findViewById(R.id.ABMissionButton);
                ABMissionButton.setText("A지점 설정");
                buttonClose(view);
                selectAB=true;
            }
         });
    }

    private void clickButtonPolygone(){
        Button polygoneButton = (Button) findViewById(R.id.polygoneButton);
        polygoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button missionButton = (Button) findViewById(R.id.missionButton);
                missionButton.setText("다각형");
                buttonClose(view);
            }
        });
    }

    private void clickCancel(){
        Button cancelbutton = (Button) findViewById(R.id.cancelbutton);
        cancelbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button missionButton = (Button) findViewById(R.id.missionButton);
                missionButton.setText("취소");
                buttonClose(view);

            }
        });
    }


    private void buttonClose(View view){
        Button lineABButton = (Button) findViewById(R.id.lineABButton);
        Button polygoneButton = (Button) findViewById(R.id.polygoneButton);
        Button cancelbutton = (Button) findViewById(R.id.cancelbutton);
        lineABButton.setVisibility(view.INVISIBLE);
        polygoneButton.setVisibility(view.INVISIBLE);
        cancelbutton.setVisibility(view.INVISIBLE);
    }

    //비행폭 초기값 설정 버튼
    private void flightWidth(){

        Button flightWidthbutton = (Button) findViewById(R.id.flightWidthbutton);
        flightWidthbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button widthIncreasebutton = (Button) findViewById(R.id.widthIncreasebutton);
                Button widthDecreasebutton = (Button) findViewById(R.id.widthDecreasebutton);
                if(widthIncreasebutton.getVisibility()==view.INVISIBLE){
                    widthIncreasebutton.setVisibility(view.VISIBLE);
                    widthDecreasebutton.setVisibility(view.VISIBLE);
                }
                else{
                    widthIncreasebutton.setVisibility(view.INVISIBLE);
                    widthDecreasebutton.setVisibility(view.INVISIBLE);
                }
            }
        });
    }

    private void flightWidthPlus(){

        Button widthIncreasebutton = (Button) findViewById(R.id.widthIncreasebutton);
        widthIncreasebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button flightWidthbutton = (Button) findViewById(R.id.flightWidthbutton);
                if(flightWidth<10){
                    flightWidth+=0.5;
                    flightWidthbutton.setText(String.valueOf(flightWidth)+"m" + "비행폭");
                }

            }
        });
    }

    private void flightWidthSubtract(){
        Button widthDecreasebutton = (Button) findViewById(R.id.widthDecreasebutton);
        widthDecreasebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button flightWidthbutton = (Button) findViewById(R.id.flightWidthbutton);
                if(flightWidth>3){
                    flightWidth-=0.5;
                    flightWidthbutton.setText(String.valueOf(flightWidth)+"m"+"비행폭");
                }

            }
        });
    }

    //AB거리 초기값 설정 버튼
    private void ABdistance(){
        Button ABdistancebutton = (Button) findViewById(R.id.ABdistancebutton);
        ABdistancebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button distanceIncresebutton = (Button) findViewById(R.id.distanceIncresebutton);
                Button distanceDecreasebutton = (Button) findViewById(R.id.distanceDecreasebutton);
                if(distanceIncresebutton.getVisibility()==view.INVISIBLE){
                    distanceIncresebutton.setVisibility(view.VISIBLE);
                    distanceDecreasebutton.setVisibility(view.VISIBLE);
                }
                else{
                    distanceIncresebutton.setVisibility(view.INVISIBLE);
                    distanceDecreasebutton.setVisibility(view.INVISIBLE);
                }
            }
        });
    }

    private void ABdistancePlus(){

        Button distanceIncresebutton = (Button) findViewById(R.id.distanceIncresebutton);
        distanceIncresebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button ABdistancebutton = (Button) findViewById(R.id.ABdistancebutton);
                if(ABdistance<100){
                    ABdistance+=10;
                    ABdistancebutton.setText(String.valueOf(ABdistance)+"m"+"AB");
                }

            }
        });
    }

    private void ABdistanceSubtract(){
        Button distanceDecreasebutton = (Button) findViewById(R.id.distanceDecreasebutton);
        distanceDecreasebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button ABdistancebutton = (Button) findViewById(R.id.ABdistancebutton);
                if(ABdistance>30){
                    ABdistance-=10;
                    ABdistancebutton.setText(String.valueOf(ABdistance)+"m"+"AB");
                }

            }
        });
    }


    private void drawingAB(){
        drawLien.clear();
        polylineAB.setMap(null);
        Collections.addAll(drawLien,
                new LatLng(coordPointA.getLatitude(),coordPointA.getLongitude()),
                new LatLng(coordPointB.getLatitude(), coordPointB.getLongitude())
        );
        int checking=1;
        while(ABdistance>flightWidth*checking){
            if(checking%2==1){
                drawPointB(checking);
                drawPointA(checking);
            }
            else{
                drawPointA(checking);
                drawPointB(checking);
            }
            checking++;
        }
        polylineAB.setCoords(drawLien);
        polylineAB.setColor(Color.YELLOW);
        polylineAB.setWidth(15);
        polylineAB.setMap(mNaverMap);
        Button ABMissionButton = (Button) findViewById(R.id.ABMissionButton);
        ABMissionButton.setText("임무전송");

        ABMissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Mission mission = new Mission();
                ArrayList<Waypoint> waypointlist= new ArrayList<Waypoint>();
                for(int i = 2 ; i < drawLien.size();i++) {
                    waypointlist.add(new Waypoint());
                    waypointlist.get(i-2).setCoordinate(new LatLongAlt(drawLien.get(i).latitude,drawLien.get(i).longitude,altitudeState));
                    waypointlist.get(i-2).setDelay(1);
                    mission.addMissionItem(i-2,waypointlist.get(i-2));
                }
                MissionApi.getApi(drone).setMission(mission,true);
                ABMissionButton.setText("임무시작");
            }
        });




    }

    private void drawPointB(int checking){
        Double tempAngleB = MathUtils.getHeadingFromCoordinates(coordPointA, coordPointB);
        LatLong lineAddB=MathUtils.newCoordFromBearingAndDistance(coordPointB,90 + tempAngleB,flightWidth*checking);
        Collections.addAll(drawLien,
                new LatLng(lineAddB.getLatitude(),lineAddB.getLongitude())
        );
    }

    private void drawPointA(int checking){
        Double tempAngleA = MathUtils.getHeadingFromCoordinates(coordPointA, coordPointB);
        LatLong lineAddA =MathUtils.newCoordFromBearingAndDistance(coordPointA,90 + tempAngleA,flightWidth*checking);
        Collections.addAll(drawLien,
                new LatLng(lineAddA.getLatitude(),lineAddA.getLongitude())
        );
    }



}
