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

import android.graphics.Color;
import androidx.lifecycle.LifecycleOwner;
import com.eia.app.db.SensorReading;
import com.eia.app.viewModels.DashboardViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SensorCardAdapter extends ListAdapter<Sensor, SensorCardAdapter.ViewHolder> {

    private final DashboardViewModel viewModel;
    private final LifecycleOwner lifecycleOwner;

    public SensorCardAdapter(DashboardViewModel viewModel, LifecycleOwner lifecycleOwner) {
        super(new DiffCallback());
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
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
            holder.ivIcon.setBackgroundColor(holder.itemView.getContext().getColor(R.color.accent_red));
            holder.sensorChart.setVisibility(View.GONE);
        } else {
            String formattedValue = String.format(Locale.getDefault(), "%.1f %s", sensor.getValue(), sensor.getUnit());
            holder.tvValue.setText(formattedValue);
            holder.ivIcon.setBackgroundColor(holder.itemView.getContext().getColor(R.color.accent_green));
            holder.sensorChart.setVisibility(View.VISIBLE);
            
            viewModel.getReadingsForSensor(sensor.getId()).observe(lifecycleOwner, readings -> {
                if (readings != null && !readings.isEmpty()) {
                    setupChart(holder.sensorChart, readings, sensor.getUnit());
                }
            });
        }
    }

    private void setupChart(LineChart chart, List<SensorReading> readings, String unit) {
        if (readings.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        long firstTimestamp = readings.get(0).getTimestamp();

        for (SensorReading reading : readings) {
            float x = (float) (reading.getTimestamp() - firstTimestamp) / 1000f; 
            entries.add(new Entry(x, reading.getValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, unit);
        dataSet.setColor(Color.parseColor("#22C55E")); // accent_green
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#22C55E"));
        dataSet.setFillAlpha(30);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setTextColor(Color.GRAY);
        chart.getXAxis().setEnabled(false);
        
        chart.setTouchEnabled(false);
        chart.invalidate();
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
        View ivIcon;
        LineChart sensorChart;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSensorName);
            tvValue = itemView.findViewById(R.id.tvSensorValue);
            ivIcon = itemView.findViewById(R.id.ivSensorIcon);
            sensorChart = itemView.findViewById(R.id.sensorChart);
        }
    }
}
