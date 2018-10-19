package com.ibm.watson.developer_cloud.android.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import android.text.Html;
import android.os.Build;


public class MessageAdapter extends ArrayAdapter<String[]> {

    private static final String TAG = "MessageAdapter";
    private Context mContext;
    int mResource;

    public MessageAdapter(@NonNull Context context, int resource, @NonNull ArrayList<String[]> objects) {
        super(context, resource, objects);
        //this.mContext = mContext;
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //return super.getView(position, convertView, parent);

        String user = getItem(position)[0];
        String type = getItem(position)[2];
        String msj = "";
        String ima = "https://image.ibb.co/jMVGYK/White_Pixels.png";
        Bitmap image;

        if (type.equals("text")) {
            msj = getItem(position)[1];
        } else if (type.equals("image")) {
            ima = getItem(position)[1];
        }


        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);
        TextView tvUser = (TextView) convertView.findViewById(R.id.UserTextView);
        TextView tvMsj = (TextView) convertView.findViewById(R.id.MessageTextView);
        ImageView ivIma = (ImageView) convertView.findViewById(R.id.ImageView);

        try {
            image = new DownloadImageTask(ivIma).execute(ima).get();
        } catch (InterruptedException e) {
            image = null;
        } catch (ExecutionException e) {
            image = null;
        }

        ivIma.setImageBitmap(image);
        tvUser.setText(user);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvMsj.setText(Html.fromHtml(msj, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvMsj.setText(Html.fromHtml(msj));
        }
        return convertView;
    }

}
