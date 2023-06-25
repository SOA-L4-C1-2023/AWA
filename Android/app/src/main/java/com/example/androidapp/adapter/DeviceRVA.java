package com.example.androidapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidapp.R;
import com.example.androidapp.model.Device;
import com.example.androidapp.view_holder.DeviceVH;

import java.util.Collections;
import java.util.List;

public class DeviceRVA extends RecyclerView.Adapter<DeviceVH> {

    private List<Device> devices = Collections.emptyList();
    Context context;

    public DeviceRVA(List<Device> devices, Context context) {
        this.devices = devices;
        this.context = context;
    }

    @Override
    public DeviceVH onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflate the layout, initialize the view holder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_layout, parent, false);
        DeviceVH holder = new DeviceVH(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(DeviceVH holder, int position) {
        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        holder.title.setText(devices.get(position).title);
        holder.address.setText(devices.get(position).address);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    // Insert a new item to the RecyclerView on a predefined position
    public void insert(int position, Device device){
        devices.add(position, device);
        notifyItemInserted(position);
    }

    // Remove a RecyclerView item containing a specified Data object
    public void remove(Device device){
        int position = devices.indexOf(device);
        devices.remove(position);
        notifyItemRemoved(position);
    }
}
