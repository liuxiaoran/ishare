package com.galaxy.ishare.order;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.galaxy.ishare.IShareContext;
import com.galaxy.ishare.IShareFragment;
import com.galaxy.ishare.R;
import com.galaxy.ishare.chat.ChatActivity;
import com.galaxy.ishare.constant.URLConstant;
import com.galaxy.ishare.database.ChatDao;
import com.galaxy.ishare.http.HttpCode;
import com.galaxy.ishare.http.HttpDataResponse;
import com.galaxy.ishare.http.HttpTask;
import com.galaxy.ishare.model.Order;
import com.galaxy.ishare.model.User;
import com.galaxy.ishare.sharedcard.PullToRefreshBase;
import com.galaxy.ishare.sharedcard.PullToRefreshListView;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderFragment extends IShareFragment {

    private final String TAG = "StateFragmene";

    private User user;
    private ChatDao chatDao;

    // 判断是下拉刷新还是加载更多的操作
    private int gestureType;
    private static final int REFRESH_GESTURE = 1;
    private static final int LOAD_MORE_GESTURE = 2;

//    public static int orderType = 1;
//    public static final int BORROW_ORDER = 1;
//    public static final int LEND_ORDER = -1;

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd HH:mm");

    private View root;

    private FrameLayout listViewFrameLayout;
//    private ListView orderLendListView;
//    List<Order> orderLendList = new ArrayList<>();
//    private OrderAdapter orderLendAdapter;
//    private PullToRefreshListView lendPullToRefreshListView;

//    private int lendPageNum = 1;
//    private boolean lendHasMoreData = true;

    private ListView orderBorrowListView;
    List<Order> orderBorrowList = new ArrayList<>();
    private OrderAdapter orderBorrowAdapter;
    private PullToRefreshListView borrowPullToRefreshListView;
    public RelativeLayout loadingLayout;

    private HttpInteract httpInteract;

    private int borrowPageNum = 1;
    private boolean borrowHasMoreData = true;
    private final int pageSize = 12;

    public static OrderFragment instance;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        init("OrderFragment");
        LayoutInflater lf = LayoutInflater.from(getActivity());
        root = lf.inflate(R.layout.fragment_order, null);
        user = IShareContext.getInstance().getCurrentUser();
        chatDao = ChatDao.getInstance(getActivity());
        httpInteract = new HttpInteract();
        instance = this;
        initWidget();
//        getData();
        borrowPullToRefreshListView.doPullRefreshing(true, 500);
//        setAdapter();

        return root;
    }

    public void initWidget() {
        borrowPullToRefreshListView = new PullToRefreshListView(getActivity());
//        lendPullToRefreshListView = new PullToRefreshListView(getActivity());
        initBorrowPullToRefreshListView(borrowPullToRefreshListView);
//        initLendPullToRefreshListView(lendPullToRefreshListView);

        loadingLayout = (RelativeLayout) root.findViewById(R.id.order_loading_layout);
        listViewFrameLayout = (FrameLayout) root.findViewById(R.id.order_listview_framelayout);
        listViewFrameLayout.addView(borrowPullToRefreshListView, 0);
//        listViewFrameLayout.addView(lendPullToRefreshListView, 1);
//        lendPullToRefreshListView.setVisibility(View.GONE);

    }

    public void initBorrowPullToRefreshListView(PullToRefreshListView mPullListView) {
        mPullListView.setPullLoadEnabled(false);
        mPullListView.setScrollLoadEnabled(true);
        orderBorrowListView = mPullListView.getRefreshableView();
//        orderBorrowListView.setDivider(new ColorDrawable(getResources().getColor(R.color.listview_divider)));
//        orderBorrowListView.setDividerHeight(1);
        orderBorrowListView.setDivider(null);// 设置不显示分割线
        orderBorrowAdapter = new OrderAdapter(getActivity(), orderBorrowList);
        orderBorrowListView.setAdapter(orderBorrowAdapter);

        orderBorrowListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick");
                if (position < orderBorrowList.size()) {
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("type", 0);
                    bundle.putSerializable("order", orderBorrowList.get(position));
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });

        mPullListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                gestureType = REFRESH_GESTURE;
                httpInteract.loadData(IShareContext.getInstance().getUserLocation().getLongitude(),
                        IShareContext.getInstance().getUserLocation().getLatitude(), 1, pageSize);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
                gestureType = LOAD_MORE_GESTURE;
                borrowPageNum++;
                httpInteract.loadData(IShareContext.getInstance().getUserLocation().getLongitude(),
                        IShareContext.getInstance().getUserLocation().getLatitude(), borrowPageNum, pageSize);

            }
        });
    }

//    public void initLendPullToRefreshListView(PullToRefreshListView mPullListView) {
//        mPullListView.setPullLoadEnabled(false);
//        mPullListView.setScrollLoadEnabled(true);
//        orderLendListView = mPullListView.getRefreshableView();
//        orderLendListView.setDivider(null);// 设置不显示分割线
//        orderLendAdapter = new OrderAdapter(getActivity(), orderLendList);
//        orderLendListView.setAdapter(orderLendAdapter);
//
//
//
//        orderLendListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent(getActivity(), ChatActivity.class);
//                OrderManager.getInstance().order = orderLendList.get(position);
//                startActivity(intent);
//            }
//        });
//
//        mPullListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
//            @Override
//            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
//                gestureType = REFRESH_GESTURE;
//                httpInteract.loadData(null, user.getUserId(), IShareContext.getInstance().getUserLocation().getLongitude(),
//                        IShareContext.getInstance().getUserLocation().getLatitude(), 0, 1, pageSize);
//            }
//
//            @Override
//            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
//                gestureType = LOAD_MORE_GESTURE;
//                lendPageNum++;
//                httpInteract.loadData(null, user.getUserId(), IShareContext.getInstance().getUserLocation().getLongitude(),
//                        IShareContext.getInstance().getUserLocation().getLatitude(), 0, lendPageNum, pageSize);
//            }
//        });
//    }

//    public void getData() {
//        httpInteract.loadData(user.getUserId(), null, IShareContext.getInstance().getUserLocation().getLongitude(),
//                IShareContext.getInstance().getUserLocation().getLatitude(), 0, 1, pageSize);
//
////        httpInteract.loadData(null, user.getUserId(), IShareContext.getInstance().getUserLocation().getLongitude(),
////                IShareContext.getInstance().getUserLocation().getLatitude(), 0, 1, pageSize);
//    }

//    public void setListView() {
//        if (orderType == BORROW_ORDER) {
//            borrowPullToRefreshListView.setVisibility(View.VISIBLE);
//            lendPullToRefreshListView.setVisibility(View.GONE);
//        } else {
//            borrowPullToRefreshListView.setVisibility(View.GONE);
//            lendPullToRefreshListView.setVisibility(View.VISIBLE);
//        }
//    }

    private void setLastUpdateTime(PullToRefreshListView pullToRefreshListView) {
        String text = formatDateTime(System.currentTimeMillis());
        pullToRefreshListView.setLastUpdatedLabel(text);
    }

    private String formatDateTime(long time) {
        if (0 == time) {
            return "";
        }
        return mDateFormat.format(new Date(time));
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    class HttpInteract {
        public void loadData(double longitude, double latitude, final int pageNumber, int pageSize) {

//            if (pageNumber == 1)
//                loadingLayout.setVisibility(View.VISIBLE);

            List<BasicNameValuePair> paramsList = new ArrayList<>();
            paramsList.add(new BasicNameValuePair("longitude", longitude + ""));
            paramsList.add(new BasicNameValuePair("latitude", latitude + ""));
            paramsList.add(new BasicNameValuePair("page_num", pageNumber + ""));
            paramsList.add(new BasicNameValuePair("page_size", pageSize + ""));
            String url = URLConstant.GET_ORDER_LIST;

            HttpTask.startAsyncDataPostRequest(url, paramsList, new HttpDataResponse() {
                @Override
                public void onRecvOK(HttpRequestBase request, String result) {
                    loadingLayout.setVisibility(View.GONE);

                    try {

                        JSONObject jsonObject = new JSONObject(result);
                        int status = jsonObject.getInt("status");
                        Log.v(TAG, "result" + result);
                        if (status == 0) {
                            JSONArray jsonArray = jsonObject.getJSONArray("data");
                            Log.v(TAG, "size" + jsonArray.length());

//                            if(borrowId != null) {
                            int addNum = 0;
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject tmpJson = jsonArray.getJSONObject(i);
                                Order order = OrderUtil.parserJSONObject2Order(tmpJson);
                                order = chatDao.getLastChat(order);

                                if (gestureType == REFRESH_GESTURE) {
                                    addNum += add2List(orderBorrowList, order);
                                } else {
                                    orderBorrowList.add(order);
                                }
                            }

                            Log.e(TAG, orderBorrowList.size() + "orderBorrowList");
                            orderBorrowAdapter.notifyDataSetChanged();

                            if(addNum == 0 && gestureType == REFRESH_GESTURE) {
                                Toast.makeText(getActivity(), "已经是最新数据", Toast.LENGTH_LONG).show();
                            }

                            if (gestureType == REFRESH_GESTURE) {
                                setLastUpdateTime(borrowPullToRefreshListView);
                                borrowPullToRefreshListView.onPullDownRefreshComplete();
                            } else {
                                Log.e(TAG, "onPullDownRefreshComplete");
                                borrowPullToRefreshListView.onPullUpRefreshComplete();
                            }

                            if (jsonArray.length() == 0) {
                                borrowHasMoreData = false;
                                borrowPullToRefreshListView.setHasMoreData(borrowHasMoreData);
                            }
                            Log.e(TAG, "end");
//                          }
//                            else {
//                                int addNum = 0;
//                                for (int i = 0; i < jsonArray.length(); i++) {
//                                    JSONObject tmpJson = jsonArray.getJSONObject(i);
//                                    Order order = OrderUtil.parserJSONObject2Order(tmpJson);
//
//                                    if (gestureType == REFRESH_GESTURE) {
//                                        addNum += add2List(orderLendList, order);
//                                    } else {
//                                        orderLendList.add(order);
//                                    }
//                                }
//
//                                orderLendAdapter.notifyDataSetChanged();
//
//                                if(addNum == 0 && gestureType == REFRESH_GESTURE) {
//                                    Toast.makeText(getActivity(), "已经是最新数据", Toast.LENGTH_LONG).show();
//                                }
//
//                                if (gestureType == REFRESH_GESTURE) {
//                                    lendPullToRefreshListView.onPullDownRefreshComplete();
//                                } else {
//                                    lendPullToRefreshListView.onPullUpRefreshComplete();
//                                }
//
//                                if (jsonArray.length() == 0) {
//                                    setLastUpdateTime(lendPullToRefreshListView);
//                                    lendHasMoreData = false;
//                                    lendPullToRefreshListView.setHasMoreData(lendHasMoreData);
//                                }
//                            }
//                            recoveryAllClickable();
                        } else {
                            Log.v(TAG, "status is " + status);
                        }
                    } catch (Exception e) {
                        Log.v(TAG, e.toString());
                        e.printStackTrace();
                    }
                }

                @Override
                public void onRecvError(HttpRequestBase request, HttpCode retCode) {
                    Log.v(TAG, "error" + retCode);
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

    public int add2List(List<Order> orderList, Order order) {
        int result = 1;
        for(int i = 0; i < orderList.size(); i++) {
            if(orderList.get(i).id == order.id) {
                result = 0;
                break;
            }
        }
        if(result == 1) {
            orderList.add(0, order);
        }
        return result;
    }
}
