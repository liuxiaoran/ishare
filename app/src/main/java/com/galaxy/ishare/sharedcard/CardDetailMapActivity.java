package com.galaxy.ishare.sharedcard;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.SupportMapFragment;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.galaxy.ishare.IShareActivity;
import com.galaxy.ishare.IShareContext;
import com.galaxy.ishare.R;

/**
 * Created by liuxiaoran on 15/5/22.
 */
public class CardDetailMapActivity extends IShareActivity {

    public static final String PARAMETER_SHOP_LATITUDE = "PARAMETER_SHOP_LATITUDE";
    public static final String PARAMETER_SHOP_LONGITUDE = "PARAMENTE_SHOP_LONGITUDE";
    public static final String PARAMETER_OWNER_LATITUDE = "PARAMETER_OWNER_LATITUDE";
    public static final String PARAMETER_OWNER_LONGITUDE = "PARAMETER_OWNER_LONGITUDE";

    public static final String TAG = "CardDetailMapActivity";

    private double shopLatitude;
    private double shopLongitude;
    private double ownerLatitude;
    private double ownerLongitude;
    private double userLatitude;
    private double userLongitude;

    private BaiduMap mBaiduMap;


    private LatLng shopLatLng;
    private LatLng ownerLatLng;
    BitmapDescriptor userLocationBitmap;
    BitmapDescriptor ownerLocationBitmap;
    BitmapDescriptor shopLocationBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_item_detail_map_activity);

        mBaiduMap = ((SupportMapFragment) (getSupportFragmentManager()
                .findFragmentById(R.id.share_item_detail_map_fragment))).getBaiduMap();

        android.support.v7.app.ActionBar actionBar = IShareContext.getInstance().createDefaultHomeActionbar(this, "三方位置");

        shopLatitude = getIntent().getDoubleExtra(PARAMETER_SHOP_LATITUDE, 0);
        shopLongitude = getIntent().getDoubleExtra(PARAMETER_SHOP_LONGITUDE, 0);
        ownerLatitude = getIntent().getDoubleExtra(PARAMETER_OWNER_LATITUDE, 0);
        ownerLongitude = getIntent().getDoubleExtra(PARAMETER_OWNER_LONGITUDE, 0);
        userLatitude = IShareContext.getInstance().getUserLocation().getLatitude();
        userLongitude = IShareContext.getInstance().getUserLocation().getLongitude();

        shopLatLng = new LatLng(shopLatitude, shopLongitude);
        ownerLatLng = new LatLng(ownerLatitude, ownerLongitude);

        // 初始化 bitmap 信息，不用时及时 recycle
        userLocationBitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_marka);
        ownerLocationBitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_markb);
        shopLocationBitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_markc);

        Log.v(TAG, "shopLatitude: " + shopLatitude + " shopLongitude:" + shopLongitude);
        Log.v(TAG, "ownerLatitude: " + ownerLatitude + " ownerLongitude:" + ownerLongitude);
        Log.v(TAG, "userLatitude: " + userLatitude + " userLongitude: " + userLatitude);

        initMap();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }

    public void initMap() {
        // 选出三点中东南西北的边界坐标
        double northLatitude = shopLatitude;
        double southLatitude = shopLatitude;
        double westLongitude = shopLongitude;
        double eastLongitude = shopLongitude;

        if (ownerLatitude > northLatitude) {
            northLatitude = ownerLatitude;
        }
        if (userLatitude > northLatitude) {
            northLatitude = userLatitude;
        }

        if (ownerLatitude < southLatitude) {
            southLatitude = ownerLatitude;
        }
        if (userLatitude < southLatitude) {
            southLatitude = userLatitude;
        }

        if (ownerLongitude < westLongitude) {
            westLongitude = ownerLongitude;
        }
        if (userLongitude < westLongitude) {
            westLongitude = userLongitude;
        }


        if (ownerLongitude > eastLongitude) {
            eastLongitude = ownerLongitude;
        }
        if (userLongitude > eastLongitude) {
            eastLongitude = userLongitude;
        }
        Log.v(TAG, "north:  " + northLatitude + " south: " + southLatitude);
        Log.v(TAG, "east: " + eastLongitude + "   west: " + westLongitude);
        final LatLngBounds latLngBounds = new LatLngBounds.Builder().include(new LatLng(northLatitude, eastLongitude))
                .include(new LatLng(southLatitude, westLongitude)).build();

        // 注意在地图加载完成之后，更新地图状态。如果直接setMapStatus没有效果
        mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                // 设置地图范围，要最大限度显示三个点
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(latLngBounds);
                mBaiduMap.animateMapStatus(mapStatusUpdate, 300);
            }
        });



        // 设置Marker 并且显示
        LatLng userLatLng = new LatLng(userLatitude, userLongitude);
        LatLng ownerLocationLatLng = new LatLng(ownerLatitude, ownerLongitude);
        LatLng shopLocationLatLng = new LatLng(shopLatitude, shopLongitude);


        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions userOption = new MarkerOptions()
                .position(userLatLng)
                .icon(userLocationBitmap)
                .zIndex(9)
                .draggable(false);

        OverlayOptions ownerOption = new MarkerOptions()
                .position(ownerLocationLatLng)
                .icon(ownerLocationBitmap)
                .zIndex(9)
                .draggable(false);
        OverlayOptions shopOption = new MarkerOptions()
                .position(shopLocationLatLng)
                .icon(shopLocationBitmap)
                .zIndex(9)
                .draggable(false);
        //在地图上添加Marker，并显示
        Marker userMarker = (Marker) mBaiduMap.addOverlay(userOption);
        Marker ownerMarker = (Marker) mBaiduMap.addOverlay(ownerOption);
        Marker shopMarker = (Marker) mBaiduMap.addOverlay(shopOption);


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 回收 bitmap 资源
        userLocationBitmap.recycle();
        shopLocationBitmap.recycle();
        ownerLocationBitmap.recycle();
    }
}
