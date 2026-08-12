package com.eia.app.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.eia.app.R;
import com.eia.app.models.Sensor;

import java.util.Locale;

public class SensorCardAdapter extends ListAdapter<Sensor, SensorCardAdapter.ViewHolder> {

    public SensorCardAdapter() {
        super(new DiffCallback());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sensor sensor = getItem(position);

        holder.tvName.setText(sensor.getName());

        if (sensor.isHasError()) {
            holder.tvValue.setText("--");
            holder.ivIcon.setImageResource(android.R.drawable.stat_notify_error);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.accent_red)));
        } else {
            String formattedValue = String.format(Locale.getDefault(), "%.1f %s", sensor.getValue(), sensor.getUnit());
            holder.tvValue.setText(formattedValue);
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_compass);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.accent_blue)));
        }
        
        // logika wykresu
    }

    static class DiffCallback extends DiffUtil.ItemCallback<Sensor> {
        @Override
        public boolean areItemsTheSame(@NonNull Sensor oldItem, @NonNull Sensor newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Sensor oldItem, @NonNull Sensor newItem) {
            return oldItem.equals(newItem);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvValue;
        ImageView ivIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSensorName);
            tvValue = itemView.findViewById(R.id.tvSensorValue);
            ivIcon = itemView.findViewById(R.id.ivSensorIcon);
        }
    }
}
