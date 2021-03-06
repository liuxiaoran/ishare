package com.galaxy.ishare.publishware;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.SupportMapFragment;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.galaxy.ishare.IShareActivity;
import com.galaxy.ishare.IShareContext;
import com.galaxy.ishare.R;
import com.galaxy.ishare.utils.BaiduMapZoomManager;

import java.util.HashMap;
import java.util.List;

import info.hoang8f.widget.FButton;

/**
 * Created by liuxiaoran on 15/5/7.
 */
public class PoiSearchActivity extends IShareActivity {

    // 传入参数
    public static final String PARAMETER_SEARCH_SHOP_NAEM = "shopName";

    // request code
    public static final int PARAMETER_PULBISH_REQUEST_CODE = 1;

    // 返回参数
    public static final String PARAMETER_SHOP_LONGITUDE = "PARAMETER_SHOP_LONGITUDE";
    public static final String PARAMETER_SHOP_LATITUDE = "PARAMETER_SHOP_LATITUDE";
    public static final String PARAMETER_SHOP_ADDR = "PARAMETER_SHOP_ADDR";
    public static final String PARAMETER_SHOP_NAME = "PARAMETER_SHOP_NAME";

    public static final String TAG = "PoiSearchActivity";

    private BaiduMap mBaiduMap;

    private PoiSearch mPoiSearch;
    private BitmapDescriptor defaultPoiBitmap;
    private BitmapDescriptor choosedPoiBitmap;
    private String shopName;
    private String shopAddr;

    private FButton confirmBtn;


    private int pageIndex = 0;

    //选择的店的坐标,当isChooseShop为true时有效
    private LatLng choosedShopLatLng;

    // 上一次选择的Marker
    private Marker lastChoosedMarker;

    // 点击Marker次数
    private int clickCount;

    // 是否选择了店
    private boolean isChooseShop = false;

    // HashMap 存储marker和对应的地址
    private HashMap<Marker, String> markerAddrInfoMap;
    private HashMap<Marker, String> markerNameInfoMap;
    int pageCapacity = 10;
    private String retShopName;

    private Button zoomInBtn, zoomOutBtn;
    private MapView mapView;
    OnGetPoiSearchResultListener poiListenr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poisearch);


        IShareContext.getInstance().createDefaultHomeActionbar(this, "请选择店的位置");

        confirmBtn = (FButton) findViewById(R.id.poi_search_confirm);
        zoomInBtn = (Button) findViewById(R.id.map_zoom_in_btn);
        zoomOutBtn = (Button) findViewById(R.id.map_zoom_out_btn);

        markerAddrInfoMap = new HashMap();
        markerNameInfoMap = new HashMap();

        choosedPoiBitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_gcoding);
        defaultPoiBitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_gcoding);

        Intent intent = getIntent();
        shopName = intent.getStringExtra(PARAMETER_SEARCH_SHOP_NAEM);

        mapView = (MapView) findViewById(R.id.map_view);
        mBaiduMap = mapView.getMap();

        // poiSearch
        mPoiSearch = PoiSearch.newInstance();
        poiListenr = new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult result) {
                if (result == null
                        || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                    Toast.makeText(PoiSearchActivity.this, "未找到结果", Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

                    // 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
                    String strInfo = "在";
                    for (CityInfo cityInfo : result.getSuggestCityList()) {
                        strInfo += cityInfo.city;
                        strInfo += ",";
                    }
                    strInfo += "找到结果";
                    Toast.makeText(PoiSearchActivity.this, strInfo, Toast.LENGTH_LONG)
                            .show();
                }
                if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                    addPoiIntoMap(result);

                }
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

                if (poiDetailResult.error != SearchResult.ERRORNO.NO_ERROR) {
                    Log.v(TAG, "no detail");
                    Toast.makeText(PoiSearchActivity.this, "抱歉，未找到详细结果", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(PoiSearchActivity.this, poiDetailResult.getName() + ": " + poiDetailResult.getAddress(), Toast.LENGTH_SHORT)
                            .show();
                    Log.v(TAG, "poi detail:" + poiDetailResult.toString());
                }

            }
        };
        mPoiSearch.setOnGetPoiSearchResultListener(poiListenr);
        searchPoi();


        final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);


        // baidumap marker click listen
        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                clickCount++;
                choosedShopLatLng = marker.getPosition();
                if (marker != lastChoosedMarker) {
                    if (lastChoosedMarker != null) {
                        lastChoosedMarker.setIcon(defaultPoiBitmap);
                    }
                    marker.setIcon(choosedPoiBitmap);
                    isChooseShop = true;

                    //  创建并显示infowindow
                    View infoWindowView = layoutInflater.inflate(R.layout.publishware_info_window_layout, null);
                    TextView nameTv = (TextView) infoWindowView.findViewById(R.id.publishware_info_name_tv);
                    TextView addrTv = (TextView) infoWindowView.findViewById(R.id.publishware_info_addr_tv);
                    addrTv.setText(markerAddrInfoMap.get(marker));
                    nameTv.setText(markerNameInfoMap.get(marker));


                    InfoWindow infoWindow = new InfoWindow(infoWindowView, choosedShopLatLng, -55);
                    mBaiduMap.showInfoWindow(infoWindow);


                } else {
                    if (clickCount % 2 == 0) {
                        marker.setIcon(defaultPoiBitmap);
                        mBaiduMap.hideInfoWindow();
                        isChooseShop = false;
                    } else {
                        marker.setIcon(choosedPoiBitmap);
                        isChooseShop = true;

                        // 创建并显示显示infowindow
                        View infoWindowView = layoutInflater.inflate(R.layout.publishware_info_window_layout, null);
                        TextView addrTv = (TextView) infoWindowView.findViewById(R.id.publishware_info_name_tv);
                        addrTv.setText(markerAddrInfoMap.get(marker));


                        InfoWindow infoWindow = new InfoWindow(infoWindowView, choosedShopLatLng, -47);
                        mBaiduMap.showInfoWindow(infoWindow);
                    }


                }

                shopAddr = marker.getTitle();
                lastChoosedMarker = marker;
                retShopName = markerNameInfoMap.get(marker);

                return true;

            }
        });

        BaiduMapZoomManager manager = BaiduMapZoomManager.getInstance(mapView, zoomOutBtn, zoomInBtn);
        manager.hideOriginalZoomBtn();
        manager.setZoomListener();
    }

    private void searchPoi() {
        if (mPoiSearch != null) {
            mPoiSearch.searchInCity((new PoiCitySearchOption())
                    .city(IShareContext.getInstance().getUserLocation().getCity())
                    .keyword(shopName)
                    .pageNum(pageIndex)
                    .pageCapacity(pageCapacity));
        } else {
            mPoiSearch = PoiSearch.newInstance();
            mPoiSearch.setOnGetPoiSearchResultListener(poiListenr);
        }

    }


    private void addPoiIntoMap(PoiResult result) {
        List<PoiInfo> poiList = result.getAllPoi();
        Log.v(TAG, poiList.size() + "poilist size");
        for (PoiInfo info : poiList) {
            LatLng pointLatLng = info.location;


            //构建MarkerOption，用于在地图上添加Marker
            OverlayOptions option = new MarkerOptions()
                    .position(pointLatLng)
                    .icon(defaultPoiBitmap)
                    .zIndex(9)
                    .draggable(false);
            //在地图上添加Marker，并显示
            Marker newMarker = (Marker) mBaiduMap.addOverlay(option);

            Log.v(TAG, "info address" + info.address + info.hasCaterDetails);
            newMarker.setTitle(info.address);
            markerAddrInfoMap.put(newMarker, info.address);
            markerNameInfoMap.put(newMarker, info.name);

//            Log.v(TAG,"uid"+info.uid);
//            mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
//                    .poiUid(info.uid));
        }

    }

    public void goToNextPage(View view) {
        pageIndex++;
        searchPoi();
    }

    public void confirmShopLocation(View view) {
        double longitude = 0.0;
        double latitude = 0.0;


        if (isChooseShop) {
            longitude = choosedShopLatLng.longitude;
            latitude = choosedShopLatLng.latitude;
        }
        Intent retIntent = new Intent(PoiSearchActivity.this, ShopLocateSearchActivity.class);
        retIntent.putExtra(PARAMETER_SHOP_LATITUDE, latitude);
        retIntent.putExtra(PARAMETER_SHOP_LONGITUDE, longitude);
        retIntent.putExtra(PARAMETER_SHOP_ADDR, shopAddr);
        retIntent.putExtra(PARAMETER_SHOP_NAME, retShopName);
        setResult(PublishItemActivity.PARAMETER_SHOP_LOCATION_RESULT_CODE, retIntent);
        finish();

    }

    @Override
    protected void onPause() {
        // MapView的生命周期与Activity同步，当activity挂起时需调用MapView.onPause()

        super.onPause();
    }

    @Override
    protected void onResume() {
        // MapView的生命周期与Activity同步，当activity恢复时需调用MapView.onResume()
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // MapView的生命周期与Activity同步，当activity销毁时需调用MapView.destroy()
        Log.v(TAG, "destroy poi search");
        super.onDestroy();
        mPoiSearch.destroy();
        // 回收 bitmap 资源
        defaultPoiBitmap.recycle();
        choosedPoiBitmap.recycle();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.v(TAG, "adf");
            NavUtils.navigateUpFromSameTask(this);
        }
        return true;
    }
}
