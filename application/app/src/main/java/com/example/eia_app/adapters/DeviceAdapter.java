package com.example.eia_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eia_app.R;
import com.example.eia_app.models.Device;

import java.util.List;

public class DeviceAdapter extends ListAdapter<Device, DeviceAdapter.ViewHolder> {
    private final OnDeviceClickListener clickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public DeviceAdapter(OnDeviceClickListener clickListener) {
        super(new DiffCallback());
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
        Device device = getItem(position);

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
        // Tu pętla po czujnikach na później

        holder.itemView.setOnClickListener(v -> clickListener.onDeviceClick(device));
    }

    static class DiffCallback extends DiffUtil.ItemCallback<Device> {
        @Override
        public boolean areItemsTheSame(@NonNull Device oldItem, @NonNull Device newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Device oldItem, @NonNull Device newItem) {
            // Zakładając, że Device ma poprawne porównywanie lub sprawdzamy kluczowe pola
            return oldItem.getName().equals(newItem.getName()) && 
                   oldItem.isOnline() == newItem.isOnline();
        }
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
