package com.eia.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.eia.app.R;
import com.eia.app.models.Device;
import com.eia.app.models.Sensor;

import java.util.List;

public class DeviceAdapter extends ListAdapter<Device, DeviceAdapter.ViewHolder> {
    private final OnDeviceClickListener clickListener;
    private final OnDeviceLongClickListener longClickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public interface OnDeviceLongClickListener {
        void onDeviceLongClick(Device device);
    }

    public DeviceAdapter(OnDeviceClickListener clickListener, OnDeviceLongClickListener longClickListener) {
        super(new DiffCallback());
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
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
        boolean hasSensors = false;
        // Tu pętla po czujnikach
        if (device.getSensorList() != null) {
            for (Sensor sensor : device.getSensorList()) {
                // Na dashboardzie jedynie glowny
                if (!sensor.isPrimary()) continue;

                hasSensors = true;
                View sensorView = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_sensor_row, holder.sensorsContainer, false);

                android.widget.TextView tvName = sensorView.findViewById(R.id.tvSensorName);
                android.widget.TextView tvValue = sensorView.findViewById(R.id.tvSensorValue);
                View ivIcon = sensorView.findViewById(R.id.ivSensorIcon);

                if (sensor.isHasError()) {
                    tvName.setText(sensor.getName());
                    tvValue.setText("--");
                    ivIcon.setBackgroundColor(holder.itemView.getContext().getColor(R.color.accent_red));
                } else {
                    tvName.setText(sensor.getName());
                    // Format (np. "22.5 °C")
                    String formattedValue = String.format(java.util.Locale.getDefault(),
                            "%.1f %s", sensor.getValue(), sensor.getUnit());
                    tvValue.setText(formattedValue);
                    ivIcon.setBackgroundColor(holder.itemView.getContext().getColor(R.color.accent_green));
                }

                holder.sensorsContainer.addView(sensorView);
            }
        }

        if (!hasSensors) {
            TextView tvNoSensors = new TextView(holder.itemView.getContext());
            tvNoSensors.setText("Brak podłączonych sensorów");
            tvNoSensors.setTextSize(12);
            tvNoSensors.setTextColor(holder.itemView.getContext().getColor(R.color.text_muted));
            tvNoSensors.setPadding(0, 8, 0, 0);
            holder.sensorsContainer.addView(tvNoSensors);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onDeviceClick(device));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onDeviceLongClick(device);
            return true;
        });
    }

    static class DiffCallback extends DiffUtil.ItemCallback<Device> {
        @Override
        public boolean areItemsTheSame(@NonNull Device oldItem, @NonNull Device newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Device oldItem, @NonNull Device newItem) {
            return oldItem.equals(newItem);
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
