package com.example.eia_app.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.example.eia_app.R;


public class ConfigFragment extends Fragment {

    private String ssid;

    public ConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getArguments() != null) {
            ssid = getArguments().getString("selected_ssid");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvSelectedWifi = view.findViewById(R.id.tvSelectedWifiName);
        if(ssid != null) {
            tvSelectedWifi.setText(ssid);
        }

        EditText etPassword = view.findViewById(R.id.etPassword);
        EditText etIpAddress = view.findViewById(R.id.etStaticIpAddress);
        EditText etGateway = view.findViewById(R.id.etStaticGateway);
        EditText etSubnet = view.findViewById(R.id.etStaticNetmask);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_configFragment_to_scanFragment);
        });

        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            String ip = etIpAddress.getText().toString().trim();
            String gateway = etGateway.getText().toString().trim();
            String subnet = etSubnet.getText().toString().trim();

            // wyslanie do servisu
        });
    }
}