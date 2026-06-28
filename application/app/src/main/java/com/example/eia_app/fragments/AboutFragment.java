package com.example.eia_app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.eia_app.R;
import com.google.android.material.navigation.NavigationView;

public class AboutFragment extends Fragment {

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Nawigacja boczna
        NavigationView navigationView = view.findViewById(R.id.about_nav_view);
        NavController navController = Navigation.findNavController(view);
        
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.about_drawer_layout);
            
            if (id == R.id.dashboardFragment) {
                navController.navigate(R.id.dashboardFragment);
            } else if (id == R.id.settingsFragment) {
                navController.navigate(R.id.settingsFragment);
            }
            
            if (drawer != null) {
                drawer.closeDrawers();
            }
            return true;
        });

        // Otwieranie panelu bocznego
        view.findViewById(R.id.btnMenu).setOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.about_drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });

        // Obsługa kliknięcia w "O aplikacji" na dole panelu bocznego
        View navAbout = view.findViewById(R.id.btnNavAbout);
        if (navAbout != null) {
            navAbout.setOnClickListener(v -> {
                androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.about_drawer_layout);
                if (drawer != null) {
                    drawer.closeDrawers();
                }
                // Jesteśmy już tutaj, więc tylko zamykamy drawer
            });
        }
    }
}
