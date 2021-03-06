package com.galaxy.ishare.sharedcard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.galaxy.ishare.IShareActivity;
import com.galaxy.ishare.IShareApplication;
import com.galaxy.ishare.IShareContext;
import com.galaxy.ishare.R;
import com.galaxy.ishare.chat.ChatManager;
import com.galaxy.ishare.constant.URLConstant;
import com.galaxy.ishare.database.CollectionDao;
import com.galaxy.ishare.http.HttpCode;
import com.galaxy.ishare.http.HttpDataResponse;
import com.galaxy.ishare.http.HttpTask;
import com.galaxy.ishare.model.CardComment;
import com.galaxy.ishare.model.CardItem;
import com.galaxy.ishare.model.User;
import com.galaxy.ishare.usercenter.me.CardIshareActivity;
import com.galaxy.ishare.utils.DisplayUtil;
import com.galaxy.ishare.utils.JsonObjectUtil;
import com.galaxy.ishare.utils.QiniuUtil;
import com.galaxy.ishare.utils.WidgetController;
import com.melnykov.fab.FloatingActionButton;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import info.hoang8f.widget.FButton;

/**
 * Created by liuxiaoran on 15/5/21.
 */
public class CardDetailActivity extends IShareActivity {

    public static final String PARAMETER_CARD_ITEM = "PARAMETER_CARD_ITEM";
    public static final String PARAMETER_WHO_SEND = "PARAMETER_WHO_SEND";

    public static final int CARDISHARE_TO_CARD_DETAIL_REQUEST_CODE = 1;

    public final int COLLECTION_MESSAGE_WHAT = 1;
    public final int UNCOLLECTION_MESSAGE_WHAT = 2;
    private static final String TAG = "carddetail";

    private ViewPager cardPager;
    private TextView shopNameTv, discountTv, ownerNameTv, shopAddrTv, shopDistanceTv, cardTypeTv,
            ownerDistanceTv, descriptionTv, commentCountTv;

    private FButton contactBtn;
    private CardItem cardItem;
    //一共有几张图片,即viewpager有几个view
    private int picNumber;
    // viewpager 中的ImageView
    private ImageView[] picIvs;

    private ArrayList<View> pagerList;

    public int pageIndex = 1;
    public int pageSize = 6;
    private ArrayList<CardComment> commentsList;


    private HttpInteract httpInteract;

    private CircleImageView ownerAvatarCv;
    private FloatingActionButton mapBtn;
    private ImageView genderIv;

    private LinearLayout ratingLayout, commentLayout;

    public int currentLastCommentIndex;
    private TextView moreCommentTv;
    private FButton collectBtn;

    private RelativeLayout ownerLayout;
    private int maxUploadPicCount = 3;
    private LinearLayout viewPagerDotLayout;
    private ImageView[] pagerDotIvs;
    private int lastChooseDotPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_item_card_detail_activity);

        IShareContext.getInstance().createDefaultHomeActionbar(this, "卡详情");

        cardItem = getIntent().getParcelableExtra(PARAMETER_CARD_ITEM);

        if (cardItem.cardImgs != null) {
            picNumber = cardItem.cardImgs.length;
        }

        picIvs = new ImageView[picNumber];
        pagerDotIvs = new ImageView[picNumber];

        pagerList = new ArrayList<>();
        httpInteract = new HttpInteract();
        commentsList = new ArrayList<>();

        initViews();

        initCardPager();

        writeValueIntoViews();

        // 创建viewpager dots  imgeviews 加入layout
        for (int i = 0; i < picIvs.length; i++) {
            pagerDotIvs[i] = new ImageView(this);
            LinearLayout.LayoutParams imageViewParams = new LinearLayout.LayoutParams(DisplayUtil.dip2px(this, 6), DisplayUtil.dip2px(this, 6));
            pagerDotIvs[i].setLayoutParams(imageViewParams);
            pagerDotIvs[i].setImageResource(R.drawable.white_dot_transparent);
            viewPagerDotLayout.addView(pagerDotIvs[i]);
        }
        pagerDotIvs[0].setImageResource(R.drawable.white_dot);
        // viewpager 滑动变 相应的dot
        cardPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                pagerDotIvs[lastChooseDotPosition].setImageResource(R.drawable.white_dot_transparent);
                lastChooseDotPosition = position;
                pagerDotIvs[position].setImageResource(R.drawable.white_dot);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CardDetailActivity.this, CardDetailMapActivity.class);
                intent.putExtra(CardDetailMapActivity.PARAMETER_OWNER_LONGITUDE, cardItem.ownerLongitude);
                intent.putExtra(CardDetailMapActivity.PARAMETER_OWNER_LATITUDE, cardItem.ownerLatitude);
                intent.putExtra(CardDetailMapActivity.PARAMETER_SHOP_LONGITUDE, cardItem.shopLongitude);
                intent.putExtra(CardDetailMapActivity.PARAMETER_SHOP_LATITUDE, cardItem.shopLatitude);

                startActivity(intent);
            }
        });

        contactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳到聊天
                User currentUser = IShareContext.getInstance().getCurrentUser();
                ChatManager.getInstance().startActivityFromShare(cardItem.getId(), cardItem.ownerId, cardItem.getOwnerAvatar(), currentUser.getUserId());

            }
        });
        moreCommentTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageIndex++;
                getComments(cardItem.getId());
            }
        });
        WidgetController.getInstance().setRatingLayout(cardItem.ratingCount, this, ratingLayout);

        getComments(cardItem.id);


        // 从我-》我的分享中进来的，需要隐藏一些视图,显示一些视图
        if (getIntent().getStringExtra(PARAMETER_WHO_SEND).equals(CardIshareActivity.CARDISHARE_TO_DETAIL)) {
            mapBtn.setVisibility(View.INVISIBLE);
            ownerLayout.setVisibility(View.GONE);
            collectBtn.setVisibility(View.INVISIBLE);
        }

        if (IShareContext.getInstance().getCurrentUser().getUserId().equals(cardItem.getOwnerId())) {
            contactBtn.setVisibility(View.INVISIBLE);
        }

    }

//    private void initMapView() {
//
//        baiduMap = cardMapView.getMap();
//
//
//        // 地图的中点显示店的位置
//        float zoomLevel = 15;// 3-20
//        LatLng shopLatLng = new LatLng(cardItem.shopLatitude, cardItem.shopLongitude);
//        MapStatusUpdate statusUpdate = MapStatusUpdateFactory.newLatLngZoom(shopLatLng, zoomLevel);
//        baiduMap.setMapStatus(statusUpdate);
//
//        defaultPoiBitmap = BitmapDescriptorFactory
//                .fromResource(R.drawable.icon_gcoding);
//
//        // 将店的位置的marker显示出来
//        //构建MarkerOption，用于在地图上添加Marker
//        OverlayOptions option = new MarkerOptions()
//                .position(shopLatLng)
//                .icon(defaultPoiBitmap)
//                .zIndex(9)
//                .draggable(false);
//        //在地图上添加Marker，并显示
//        Marker newMarker = (Marker) baiduMap.addOverlay(option);
//
//    }

    private void writeValueIntoViews() {
        shopNameTv.setText(cardItem.shopName);
        discountTv.setText(cardItem.discount + "折");
        ownerNameTv.setText(cardItem.ownerName);
        String[] trades = getResources().getStringArray(R.array.trade_items);
        shopAddrTv.setText(cardItem.shopLocation);
        shopDistanceTv.setText(cardItem.shopDistance + "");
        final String[] cardTypes = getResources().getStringArray(R.array.card_items);
        cardTypeTv.setText(cardTypes[cardItem.wareType]);
        ownerDistanceTv.setText(cardItem.ownerDistance + "");
        descriptionTv.setText(cardItem.description);

        ImageSize avatarSize = new ImageSize(DisplayUtil.dip2px(this, 40), DisplayUtil.dip2px(this, 40));
        if (cardItem.getOwnerAvatar() != null) {
            ImageLoader.getInstance().loadImage(cardItem.ownerAvatar, avatarSize, IShareApplication.defaultOptions, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

                    ownerAvatarCv.setImageBitmap(loadedImage);
                }
            });
        }

        commentCountTv.setText(cardItem.getCommentCount() + "");


        if ("男".equals(cardItem.ownerGender)) {
            genderIv.setImageResource(R.drawable.icon_male);
        }

        if (CollectionDao.getInstance(this).find(cardItem.serverCollectionId, IShareContext.getInstance().getCurrentUser().getUserId()) != null) {
            collectBtn.setText("取消收藏");
            collectBtn.setButtonColor(getResources().getColor(R.color.gray));
        } else {
            collectBtn.setText("收藏");
            collectBtn.setButtonColor(getResources().getColor(R.color.color_primary));
        }


        collectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((FButton) v).getText().toString().equals("收藏")) {

                    httpInteract.collectCard(cardItem.id + "");
                } else {

                    httpInteract.unCollectCard(cardItem.serverCollectionId + "");
                }

            }
        });


    }

    Handler collectionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == COLLECTION_MESSAGE_WHAT) {
                int serverCollectionId = msg.arg1;
                cardItem.setUserId(IShareContext.getInstance().getCurrentUser().getUserId());
                cardItem.serverCollectionId = serverCollectionId;
                CollectionDao.getInstance(CardDetailActivity.this).add(cardItem);
                collectBtn.setText("取消收藏");
                collectBtn.setButtonColor(getResources().getColor(R.color.gray));

            } else if (msg.what == UNCOLLECTION_MESSAGE_WHAT) {
                CollectionDao.getInstance(CardDetailActivity.this).delete(cardItem);
                collectBtn.setText("收藏");
                collectBtn.setButtonColor(getResources().getColor(R.color.color_primary));
            }
        }
    };

    private void initViews() {

        viewPagerDotLayout = (LinearLayout) findViewById(R.id.share_item_detail_viewpager_dots_layout);

        cardPager = (ViewPager) findViewById(R.id.share_item_detail_viewpager);

        shopNameTv = (TextView) findViewById(R.id.share_item_detail_shop_name_tv);
        discountTv = (TextView) findViewById(R.id.share_item_detail_discount_tv);
        ownerNameTv = (TextView) findViewById(R.id.share_item_detail_owner_name_tv);

        shopAddrTv = (TextView) findViewById(R.id.share_item_detail_shop_location_tv);
        shopDistanceTv = (TextView) findViewById(R.id.share_item_detail_shop_distance_tv);
        cardTypeTv = (TextView) findViewById(R.id.share_item_detail_card_type_tv);
        ownerDistanceTv = (TextView) findViewById(R.id.share_item_detail_owner_distance_tv);
        descriptionTv = (TextView) findViewById(R.id.share_item_detail_card_description_tv);


        ownerAvatarCv = (CircleImageView) findViewById(R.id.share_item_detail_owner_avatar_cv);

        mapBtn = (FloatingActionButton) findViewById(R.id.share_item_detail_map_btn);
        contactBtn = (FButton) findViewById(R.id.share_item_detail_contact_btn);
        commentCountTv = (TextView) findViewById(R.id.share_item_detail_comment_number_tv);

        ratingLayout = (LinearLayout) findViewById(R.id.share_item_detail_rating_layout);
        commentLayout = (LinearLayout) findViewById(R.id.share_item_detail_comments_layout);
        genderIv = (ImageView) findViewById(R.id.share_item_detail_owner_gender_iv);

        moreCommentTv = (TextView) findViewById(R.id.share_item_detail_more_comment_tv);
        collectBtn = (FButton) findViewById(R.id.share_item_collect_btn);

        ownerLayout = (RelativeLayout) findViewById(R.id.share_item_detail_owner_layout);

        viewPagerDotLayout = (LinearLayout) findViewById(R.id.share_item_detail_viewpager_dots_layout);

    }

    private void initCardPager() {
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < picNumber; i++) {
            View view = inflater.inflate(R.layout.share_item_detail_viewpager, null);
            picIvs[i] = (ImageView) view.findViewById(R.id.share_item_detail_card_pager_iv);

            final int finalI = i;
            ImageLoader.getInstance().displayImage(QiniuUtil.getInstance().getFileThumbnailUrl(cardItem.cardImgs[i], DisplayUtil.getScreenWidth(this)
                    - DisplayUtil.dip2px(this, 80), DisplayUtil.dip2px(this, 200)), picIvs[finalI], IShareApplication.defaultOptions);
            pagerList.add(view);
        }

        MyPagerAdapter pagerAdapter = new MyPagerAdapter();
        cardPager.setAdapter(pagerAdapter);


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (ItemListFragment.INTENT_ITEM_TO_DETAIL.equals(getIntent().getStringExtra(PARAMETER_WHO_SEND))) {
                NavUtils.navigateUpFromSameTask(this);
            } else {
                this.finish();
            }
        }
        return true;
    }

    public class MyPagerAdapter extends android.support.v4.view.PagerAdapter {

        @Override
        public int getCount() {
            return pagerList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(pagerList.get(position), 0);//添加页卡
            return pagerList.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(pagerList.get(position));
        }
    }


    @Override
    protected void onDestroy() {
//        defaultPoiBitmap.recycle();
        super.onDestroy();
    }

    class HttpInteract {
        public void borrowCard() {

            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("borrow_id", IShareContext.getInstance().getCurrentUser().getUserId()));
            params.add(new BasicNameValuePair("lend_id", cardItem.ownerId));
            params.add(new BasicNameValuePair("card_id", cardItem.id + ""));
            HttpTask.startAsyncDataPostRequest(URLConstant.BORROW_CARD, params, new HttpDataResponse() {
                @Override
                public void onRecvOK(HttpRequestBase request, String result) {
                    JSONObject jsonObject = null;
                    try {

                        jsonObject = new JSONObject(result);

                        if (jsonObject.getInt("status") == 0) {
                            Toast.makeText(CardDetailActivity.this, "已发送借卡请求", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CardDetailActivity.this, "借卡失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onRecvError(HttpRequestBase request, HttpCode retCode) {
                    Toast.makeText(CardDetailActivity.this, "借卡失败，请重试", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onRecvCancelled(HttpRequestBase request) {

                }

                @Override
                public void onReceiving(HttpRequestBase request, int dataSize, int downloadSize) {

                }
            });

        }


        public void collectCard(String cardId) {
            Log.v(TAG, "cardId:" + cardId);
            List<BasicNameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("card_id", cardId));
            HttpTask.startAsyncDataPostRequest(URLConstant.ADD_COLLECTION, params, new HttpDataResponse() {
                @Override
                public void onRecvOK(HttpRequestBase request, String result) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        int status = jsonObject.getInt("status");
                        if (status == 0) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            int serverCollectionId = data.getInt("id");
                            Message message = new Message();
                            message.what = COLLECTION_MESSAGE_WHAT;
                            message.arg1 = serverCollectionId;
                            collectionHandler.sendMessage(message);
                        } else {
                            Toast.makeText(CardDetailActivity.this, "收藏失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onRecvError(HttpRequestBase request, HttpCode retCode) {
                    Log.v(TAG, "error");
                    Toast.makeText(CardDetailActivity.this, "收藏失败", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onRecvCancelled(HttpRequestBase request) {

                }

                @Override
                public void onReceiving(HttpRequestBase request, int dataSize, int downloadSize) {

                }
            });

        }

        public void unCollectCard(String cardId) {
            List<BasicNameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("collection_id", cardId));
            HttpTask.startAsyncDataPostRequest(URLConstant.REMOVE_COLLOECTION, params, new HttpDataResponse() {
                @Override
                public void onRecvOK(HttpRequestBase request, String result) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        int status = jsonObject.getInt("status");
                        if (status == 0) {
                            collectionHandler.sendEmptyMessage(UNCOLLECTION_MESSAGE_WHAT);
                        } else {
                            Toast.makeText(CardDetailActivity.this, "取消收藏", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onRecvError(HttpRequestBase request, HttpCode retCode) {
                    Toast.makeText(CardDetailActivity.this, "取消收藏", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onRecvCancelled(HttpRequestBase request) {

                }

                @Override
                public void onReceiving(HttpRequestBase request, int dataSize, int downloadSize) {

                }
            });
        }

    }

    private void putCommentIntoLayout(int beginIndex, int size) {


        for (int i = beginIndex; i <= beginIndex + size - 1; i++) {
            View comment = createCommentView(commentsList.get(i));
            commentLayout.addView(comment, i);
        }


    }

    private View createCommentView(CardComment cardComment) {
        LayoutInflater inflater = getLayoutInflater();
        View commentView = inflater.inflate(R.layout.share_item_detail_comment, null);
        CircleImageView avatarIv = (CircleImageView) commentView.findViewById(R.id.share_item_detail_comment_avatar_iv);
        int avatarEdge = DisplayUtil.dip2px(this, 40);
        String avatarThumbnail = QiniuUtil.getInstance().getFileThumbnailUrl(cardComment.commenterAvatar, avatarEdge, avatarEdge);
        ImageLoader.getInstance().displayImage(avatarThumbnail, avatarIv);

        TextView commenterNameTv = (TextView) commentView.findViewById(R.id.share_item_detail_commenter_name_tv);
        ImageView genderIv = (ImageView) commentView.findViewById(R.id.share_item_detail_comment_gender_iv);
        TextView commentTv = (TextView) commentView.findViewById(R.id.share_item_detail_comment_tv);
        TextView timeTv = (TextView) commentView.findViewById(R.id.share_item_detail_time_tv);
        LinearLayout ratingLayout = (LinearLayout) commentView.findViewById(R.id.share_item_comment_rating_layout);

        commenterNameTv.setText(cardComment.nickName);
        if (cardComment.gender.equals("男")) {
            genderIv.setImageResource(R.drawable.icon_male);
        } else {
            genderIv.setImageResource(R.drawable.icon_female);
        }
        commentTv.setText(cardComment.commentContent);

        String time = cardComment.comment_time.split(" ")[0];
        time = time.substring(2);
        timeTv.setText(time);


        WidgetController.getInstance().setRatingLayout(cardComment.rating, this, ratingLayout);

        return commentView;


    }

    public void getComments(int cardId) {

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("card_id", cardId + ""));
        params.add(new BasicNameValuePair("page_num", pageIndex + ""));
        params.add(new BasicNameValuePair("page_size", pageSize + ""));
        HttpTask.startAsyncDataPostRequest(URLConstant.GET_CARD_COMMENTS, params, new HttpDataResponse() {
            @Override
            public void onRecvOK(HttpRequestBase request, String result) {

                try {
                    JSONObject jsonObject = new JSONObject(result);
                    int status = jsonObject.getInt("status");
                    if (status == 0) {

                        JSONArray jsonArray = jsonObject.getJSONArray("data");
                        Log.v(TAG, "jsonArray:" + jsonArray.toString());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject comment = jsonArray.getJSONObject(i);
                            commentsList.add(JsonObjectUtil.parseJsonToComment(comment));
                        }


                        if (jsonArray.length() < pageSize) {
                            moreCommentTv.setText("全部评论了");
                            // 不能点击了
                            moreCommentTv.setEnabled(false);
                        }
                        putCommentIntoLayout(currentLastCommentIndex, jsonArray.length());

                        currentLastCommentIndex += jsonArray.length();
                    }
                } catch (JSONException e) {
                    Log.v(TAG, e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void onRecvError(HttpRequestBase request, HttpCode retCode) {
                Log.v(TAG, "get detail is error");
            }

            @Override
            public void onRecvCancelled(HttpRequestBase request) {

            }

            @Override
            public void onReceiving(HttpRequestBase request, int dataSize, int downloadSize) {

            }
        });
    }

}
