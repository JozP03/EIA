package com.example.eia_app.fragments;

import android.os.Bundle;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.eia_app.R;
import com.example.eia_app.repositories.MqttRepository;
import com.example.eia_app.viewModels.DashboardViewModel;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    
    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        viewModel.initMqttConnection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView textView = view.findViewById(R.id.tvTemperature);
        viewModel.getTemperature().observe(getViewLifecycleOwner(), temperature -> {
            if(temperature != null) {
                textView.setText(String.format(Locale.US, "%.2f °C", temperature));
            }
        });
    }
    @Override
    public void onDestroy() {

        MqttRepository.getInstance().disconnectFromBroker();
        super.onDestroy();
    }
}