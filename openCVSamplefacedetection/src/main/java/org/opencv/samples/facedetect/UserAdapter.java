package org.opencv.samples.facedetect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * 用户信息适配器
 * Created by Luke on 2017/8/21.
 */

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<UserInfo> users;
    private Context mContext;

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        ImageView userFace;
        TextView userName;
        TextView userSex;
        TextView userAge;

        public ViewHolder(View itemView) {
            super(itemView);
            linearLayout = (LinearLayout) itemView;
            userFace = (ImageView) itemView.findViewById(R.id.user_face);
            userName = (TextView) itemView.findViewById(R.id.user_name);
            userSex = (TextView) itemView.findViewById(R.id.user_sex);
            userAge = (TextView) itemView.findViewById(R.id.user_age);
        }
    }

    public UserAdapter(List<UserInfo> users) {
        this.users = users;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item,
                parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        UserInfo user = users.get(position);
        holder.userName.setText(holder.userName.getText().toString() + user.getName());
        holder.userSex.setText(holder.userSex.getText().toString() + user.getSex());
        holder.userAge.setText(holder.userAge.getText().toString() + user.getAge());
        Bitmap bitmap = BitmapFactory.decodeFile(user.getPath());
        holder.userFace.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}
