package com.eia.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.eia.app.R;
import com.eia.app.db.SensorReading;
import com.eia.app.models.Sensor;
import com.eia.app.viewModels.DashboardViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SensorGroupAdapter extends ListAdapter<String, SensorGroupAdapter.ViewHolder> {

    private final DashboardViewModel viewModel;
    private final LifecycleOwner lifecycleOwner;
    private final Map<String, List<Sensor>> sensorGroups = new HashMap<>();
    private final OnSensorLongClickListener longClickListener;

    public interface OnSensorLongClickListener {
        void onSensorLongClick(String physicalId, String currentName);
    }

    public SensorGroupAdapter(DashboardViewModel viewModel, LifecycleOwner lifecycleOwner, OnSensorLongClickListener longClickListener) {
        super(new StringDiffCallback());
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.longClickListener = longClickListener;
    }

    public void updateData(List<Sensor> sensors) {
        sensorGroups.clear();
        List<String> physicalIds = new ArrayList<>();
        
        for (Sensor s : sensors) {
            String pid = s.getPhysicalId();
            if (pid == null) pid = "Unknown";
            
            if (!sensorGroups.containsKey(pid)) {
                sensorGroups.put(pid, new ArrayList<>());
                physicalIds.add(pid);
            }
            sensorGroups.get(pid).add(s);
        }
        submitList(physicalIds);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String physicalId = getItem(position);
        List<Sensor> sensorsInGroup = sensorGroups.get(physicalId);
        
        if (sensorsInGroup == null || sensorsInGroup.isEmpty()) return;

        Sensor first = sensorsInGroup.get(0);
        holder.tvTitle.setText(first.getName());

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onSensorLongClick(physicalId, first.getName());
            }
            return true;
        });

        holder.measuresContainer.removeAllViews();
        for (Sensor s : sensorsInGroup) {
            addMeasureRow(holder.measuresContainer, s);
        }
    }

    private void addMeasureRow(LinearLayout container, Sensor sensor) {
        View row = LayoutInflater.from(container.getContext()).inflate(R.layout.item_measure_row, container, false);
        
        TextView tvLabel = row.findViewById(R.id.tvMeasureLabel);
        TextView tvValue = row.findViewById(R.id.tvMeasureValue);
        View statusIco = row.findViewById(R.id.measureStatusIco);
        LineChart chart = row.findViewById(R.id.measureChart);

        String typeName;
        switch (sensor.getPrefix()) {
            case "T": typeName = "Temperatura"; break;
            case "H": typeName = "Wilgotność"; break;
            case "P": typeName = "Ciśnienie"; break;
            case "L": typeName = "Jasność"; break;
            case "V": typeName = "Napięcie"; break;
            default: typeName = "Odczyt " + sensor.getPrefix(); break;
        }
        tvLabel.setText(typeName);

        if (sensor.isHasError()) {
            tvValue.setText("--");
            statusIco.setBackgroundColor(container.getContext().getColor(R.color.accent_red));
            chart.setVisibility(View.GONE);
        } else {
            tvValue.setText(String.format(Locale.getDefault(), "%.1f %s", sensor.getValue(), sensor.getUnit()));
            statusIco.setBackgroundColor(container.getContext().getColor(R.color.accent_green));
            chart.setVisibility(View.VISIBLE);
            
            viewModel.getReadingsForSensor(sensor.getId()).observe(lifecycleOwner, readings -> {
                if (readings != null && !readings.isEmpty()) {
                    setupChart(chart, readings, sensor.getPrefix());
                }
            });
        }

        container.addView(row);
    }

    private void setupChart(LineChart chart, List<SensorReading> readings, String prefix) {
        List<Entry> entries = new ArrayList<>();
        float dataMin = Float.MAX_VALUE;
        float dataMax = Float.MIN_VALUE;

        for (int i = 0; i < readings.size(); i++) {
            float val = readings.get(i).getValue();
            entries.add(new Entry(i, val));
            if (val < dataMin) dataMin = val;
            if (val > dataMax) dataMax = val;
        }

        // zakresy
        float min = 0f;
        float max = 100f;

        switch (prefix) {
            case "T": // Temperatura 0-40
                min = 0f; max = 40f; break;
            case "H": // Wilgotność 0-100
                min = 0f; max = 100f; break;
            case "P": // Ciśnienie 950-1050
                min = 950f; max = 1050f; break;
            case "L": // Jasność 0-1000
                min = 0f; max = 1000f; break;
            case "V": // Napięcie 0-5
                min = 0f; max = 5f; break;
            default:
                min = dataMin - 5f; max = dataMax + 5f;
        }

        if (dataMin < min) min = dataMin - (prefix.equals("P") ? 10f : 5f);
        if (dataMax > max) max = dataMax + (prefix.equals("P") ? 10f : 5f);

        int accentGreen = chart.getContext().getColor(R.color.accent_green);
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(accentGreen);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(accentGreen);
        dataSet.setFillAlpha(20);

        chart.setData(new LineData(dataSet));
        chart.getAxisLeft().setAxisMinimum(min);
        chart.getAxisLeft().setAxisMaximum(max);
        
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setTextColor(chart.getContext().getColor(R.color.text_muted));
        chart.getAxisLeft().setTextSize(10f);
        chart.setTouchEnabled(false);
        chart.invalidate();
    }

    static class StringDiffCallback extends DiffUtil.ItemCallback<String> {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        LinearLayout measuresContainer;
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPhysicalSensorName);
            measuresContainer = itemView.findViewById(R.id.layoutMeasuresContainer);
        }
    }
}
