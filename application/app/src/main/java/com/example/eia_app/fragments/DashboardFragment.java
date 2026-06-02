package com.example.eia_app.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.eia_app.R;
import com.example.eia_app.repositories.MqttRepository;
import com.example.eia_app.viewModels.DashboardViewModel;

public class DashboardFragment extends Fragment {

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DashboardViewModel viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        viewModel.initMqttConnection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}