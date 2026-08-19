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

    public SensorGroupAdapter(DashboardViewModel viewModel, LifecycleOwner lifecycleOwner) {
        super(new StringDiffCallback());
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
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
        holder.tvTitle.setText(first.getName() + " (" + physicalId + ")");

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
                    setupChart(chart, readings);
                }
            });
        }

        container.addView(row);
    }

    private void setupChart(LineChart chart, List<SensorReading> readings) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < readings.size(); i++) {
            entries.add(new Entry(i, readings.get(i).getValue()));
        }
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#22C55E"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#22C55E"));
        dataSet.setFillAlpha(20);

        chart.setData(new LineData(dataSet));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setTextSize(8f);
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
