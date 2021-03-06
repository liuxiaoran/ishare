package com.galaxy.ishare.usercenter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.galaxy.ishare.IShareContext;
import com.galaxy.ishare.IShareFragment;
import com.galaxy.ishare.R;
import com.galaxy.ishare.bindphone.BindPhoneActivity;
import com.galaxy.ishare.constant.URLConstant;
import com.galaxy.ishare.http.HttpCode;
import com.galaxy.ishare.http.HttpDataResponse;
import com.galaxy.ishare.http.HttpTask;
import com.galaxy.ishare.main.MainActivity;
import com.galaxy.ishare.model.User;
import com.galaxy.ishare.usercenter.me.CardIAddrActivity;
import com.galaxy.ishare.usercenter.me.CardICollectActivity;
import com.galaxy.ishare.usercenter.me.CardIshareActivity;
import com.galaxy.ishare.usercenter.me.CardRequestTestActivity;
import com.galaxy.ishare.usercenter.me.CustomerServiceActivity;
import com.galaxy.ishare.usercenter.me.UserCreditActivity;
import com.galaxy.ishare.utils.PhoneUtil;
import com.galaxy.ishare.utils.QiniuUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.soundcloud.android.crop.Crop;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MeFragment extends IShareFragment {

    private View myself;
    private TextView nameTv;
    public static CircleImageView avatarIV;
    private ImageView manGenderIv, womanGenderIv;
    private TextView phoneTv;
    private TextView idCreditTv, nameCreditTv, workCreditTv, workCardCreditTv, personPicCreditTv;
    public static final String ME_TO_BIND_PHONE = "ME_TO_BIND_PHONE";

    private RelativeLayout cardIShareLayout;
    private RelativeLayout cardRequestLayout;
    private RelativeLayout cardICollectLayout;
    private RelativeLayout contactKeFuLayout;
    private RelativeLayout phoneLayout;
    private RelativeLayout addrLayout;
    private RelativeLayout CustomerServiceLayout;
    private LinearLayout creditLayout;
    private LinearLayout manLayout, womanLayout;
    private HttpInteract httpInteract;
    private String TAG = "meFragment";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        init("MeFragment");
        LayoutInflater lf = LayoutInflater.from(getActivity());
        myself = lf.inflate(R.layout.activity_myself, container, false);

        initViews(myself);
        httpInteract = new HttpInteract();
        writeValueToWidget();

        ClickListener clickListener = new ClickListener();
        manLayout.setOnClickListener(clickListener);
        womanLayout.setOnClickListener(clickListener);
        nameTv.setOnClickListener(clickListener);

        return myself;
    }

    private EditText nameEt;

    class ClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.usercenter_gender_man_layout) {
                if (!manGenderIv.isSelected()) {
                    manGenderIv.setSelected(true);
                    womanGenderIv.setSelected(false);
                    httpInteract.updateGender("男");
                    updateLocalGender("男");
                }

            } else if (v.getId() == R.id.usercenter_gender_woman_layout) {
                if (!womanGenderIv.isSelected()) {
                    womanGenderIv.setSelected(true);
                    manGenderIv.setSelected(false);
                    httpInteract.updateGender("女");
                    updateLocalGender("女");
                }
            } else if (v.getId() == R.id.usercenter_nickname_tv) {
                MaterialDialog changeNameDialog = new MaterialDialog.Builder(getActivity())
                        .title("更改名字")
                        .customView(R.layout.dialog_change_name, true)
                        .positiveText("确定")
                        .negativeText("取消")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {

                                String newName = nameEt.getText().toString();
                                if (newName != null && !newName.equals("null") && !newName.equals(" ")) {
                                    nameTv.setText(newName);
                                    updateLocalName(newName);
                                    httpInteract.updateName(newName);
                                }
                            }

                        }).build();
                nameEt = (EditText) changeNameDialog.getCustomView().findViewById(R.id.dialog_change_name_et);
                nameEt.setText(IShareContext.getInstance().getCurrentUser().getUserName());
                changeNameDialog.show();
            }
        }
    }

    private void initViews(View view) {
        nameTv = (TextView) view.findViewById(R.id.usercenter_nickname_tv);
        avatarIV = (CircleImageView) view.findViewById(R.id.usercenter_avatar_iv);
        phoneTv = (TextView) view.findViewById(R.id.usercenter_phone_tv);
        cardICollectLayout = (RelativeLayout) view.findViewById(R.id.usercenter_i_collect_layout);
        cardIShareLayout = (RelativeLayout) view.findViewById(R.id.usercenter_i_share_layout);
        cardRequestLayout = (RelativeLayout) view.findViewById(R.id.usercenter_i_request_layout);
        contactKeFuLayout = (RelativeLayout) view.findViewById(R.id.usercenter_contactkefu_layout);
        phoneLayout = (RelativeLayout) view.findViewById(R.id.usercenter_phone_layout);
        creditLayout = (LinearLayout) view.findViewById(R.id.usercenter_credit_layout);
        manLayout = (LinearLayout) view.findViewById(R.id.usercenter_gender_man_layout);
        womanLayout = (LinearLayout) view.findViewById(R.id.usercenter_gender_woman_layout);
        addrLayout = (RelativeLayout) view.findViewById(R.id.usercenter_address_layout);
        CustomerServiceLayout = (RelativeLayout) view.findViewById(R.id.usercenter_contactkefu_layout);

        manGenderIv = (ImageView) view.findViewById(R.id.usercenter_man_gender_iv);
        womanGenderIv = (ImageView) view.findViewById(R.id.usercenter_woman_gender_iv);
        idCreditTv = (TextView) view.findViewById(R.id.usercenter_credit_idcard_tv);
        nameCreditTv = (TextView) view.findViewById(R.id.usercenter_credit_realname_tv);
        workCreditTv = (TextView) view.findViewById(R.id.usercenter_credit_job_tv);
        workCardCreditTv = (TextView) view.findViewById(R.id.usercenter_credit_job_card_tv);
        personPicCreditTv = (TextView) view.findViewById(R.id.usercenter_credit_person_pic_tv);


        cardICollectLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CardICollectActivity.class);
                startActivity(intent);
            }
        });

        cardIShareLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CardIshareActivity.class);
                startActivity(intent);
            }
        });

        cardRequestLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CardRequestTestActivity.class);
                startActivity(intent);
            }
        });
        creditLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UserCreditActivity.class);
                startActivity(intent);
            }
        });
        avatarIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
        phoneLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), BindPhoneActivity.class);
                intent.putExtra(BindPhoneActivity.PARAMETER_WHO_COME, MeFragment.ME_TO_BIND_PHONE);
                startActivity(intent);
            }
        });
        addrLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CardIAddrActivity.class);
                startActivity(intent);
            }
        });

        CustomerServiceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CustomerServiceActivity.class);
                startActivity(intent);
            }
        });

    }

    public void showDialog() {

        new MaterialDialog.Builder(getActivity())
                .title("选择图片来源")
                .items(R.array.pic_source_items)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (which == 0) {
                            //选择本地图片
                            Crop.pickImage(getActivity());
                        } else if (which == 1) {

                            //拍照
                            Intent intentFromCapture = new Intent(
                                    MediaStore.ACTION_IMAGE_CAPTURE);
                            // 判断存储卡是否可以用，可用进行存储
                            if (PhoneUtil.hasSdcard()) {

                                File file = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                        MainActivity.IMAGE_FILE_NAME);
                                intentFromCapture.putExtra(
                                        MediaStore.EXTRA_OUTPUT,
                                        Uri.fromFile(file));
                            }
                            getActivity().startActivityForResult(intentFromCapture,
                                    MainActivity.CAMERA_REQUEST_CODE);
                        }
                    }
                })
                .show();

    }

    private void writeValueToWidget() {
        nameTv.setText(IShareContext.getInstance().getCurrentUser().getUserName());
        ImageLoader.getInstance().displayImage(IShareContext.getInstance().getCurrentUser().getAvatar(), avatarIV, null, null);
        User user = IShareContext.getInstance().getCurrentUser();
        if (user.getGender().equals("男")) {
            manGenderIv.setSelected(true);
        } else {
            womanGenderIv.setSelected(true);
        }
        if (user.getUserPhone() != null && !user.getUserPhone().equals("")) {
            phoneTv.setText(user.getUserPhone());
        }
//        if (user.getRealName() != null) {
//            nameCreditTv.setTextColor(getResources().getColor(R.color.color_primary));
//        }
//        if (user.getIdCardPic1Url() != null && user.getIdCardPic2Url()!=null ) {
//            idCreditTv.setTextColor(getResources().getColor(R.color.color_primary));
//        }
//        if (user.getJobCardUrl() != null) {
//            workCardCreditTv.setTextColor(getResources().getColor(R.color.color_primary));
//        }
//        if (user.getJobLocation() != null) {
//            workCreditTv.setTextColor(getResources().getColor(R.color.color_primary));
//        }
//        if (user.getPersonPicUrl() != null) {
//            personPicCreditTv.setTextColor(getResources().getColor(R.color.color_primary));
//        }


    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v(TAG, "arrive fragment result");
        getImageToViewAndUploadToQiniu(Crop.getOutput(data));
    }

    private void getImageToViewAndUploadToQiniu(Uri uri) {

        avatarIV.setImageURI(uri);

        // 产生key 并且上传七牛
        String key = QiniuUtil.getInstance().generateKey("avatar");
        QiniuUtil.getInstance().uploadFileDefault(MainActivity.picSaveFile.getAbsolutePath(), key, new UpCompletionHandler() {
            @Override
            public void complete(String s, ResponseInfo responseInfo, JSONObject jsonObject) {

                if (responseInfo.isOK()) {
                    Log.v(TAG, "avatar upload qiniu is ok");
                }
            }
        });

        String avatarUrl = QiniuUtil.getInstance().getFileUrl(key);

        // 更新本地头像存储
        User user = IShareContext.getInstance().getCurrentUser();
        user.setAvatar(avatarUrl);
        IShareContext.getInstance().saveCurrentUser(user);


        // 发出广播通过头像改变
//        Intent intent = new Intent();
//        intent.setAction(BroadcastActionConstant.UPDATE_USER_AVATAR);
//        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);


        // 通知服务器头像改变
        httpInteract.updateAvatar(avatarUrl);


    }

    public void updateLocalGender(String gender) {
        User user = IShareContext.getInstance().getCurrentUser();
        user.setGender(gender);
        IShareContext.getInstance().saveCurrentUser(user);
    }

    public void updateLocalName(String name) {
        User user = IShareContext.getInstance().getCurrentUser();
        user.setUserName(name);
        IShareContext.getInstance().saveCurrentUser(user);
    }

    class HttpInteract {
        public void updateAvatar(String avatarUrl) {

            List<BasicNameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("avatar", avatarUrl));
            HttpTask.startAsyncDataPostRequest(URLConstant.UPDATE_USER_INFO, params, new HttpDataResponse() {
                @Override
                public void onRecvOK(HttpRequestBase request, String result) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        int status = jsonObject.getInt("status");
                        if (status == 0) {
                            Log.v(TAG, "update avatar   success");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onRecvError(HttpRequestBase request, HttpCode retCode) {

                }

                @Override
                public void onRecvCancelled(HttpRequestBase request) {

                }

                @Override
                public void onReceiving(HttpRequestBase request, int dataSize, int downloadSize) {

                }
            });
        }

        public void updateGender(String gender) {
            List<BasicNameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("gender", gender));
            HttpTask.startAsyncDataPostRequest(URLConstant.UPDATE_USER_INFO, params, new HttpDataResponse() {
                @Override
                public void onRecvOK(HttpRequestBase request, String result) {

                }

                @Override
                public void onRecvError(HttpRequestBase request, HttpCode retCode) {

                }

                @Override
                public void onRecvCancelled(HttpRequestBase request) {

                }

                @Override
                public void onReceiving(HttpRequestBase request, int dataSize, int downloadSize) {

                }
            });
        }

        public void updateName(String name) {
            List<BasicNameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("nickname", name));
            HttpTask.startAsyncDataPostRequest(URLConstant.UPDATE_USER_INFO, params, new HttpDataResponse() {
                @Override
                public void onRecvOK(HttpRequestBase request, String result) {

                }

                @Override
                public void onRecvError(HttpRequestBase request, HttpCode retCode) {

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

}
