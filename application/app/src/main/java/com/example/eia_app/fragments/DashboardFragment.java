package com.example.eia_app.fragments;

import android.os.Bundle;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

        com.google.android.material.navigation.NavigationView navigationView = view.findViewById(R.id.dashboard_nav_view);
        NavController navController = Navigation.findNavController(view);
        NavigationUI.setupWithNavController(navigationView, navController);

        // reset konfiguracji testowo na przycisku +
        view.findViewById(R.id.btnAddDevice).setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Resetowanie konfiguracji")
                    .setMessage("Czy na pewno chcesz usunąć ustawienia i skonfigurować urządzenie ponownie?")
                    .setPositiveButton("Tak", (dialog, which) -> {
                        requireActivity().getSharedPreferences("EIA_PREFS", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("is_configured", false).apply();

                        MqttRepository.getInstance().disconnectFromBroker();

                        androidx.navigation.Navigation.findNavController(view)
                                .navigate(R.id.action_dashboardFragment_to_connectionFragment);
                    })
                    .setNegativeButton("Anuluj", null)
                    .show();
        });

        // Otwieranie panelu bocznego
        view.findViewById(R.id.btnMenu).setOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.dashboard_drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });

        // przysk o aplikacji
        View navAbout = view.findViewById(R.id.aboutFragment);
        if (navAbout != null) {
            navAbout.setOnClickListener(v -> {
                androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.dashboard_drawer_layout);
                if (drawer != null) {
                    drawer.closeDrawers();
                }
                Navigation.findNavController(view).navigate(R.id.aboutFragment);
            });
        }


        TextView textView = view.findViewById(R.id.tvTemperature);
        viewModel.getTemperature().observe(getViewLifecycleOwner(), temperature -> {
            if(temperature != null) {
                textView.setText(String.format(Locale.US, "%.2f °C", temperature));
            }
        });

        TextView statusTextView = view.findViewById(R.id.tvStatus);
        View statusIconView = view.findViewById(R.id.tvStatusIco);
        viewModel.getStatus().observe(getViewLifecycleOwner(), status -> {
            if(status != null) {
                if(status.equalsIgnoreCase("ONLINE")){
                    statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green));
                    statusIconView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_green));
                } else {
                    statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_red));
                    statusIconView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_red));
                }
                statusTextView.setText(status);
            }
        });
    }
    @Override
    public void onDestroy() {

        MqttRepository.getInstance().disconnectFromBroker();
        super.onDestroy();
    }
}