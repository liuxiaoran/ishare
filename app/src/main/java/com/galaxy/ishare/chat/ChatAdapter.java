package com.galaxy.ishare.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.galaxy.ishare.IShareApplication;
import com.galaxy.ishare.IShareContext;
import com.galaxy.ishare.R;
import com.galaxy.ishare.model.Chat;
import com.galaxy.ishare.model.User;
import com.galaxy.ishare.utils.DateUtil;
import com.galaxy.ishare.utils.DisplayUtil;
import com.galaxy.ishare.utils.QiniuUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Zhan on 2015/5/19.
 */
public class ChatAdapter extends BaseAdapter {
    private static String TAG = "ChatAdapter";

    private Context mContext;
    private List<Chat> chatList;
    private LayoutInflater mInflater;
    private User user;
    private String fromAvatar;
    private String toAvatar;

    public ChatAdapter(Context mContext, List<Chat> chatList, String fromAvatar, String toAvatar) {
        this.mContext = mContext;
        this.chatList = chatList;
        this.fromAvatar = fromAvatar;
        this.toAvatar = toAvatar;
        mInflater = LayoutInflater.from(mContext);
        user = IShareContext.getInstance().getCurrentUser();
    }

    public int getCount() {
        return chatList.size();
    }

    public Object getItem(int position) {
        return chatList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        Chat msg = chatList.get(position);
        String avatarUrl = null;
        ViewHolder viewHolder = null;

        if (convertView == null)
        {
            if (!user.getUserId().equals(msg.fromUser))
            {
                Log.e(TAG, "chat_msg_right");
                convertView = mInflater.inflate(R.layout.chat_msg_left, null);
                avatarUrl = toAvatar;
            }else{
                Log.e(TAG, "chat_msg_left");
                convertView = mInflater.inflate(R.layout.chat_msg_right, null);
                avatarUrl = fromAvatar;
            }

            viewHolder = new ViewHolder();
            viewHolder.ivUserHead = (ImageView) convertView.findViewById(R.id.iv_userhead);
            viewHolder.tvSendTime = (TextView) convertView.findViewById(R.id.tv_sendtime);
            viewHolder.tvText = (TextView) convertView.findViewById(R.id.tv_text);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String thumbnailUrl = QiniuUtil.getInstance().getFileThumbnailUrl(avatarUrl, DisplayUtil.dip2px(mContext, 48), DisplayUtil.dip2px(mContext, 48));
        ImageSize imageSize = new ImageSize(DisplayUtil.dip2px(mContext, 48), DisplayUtil.dip2px(mContext, 48));
        final ViewHolder finalCardHolder = viewHolder;
        ImageLoader.getInstance().loadImage(thumbnailUrl, imageSize, IShareApplication.defaultOptions,
                new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        finalCardHolder.ivUserHead.setImageBitmap(loadedImage);
                    }
                });

        if(isShowTime(position)) {
            viewHolder.tvSendTime.setText(getTime(position));
        } else {
            viewHolder.tvSendTime.setVisibility(View.GONE);
        }

        viewHolder.tvText.setText(msg.content + "");

        return convertView;
    }

    public boolean isShowTime(int position) {
        boolean result =  true;
        String format = "yyyy-MM-dd HH:mm:ss";
        if(position > 0 && position + 1 < chatList.size()) {
            if(DateUtil.date2TimeStamp(chatList.get(position + 1).time, format)
                    - DateUtil.date2TimeStamp(chatList.get(position).time, format) < 300000) {
                result = false;
            }
        } else if(position == chatList.size() - 1) {
            if(DateUtil.getTimeStamp()
                    - DateUtil.date2TimeStamp(chatList.get(position).time, format) < 300000) {
                return false;
            }
        }
        return result;
    }

    public String getTime(int position) {
        String result = "";
        String curDate = DateUtil.getDate(0);
        String nextDate = DateUtil.getDate(1);
        Chat chat = chatList.get(position);

        if(curDate.compareTo(chat.time) > 0
                && nextDate.compareTo(chat.time) < 0) {
            result =  chat.time.substring(0, 11);
        } else {
            result = chat.time.substring(11, 19);
        }

        return result;
    }

    public void setChatList(List<Chat> chatList) {
        this.chatList = chatList;
    }

    class ViewHolder {
        public ImageView ivUserHead;
        public TextView tvSendTime;
        public TextView tvText;
        public ImageView ivPic;
        public ImageView ivSound;
        public TextView tvSoundTime;
    }
}
