package com.galaxy.ishare.publishware;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.galaxy.ishare.IShareActivity;
import com.galaxy.ishare.IShareContext;
import com.galaxy.ishare.R;
import com.galaxy.ishare.database.UserLocationDao;
import com.galaxy.ishare.model.UserLocation;
import com.galaxy.ishare.usercenter.me.CardIAddrActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuxiaoran on 15/5/20.
 * 传入PARAMETER_ADDR
 * <p/>
 * 地图的推荐策略是 searchNearbyPoi,之后是通过reverseGeo 得到nearPoi
 * 地址的推荐策略是 searchInCity
 */
public class CardOwnerAvailableAddrSearchActivity extends IShareActivity {

    public static final String PARAMETER_ADDR = "PARAMETER_ADDR";
    public static final int CARDADDR_TO_ADDRSEARCH_REQUEST_CODE = 1;
    public static final String INTENT_LOCATION_EXTRA = "INTENT_LOCATION_EXTRA";

    private ListView mapListView;
    private MapView mapView;
    private BaiduMap baiduMap;
    private MapListViewAdapter mapListViewAdpater;
    private AddrListViewAdapter addrListAdapter;

    private List<PoiInfo> mapPoiList;
    private ArrayList<PoiInfo> addrPoiList;
    private PoiSearch mapPoiSearch;
    private PoiSearch addrSearch;
    int requestCode;

    private ListView addrResultListView;
    private LinearLayout mapLayout;

    String targetAddr;
    private GeoCoder geoSearch;
    private boolean hasMapReverseGeoResult = false;
    private String reverseGeoAddrName;
    private String reverseGeoAddr;
    private LatLng reverseLatLng;
    private String TAG = "cardownersearch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.publishware_available_map_activity);

        android.support.v7.app.ActionBar actionBar = IShareContext.getInstance().createCustomActionBar(this, R.layout.main_search_actionbar, true);
        TextView searchTv = (TextView) actionBar.getCustomView().findViewById(R.id.search_tv);

        final EditText contentEt = (EditText) actionBar.getCustomView().findViewById(R.id.search_et);
        contentEt.setHint("输入您的位置");
        contentEt.setHintTextColor(getResources().getColor(R.color.dark_hint_text));

        targetAddr = getIntent().getStringExtra(PARAMETER_ADDR);
        if (targetAddr == null) {
            targetAddr = IShareContext.getInstance().getCurrentUser().getUserLocation().getLocationStr();
        }

        initViews();
        // 将用户现在的位置设置默认的位置
        double currentLatitude = 39.96;
        double currentLongtitude = 116;
        if (IShareContext.getInstance().getUserLocationNotNull() != null) {
            currentLatitude = IShareContext.getInstance().getUserLocationNotNull().getLatitude();
        }
        if (IShareContext.getInstance().getUserLocationNotNull() != null) {
            currentLongtitude = IShareContext.getInstance().getUserLocationNotNull().getLongitude();
        }
        LatLng currentLatLng = new LatLng(currentLatitude, currentLongtitude);
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(currentLatLng);
        baiduMap.setMapStatus(update);
        initPoiSearch();

        mapListView.setAdapter(mapListViewAdpater);
        mapListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 将地图中点移动到指定目标LatLng
//                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(mapPoiList.get(position).location);
//                baiduMap.animateMapStatus(u);

                if (position == 0 && hasMapReverseGeoResult) {
                    returnResult(reverseGeoAddr, reverseLatLng.longitude, reverseLatLng.latitude);

                } else {
                    PoiInfo targetInfo = mapPoiList.get(position);
                    returnResult(targetInfo.address, targetInfo.location.longitude, targetInfo.location.latitude);
                }

            }
        });

        contentEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {


                if (s.toString().length() == 0) {
                    mapLayout.setVisibility(View.VISIBLE);
                    addrResultListView.setVisibility(View.INVISIBLE);
                } else {
                    Log.v(TAG, "arrive text change" + " ");
                    mapLayout.setVisibility(View.INVISIBLE);
                    addrResultListView.setVisibility(View.VISIBLE);
                    searchAddr(contentEt.getText().toString());

                }

            }
        });

        searchTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contentEt.getText().toString().length() == 0) {
                    mapLayout.setVisibility(View.VISIBLE);
                    addrResultListView.setVisibility(View.INVISIBLE);
                } else {

                    mapLayout.setVisibility(View.INVISIBLE);
                    addrResultListView.setVisibility(View.VISIBLE);
                    searchAddr(contentEt.getText().toString());

                }
            }
        });
        // 开始显示当前地址附近的poi
        searchPoiNearby(targetAddr, currentLatLng);


        addrListAdapter = new AddrListViewAdapter();
        addrResultListView.setAdapter(addrListAdapter);
        addrResultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                PoiInfo targetInfo = addrPoiList.get(position);
                returnResult(targetInfo.address, targetInfo.location.longitude, targetInfo.location.latitude);

            }
        });

        OnGetGeoCoderResultListener listener = new OnGetGeoCoderResultListener() {
            public void onGetGeoCodeResult(GeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    //没有检索到结果
                }
                //获取地理编码结果
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    hasMapReverseGeoResult = false;
                    //没有找到检索结果
                    Toast.makeText(CardOwnerAvailableAddrSearchActivity.this, "反地理编码没有结果", Toast.LENGTH_SHORT).show();
                } else {
                    //获取反向地理编码结果
                    hasMapReverseGeoResult = true;
                    reverseGeoAddrName = result.getBusinessCircle() + " " + result.getAddress();
                    reverseGeoAddr = result.getAddressDetail().province + " " + result.getAddressDetail().street + " " + result.getAddressDetail().streetNumber;
                    reverseLatLng = result.getLocation();

                    mapPoiList.clear();
                    mapPoiList = result.getPoiList();// 附近的poi
                    mapListViewAdpater.notifyDataSetChanged();

                    mapListView.setSelection(0);  // 移动到第一条
                }


            }
        };
        geoSearch.setOnGetGeoCodeResultListener(listener);

        baiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChange(MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {

                reverseGeoSearch(mapStatus.target);
            }
        });


    }

    private void reverseGeoSearch(LatLng location) {
        geoSearch.reverseGeoCode(new ReverseGeoCodeOption().location(location));
    }

    private void searchAddr(String target) {
        addrSearch.searchInCity((new PoiCitySearchOption())
                .city(IShareContext.getInstance().getUserLocation().getCity())
                .keyword(target)
                .pageNum(0)
                .pageCapacity(50));
    }

    private void returnResult(String addr, double longitude, double latitude) {
        Intent ret = new Intent();
        int resultCode = 0;

        UserLocation userLocation = new UserLocation();
        userLocation.setUserId(IShareContext.getInstance().getCurrentUser().getUserId());
        userLocation.setAddress(addr);
        userLocation.setLongitude(longitude);
        userLocation.setLatitude(latitude);

        resultCode = CardIAddrActivity.ADDR_SEARCH_TO_CARD_ADD_RESULT_CODE;
        ret.setClass(this, CardIAddrActivity.class);
        ret.putExtra(INTENT_LOCATION_EXTRA, userLocation);
        setResult(resultCode, ret);
        this.finish();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        if (item.getItemId() == android.R.id.home) {
//            NavUtils.navigateUpFromSameTask(this);
            this.finish();

        }
        return super.onOptionsItemSelected(item);
    }

    private void initPoiSearch() {
        // poiSearch
        mapPoiSearch = PoiSearch.newInstance();
        OnGetPoiSearchResultListener poiListenr = new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult result) {
                if (result == null
                        || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                    Toast.makeText(CardOwnerAvailableAddrSearchActivity.this, "未找到结果", Toast.LENGTH_LONG)
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
                    Toast.makeText(CardOwnerAvailableAddrSearchActivity.this, strInfo, Toast.LENGTH_LONG)
                            .show();

                    mapLayout.setVisibility(View.VISIBLE);
                    addrResultListView.setVisibility(View.INVISIBLE);
                }
                if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                    // 将结果加到listview的data中
                    mapPoiList.clear();
                    List<PoiInfo> poiBaiduList = result.getAllPoi();
                    for (PoiInfo info : poiBaiduList) {
                        mapPoiList.add(info);
                    }

                    mapListViewAdpater.notifyDataSetChanged();


                }
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

            }
        };
        mapPoiSearch.setOnGetPoiSearchResultListener(poiListenr);


        addrSearch = PoiSearch.newInstance();
        final OnGetPoiSearchResultListener addrPoiListener = new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult result) {
                if (result == null
                        || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                    Toast.makeText(CardOwnerAvailableAddrSearchActivity.this, "未找到结果", Toast.LENGTH_LONG)
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
                    Toast.makeText(CardOwnerAvailableAddrSearchActivity.this, strInfo, Toast.LENGTH_LONG)
                            .show();
                }
                if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                    List<PoiInfo> poiBaiduList = result.getAllPoi();


                    if (poiBaiduList.size() == 0) {
                        mapLayout.setVisibility(View.VISIBLE);
                        addrResultListView.setVisibility(View.INVISIBLE);
                    } else {

                        addrPoiList.clear();
                        // 将结果加到listview的data中
                        for (PoiInfo info : poiBaiduList) {
                            addrPoiList.add(info);
                        }
                        addrListAdapter.notifyDataSetChanged();
                    }


                }
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

            }
        };
        addrSearch.setOnGetPoiSearchResultListener(addrPoiListener);


    }

    private void searchPoiNearby(String addr, LatLng location) {
//        mPoiSearch.searchInCity((new PoiCitySearchOption())
//                .city(IShareContext.getInstance().getUserLocation().getCity())
//                .keyword(addr)
//                .pageNum(0));
        mapPoiSearch.searchNearby((new PoiNearbySearchOption())
                .keyword(addr)
                .pageNum(0)
                .location(location)
                .radius(500)
                .pageCapacity(25));
    }


    private void initViews() {
        mapView = (MapView) findViewById(R.id.publishware_owner_available_mapview);
        baiduMap = mapView.getMap();

        mapListView = (ListView) findViewById(R.id.publishware_owner_location_listview);
        mapListViewAdpater = new MapListViewAdapter(this);
        mapPoiList = new ArrayList<>();
        addrPoiList = new ArrayList<>();

        mapLayout = (LinearLayout) findViewById(R.id.publishware_available_map_layout);
        addrResultListView = (ListView) findViewById(R.id.publishware_addr_search_result_listview);
        geoSearch = GeoCoder.newInstance();
    }

    class MapListViewAdapter extends BaseAdapter {

        private LayoutInflater layoutInflater;

        public MapListViewAdapter(Context context) {

            layoutInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mapPoiList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.publishware_owner_poi_listview, null);

            }
            Log.v(TAG, position + "  pos");
            TextView nameTv = (TextView) convertView.findViewById(R.id.publishware_addr_name_tv);
            TextView locationTv = (TextView) convertView.findViewById(R.id.publishware_addr_tv);
            TextView currentHintTv = (TextView) convertView.findViewById(R.id.publishware_current_tv);
            if (position == 0 && hasMapReverseGeoResult) {
                currentHintTv.setVisibility(View.VISIBLE);
                nameTv.setText(reverseGeoAddrName);
                locationTv.setText(reverseGeoAddr);
            } else {
                currentHintTv.setVisibility(View.INVISIBLE);
                nameTv.setText(mapPoiList.get(position).name);
                locationTv.setText(mapPoiList.get(position).address);
            }

            return convertView;
        }
    }

    class AddrListViewAdapter extends BaseAdapter {

        private LayoutInflater layoutInflater;

        public AddrListViewAdapter() {
            layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return addrPoiList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.publishware_addr_search_result_item, null);
            }
            TextView nameTv = (TextView) convertView.findViewById(R.id.publishware_add_addr_name_tv);
            TextView locationTv = (TextView) convertView.findViewById(R.id.publishware_add_addr_tv);

            nameTv.setText(addrPoiList.get(position).name);
            locationTv.setText(addrPoiList.get(position).address);

            return convertView;
        }
    }


    @Override
    protected void onPause() {
        // MapView的生命周期与Activity同步，当activity挂起时需调用MapView.onPause()
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        // MapView的生命周期与Activity同步，当activity恢复时需调用MapView.onResume()
        mapView.onResume();
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        // MapView的生命周期与Activity同步，当activity销毁时需调用MapView.destroy()
        super.onDestroy();
        mapView.onDestroy();
        mapPoiSearch.destroy();
        addrSearch.destroy();


    }


}
