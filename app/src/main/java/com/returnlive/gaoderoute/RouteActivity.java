package com.returnlive.gaoderoute;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapCarInfo;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapRestrictionInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.autonavi.tbt.TrafficFacilityInfo;
import com.returnlive.gaoderoute.utils.AMapUtil;
import com.returnlive.gaoderoute.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class RouteActivity extends Activity implements AMapNaviListener {

    @BindView(R.id.autotextview_roadsearch_start)
    AutoCompleteTextView autotextviewRoadsearchStart;
    @BindView(R.id.autotextview_roadsearch_goals)
    AutoCompleteTextView autotextviewRoadsearchGoals;
    @BindView(R.id.btn_route)
    Button btn_route;
    @BindView(R.id.btn_select_route)
    Button btn_select_route;
    @BindView(R.id.imagebtn_roadsearch_search)
    Button imagebtnRoadsearchSearch;
    @BindView(R.id.map)
    MapView mMapView;
    private AMap aMap;
    private Unbinder unbinder;
    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private static final String TAG = "RouteActivity";
    private MarkerOptions markerOptions;
    private String cityName;
    private NaviLatLng startNaviLatLng = null;//起点坐标
    private NaviLatLng endNaviLatLng = null;//终点坐标

    private LatLonPoint startPoint = null;//起点坐标
    private LatLonPoint endPoint = null;//终点坐标
    private LatLonPoint myplacePoint = null;//我的位置

    private PoiSearch.Query startSearchQuery;
    private PoiSearch.Query endSearchQuery;
    private RouteSearch routeSearch;
    private DriveRouteResult mDriveRouteResult;// 驾车模式查询结果
    private AMapNavi mAMapNavi;
    /**
     * 选择终点Aciton标志位
     */
    private boolean mapClickEndReady;
    private NaviLatLng startLatlng;
    private NaviLatLng endLatlng;
    private List<NaviLatLng> startList = new ArrayList<NaviLatLng>();
    /**
     * 途径点坐标集合
     */
    private List<NaviLatLng> wayList = new ArrayList<NaviLatLng>();
    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<NaviLatLng> endList = new ArrayList<NaviLatLng>();
    /**
     * 保存当前算好的路线
     */
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<RouteOverLay>();

    /**
     * 当前用户选中的路线，在下个页面进行导航
     */
    private int routeIndex;
    /**
     * 路线的权值，重合路线情况下，权值高的路线会覆盖权值低的路线
     **/
    private int zindex = 1;
    /**
     * 路线计算成功标志位
     */
    private boolean calculateSuccess = false;
    private boolean chooseRouteSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_activity);
        unbinder = ButterKnife.bind(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mMapView.onCreate(savedInstanceState);
        autotextviewRoadsearchStart.addTextChangedListener(startListener);
        autotextviewRoadsearchGoals.addTextChangedListener(endListener);
        init();
    }

    private void init() {
        aMap = mMapView.getMap();
        markerOptions = new MarkerOptions();
        locationClient = new AMapLocationClient(this.getApplicationContext());
        locationOption = getDefaultOption();
        //设置定位参数
        locationClient.setLocationOption(locationOption);
        // 设置定位监听
        locationClient.setLocationListener(locationListener);
        locationClient.startLocation();
        //获取AMapNavi实例
        mAMapNavi = AMapNavi.getInstance(getApplicationContext());
//添加监听回调，用于处理算路成功
        mAMapNavi.addAMapNaviListener(this);
    }


    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            if (null != location) {

                StringBuffer sb = new StringBuffer();
                //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                if(location.getErrorCode() == 0){
                    sb.append("定位成功" + "\n");
                    sb.append("定位类型: " + location.getLocationType() + "\n");
                    sb.append("经    度    : " + location.getLongitude() + "\n");
                    sb.append("纬    度    : " + location.getLatitude() + "\n");
                    sb.append("精    度    : " + location.getAccuracy() + "米" + "\n");
                    sb.append("提供者    : " + location.getProvider() + "\n");
                    LatLng localLatLng=new LatLng(location.getLatitude(), location.getLongitude());
                    LatLonPoint startLonPoint = new LatLonPoint(location.getLatitude(), location.getLongitude());
                    startPoint = startLonPoint;
                    myplacePoint = startLonPoint;
                    Log.e(TAG, "startLonPoint: "+startLonPoint );
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(localLatLng,13));
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location));
                    //添加marker
                    markerOptions.position(localLatLng);
                    aMap.addMarker(markerOptions);
                    cityName = location.getCity();
                    sb.append("速    度    : " + location.getSpeed() + "米/秒" + "\n");
                    sb.append("角    度    : " + location.getBearing() + "\n");
                    // 获取当前提供定位服务的卫星个数
                    sb.append("星    数    : " + location.getSatellites() + "\n");
                    sb.append("国    家    : " + location.getCountry() + "\n");
                    sb.append("省            : " + location.getProvince() + "\n");
                    sb.append("市            : " + location.getCity() + "\n");
                    sb.append("城市编码 : " + location.getCityCode() + "\n");
                    sb.append("区            : " + location.getDistrict() + "\n");
                    sb.append("区域 码   : " + location.getAdCode() + "\n");
                    sb.append("地    址    : " + location.getAddress() + "\n");
                    autotextviewRoadsearchStart.setText("我的位置");
                    sb.append("兴趣点    : " + location.getPoiName() + "\n");
                    //定位完成的时间
                    sb.append("定位时间: " + Utils.formatUTC(location.getTime(), "yyyy-MM-dd HH:mm:ss") + "\n");
                } else {
                    //定位失败
                    sb.append("定位失败" + "\n");
                    sb.append("错误码:" + location.getErrorCode() + "\n");
                    sb.append("错误信息:" + location.getErrorInfo() + "\n");
                    sb.append("错误描述:" + location.getLocationDetail() + "\n");
                }
                //定位之后的回调时间
                sb.append("回调时间: " + Utils.formatUTC(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss") + "\n");

                //解析定位结果，
                String result = sb.toString();
                Log.e(TAG, "result: "+result );
                locationClient.stopLocation();//定位成功后停止定位
            } else {
                Log.e(TAG, "定位失败: " );
            }
        }
    };




    /**
     * 默认的定位参数
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private AMapLocationClientOption getDefaultOption(){
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
//        mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        return mOption;
    }




    @OnClick({R.id.btn_select_route,R.id.btn_route, R.id.imagebtn_roadsearch_search,R.id.btn_myplace_start,R.id.btn_myplace_end})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_select_route:
                changeRoute();

                break;
            case R.id.imagebtn_roadsearch_search:
                /*startvar1 = startPoint.getLongitude();
                startvar2 = startPoint.getLatitude();
                endvar1 = endPoint.getLongitude();
                endvar2 = endPoint.getLatitude();*/
                Intent intnet = new Intent(this,DaoHangActivity.class);
                intnet.putExtra("gps", true);
                startActivity(intnet);
                break;
            case R.id.btn_route:
                addRoute();
                break;
            case R.id.btn_myplace_start:
                autotextviewRoadsearchStart.setText("我的位置");
                startPoint = myplacePoint;
                break;
            case R.id.btn_myplace_end:
                autotextviewRoadsearchGoals.setText("我的位置");
                endPoint = myplacePoint;
                break;
        }
    }

    private void addRoute() {
        Log.e(TAG, "精度: "+startPoint.getLatitude() );
        Log.e(TAG, "维度: "+startPoint.getLongitude() );
        Log.e(TAG, "endPoint: "+endPoint );

        clearRoute();
        startLatlng = new NaviLatLng(startPoint.getLatitude(), startPoint.getLongitude());
        endLatlng = new NaviLatLng(endPoint.getLatitude(), endPoint.getLongitude());
        startList.clear();
        startList.add(startLatlng);
        endList.clear();
        endList.add(endLatlng);

        int strategy=0;
        try {
            strategy = mAMapNavi.strategyConvert(false, false, false, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (strategy >= 0) {
            String carNumber = "津DFZ582";
            AMapCarInfo carInfo = new AMapCarInfo();
            //设置车牌
            carInfo.setCarNumber(carNumber);
            //设置车牌是否参与限行算路
            carInfo.setRestriction(true);
            mAMapNavi.setCarInfo(carInfo);
            mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategy);
        }
    }

    public void changeRoute() {
        if (!calculateSuccess) {
            Toast.makeText(this, "请先生成路线", Toast.LENGTH_SHORT).show();
            return;
        }
        /**
         * 计算出来的路径只有一条
         */
        if (routeOverlays.size() == 1) {
            chooseRouteSuccess = true;
            //必须告诉AMapNavi 你最后选择的哪条路
            mAMapNavi.selectRouteId(routeOverlays.keyAt(0));
            Toast.makeText(this, "导航距离:" + (mAMapNavi.getNaviPath()).getAllLength() + "m" + "\n" + "导航时间:" + (mAMapNavi.getNaviPath()).getAllTime() + "s", Toast.LENGTH_SHORT).show();
            return;
        }

        if (routeIndex >= routeOverlays.size())
            routeIndex = 0;
        int routeID = routeOverlays.keyAt(routeIndex);
        //突出选择的那条路
        for (int i = 0; i < routeOverlays.size(); i++) {
            int key = routeOverlays.keyAt(i);
            routeOverlays.get(key).setTransparency(0.4f);
        }
        routeOverlays.get(routeID).setTransparency(1);
        /**把用户选择的那条路的权值弄高，使路线高亮显示的同时，重合路段不会变的透明**/
        routeOverlays.get(routeID).setZindex(zindex++);

        //必须告诉AMapNavi 你最后选择的哪条路
        mAMapNavi.selectRouteId(routeID);
        Toast.makeText(this, "路线标签:" + mAMapNavi.getNaviPath().getLabels(), Toast.LENGTH_SHORT).show();
        routeIndex++;
        chooseRouteSuccess = true;

        /**选完路径后判断路线是否是限行路线**/
        AMapRestrictionInfo info = mAMapNavi.getNaviPath().getRestrictionInfo();
        if (!TextUtils.isEmpty(info.getRestrictionTitle())) {
            Toast.makeText(this, info.getRestrictionTitle(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        startList.clear();
        wayList.clear();
        endList.clear();
        routeOverlays.clear();
        /**
         * 当前页面只是展示地图，activity销毁后不需要再回调导航的状态
         */
        mAMapNavi.removeAMapNaviListener(this);
        mAMapNavi.destroy();
        unbinder.unbind();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    private TextWatcher startListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String newText = s.toString().trim();
            if (!AMapUtil.IsEmptyOrNullString(newText)) {
                final InputtipsQuery inputquery = new InputtipsQuery(newText, cityName);
                Inputtips inputTips = new Inputtips(RouteActivity.this, inputquery);

                inputTips.setInputtipsListener(new Inputtips.InputtipsListener() {
                    @Override
                    public void onGetInputtips(List<Tip> tipList, int rCode) {
                        Tip tip = null;
                        if (rCode == AMapException.CODE_AMAP_SUCCESS) {

                            // 正确返回
                            List<String> listString = new ArrayList<String>();
                            for (int i = 0; i < tipList.size(); i++) {
                                listString.add(tipList.get(i).getName());
                                tip = tipList.get(i);

                            }
                            ArrayAdapter<String> aAdapter = new ArrayAdapter<String>(
                                    getApplicationContext(),
                                    R.layout.route_inputs, listString);
                            autotextviewRoadsearchStart.setAdapter(aAdapter);
                            if (!autotextviewRoadsearchStart.getText().toString().equals("我的位置")){
                                startPoint = tip.getPoint();
                            }
                            aAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(RouteActivity.this, rCode, Toast.LENGTH_SHORT).show();
                        }
                    }
                });


                inputTips.requestInputtipsAsyn();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };





    private TextWatcher endListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String newText = s.toString().trim();
            if (!AMapUtil.IsEmptyOrNullString(newText)) {
                InputtipsQuery inputquery = new InputtipsQuery(newText, cityName);
                Inputtips inputTips = new Inputtips(RouteActivity.this, inputquery);
                inputTips.setInputtipsListener(new Inputtips.InputtipsListener() {
                    @Override
                    public void onGetInputtips(List<Tip> tipList, int rCode) {
                        Tip tip = null;
                        if (rCode == AMapException.CODE_AMAP_SUCCESS) {// 正确返回
                            List<String> listString = new ArrayList<String>();
                            for (int i = 0; i < tipList.size(); i++) {
                                listString.add(tipList.get(i).getName());
                                tip = tipList.get(i);

                            }
                            ArrayAdapter<String> aAdapter = new ArrayAdapter<String>(
                                    getApplicationContext(),
                                    R.layout.route_inputs, listString);
                            autotextviewRoadsearchGoals.setAdapter(aAdapter);
                            endPoint = tip.getPoint();
                            aAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(RouteActivity.this, rCode, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                inputTips.requestInputtipsAsyn();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    /**
     * 清除当前地图上算好的路线
     */
    private void clearRoute() {
        for (int i = 0; i < routeOverlays.size(); i++) {
            RouteOverLay routeOverlay = routeOverlays.valueAt(i);
            routeOverlay.removeFromMap();
        }
        routeOverlays.clear();
    }


    private void drawRoutes(int routeId, AMapNaviPath path) {
        calculateSuccess = true;
        aMap.moveCamera(CameraUpdateFactory.changeTilt(0));
        RouteOverLay routeOverLay = new RouteOverLay(aMap, path, this);
        routeOverLay.setTrafficLine(false);
        routeOverLay.addToMap();
        routeOverlays.put(routeId, routeOverLay);
    }

    @Override
    public void onInitNaviFailure() {

    }

    @Override
    public void onInitNaviSuccess() {

    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }

    @Override
    public void onCalculateRouteSuccess() {
        /**
         * 清空上次计算的路径列表。
         */
        routeOverlays.clear();
        AMapNaviPath path = mAMapNavi.getNaviPath();
        /**
         * 单路径不需要进行路径选择，直接传入－1即可
         */
        drawRoutes(-1, path);
    }

    @Override
    public void onCalculateRouteFailure(int i) {
        calculateSuccess = false;
        Toast.makeText(getApplicationContext(), "计算路线失败，errorcode＝" + i, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void onCalculateMultipleRoutesSuccess(int[] ints) {
//清空上次计算的路径列表。
        routeOverlays.clear();
        HashMap<Integer, AMapNaviPath> paths = mAMapNavi.getNaviPaths();
        for (int i = 0; i < ints.length; i++) {
            AMapNaviPath path = paths.get(ints[i]);
            if (path != null) {
                drawRoutes(ints[i], path);
            }
        }
    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }
}
