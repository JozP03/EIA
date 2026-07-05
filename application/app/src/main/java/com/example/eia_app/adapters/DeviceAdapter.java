package com.example.eia_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eia_app.R;
import com.example.eia_app.models.Device;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private final List<Device> devices;
    private final OnDeviceClickListener clickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public DeviceAdapter(List<Device> devices, OnDeviceClickListener clickListener) {
        this.devices = devices;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = devices.get(position);

        holder.tvDeviceName.setText(device.getName());

        // Obsługa statusu
        if (device.isOnline()) {
            holder.tvStatus.setText("ONLINE");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.accent_green));
            holder.tvStatusIco.setBackgroundColor(holder.itemView.getContext().getColor(R.color.accent_green));
        } else {
            holder.tvStatus.setText("OFFLINE");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.text_muted));
            holder.tvStatusIco.setBackgroundColor(holder.itemView.getContext().getColor(R.color.text_muted));
        }


        holder.sensorsContainer.removeAllViews();
        // Tu pętla po czujnikach na pozniej

        holder.itemView.setOnClickListener(v -> clickListener.onDeviceClick(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName, tvStatus;
        View tvStatusIco;
        LinearLayout sensorsContainer;

        ViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvStatusIco = itemView.findViewById(R.id.tvStatusIco);
            sensorsContainer = itemView.findViewById(R.id.layoutSensorsContainer);
        }
    }
}
