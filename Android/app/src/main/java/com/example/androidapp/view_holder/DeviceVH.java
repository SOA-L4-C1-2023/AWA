package com.example.androidapp.view_holder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.androidapp.R;

public class DeviceVH extends RecyclerView.ViewHolder {
    public TextView title;
    public TextView address;

    public DeviceVH(View itemView) {
        super(itemView);
        title = (TextView) itemView.findViewById(R.id.title);
        address = (TextView) itemView.findViewById(R.id.address);
    }
}
