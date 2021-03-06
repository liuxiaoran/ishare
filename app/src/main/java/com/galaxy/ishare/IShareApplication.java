package com.galaxy.ishare;


import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.WindowManager;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.mapapi.SDKInitializer;
import com.galaxy.ishare.constant.BroadcastActionConstant;
import com.galaxy.ishare.model.User;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;


public class IShareApplication extends Application {

    public LocationClient mLocationClient;
    public MyLocationListener mMyLocationListener;


    private static final String TAG = "application";

    public static DisplayImageOptions defaultOptions;

    @Override
    public void onCreate() {
        super.onCreate();

        Global.mContext = this;
        Global.mDensity = getResources().getDisplayMetrics().density;

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Global.screenWidth = wm.getDefaultDisplay().getWidth();
        Global.screenHeight = wm.getDefaultDisplay().getHeight();

        // 初始化sp
        IShareContext.getInstance().init(getApplicationContext());

        // baidu map init
        SDKInitializer.initialize(getApplicationContext());

        // baidu location init
        mLocationClient = new LocationClient(this.getApplicationContext());
        mMyLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(mMyLocationListener);

        // 将变量读入到Global中
        if (IShareContext.getInstance().getCurrentUser() != null) {
            Log.v(TAG, IShareContext.getInstance().getCurrentUser().getKey() + "KEY");
            Global.key = IShareContext.getInstance().getCurrentUser().getKey();
            Global.userId = IShareContext.getInstance().getCurrentUser().getUserId();


        }


        // 初始化 ImageLoader
        // Create default options which will be used for every
        //  displayImage(...) call if no options will be passed to this method
        // 之后使用display这个设置有用，使用loadImage 这个函数没有用
        defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .showImageForEmptyUri(R.drawable.load_empty)
                .showImageOnFail(R.drawable.load_error)
//                .showImageOnLoading(R.drawable.loading)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY)
                .resetViewBeforeLoading(true)
                .considerExifParams(true)
                .displayer(new FadeInBitmapDisplayer(300))
                .build();


        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(getApplicationContext());
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.threadPoolSize(3);
        config.defaultDisplayImageOptions(defaultOptions);

        ImageLoader.getInstance().init(config.build());


    }


    /**
     * 实现实位回调监听
     */
    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            String locationStr = location.getAddrStr();
            String city = location.getCity();
            String province = location.getProvince();
            String district = location.getDistrict();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            User.UserLocation userLocation = new User.UserLocation(city, province, district, locationStr, longitude, latitude);
            IShareContext.getInstance().setUserLocation(userLocation);

            mLocationClient.stop();

            // 获取新的location ，发出广播,更新位置
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(BroadcastActionConstant.UPDATE_USER_LOCATION));
            Log.v("testbroadcast", "application send broadcast");


            // 将新的位置存在sp中
            User user = null;
            user = IShareContext.getInstance().getCurrentUser();
            if (user != null) {
                user.setUserLocation(userLocation);
            } else {
                user = new User();
                user.setUserLocation(userLocation);
            }
            IShareContext.getInstance().saveCurrentUser(user);


        }


    }
}
