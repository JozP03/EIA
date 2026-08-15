package com.eia.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eia.app.R;
import com.eia.app.adapters.SensorCardAdapter;
import com.eia.app.models.Device;
import com.eia.app.models.Sensor;
import com.eia.app.viewModels.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

public class DeviceDetailsFragment extends Fragment {

    private String deviceId;
    private DashboardViewModel viewModel;
    private SensorCardAdapter adapter;
    private TextView tvDeviceName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvDeviceName = view.findViewById(R.id.tvDeviceName);
        RecyclerView rvSensors = view.findViewById(R.id.rvSensorCards);
        TextView tvEmpty = view.findViewById(R.id.tvEmptySensors);
        
        view.findViewById(R.id.btnBack).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        adapter = new SensorCardAdapter(viewModel, getViewLifecycleOwner());
        rvSensors.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSensors.setAdapter(adapter);

        viewModel.getDevices().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null && deviceId != null) {
                for (Device device : devices) {
                    if (device.getId().equals(deviceId)) {
                        tvDeviceName.setText(device.getName());
                        List<Sensor> sensorList = device.getSensorList();
                        
                        if (sensorList == null || sensorList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            rvSensors.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvSensors.setVisibility(View.VISIBLE);
                            adapter.submitList(new ArrayList<>(sensorList));
                        }
                        break;
                    }
                }
            }
        });
    }
}
