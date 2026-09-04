package com.eia.app;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import com.eia.app.repositories.MqttRepository;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);

            SharedPreferences prefs = getSharedPreferences("EIA_PREFS", MODE_PRIVATE);

            String mqttHost = prefs.getString("mqtt_host", "broker.hivemq.com");
            String mqttUser = prefs.getString("mqtt_user", "");
            String mqttPass = prefs.getString("mqtt_pass", "");
            
            MqttRepository mqtt = MqttRepository.getInstance();
            mqtt.configure(mqttHost, mqttUser, mqttPass);

            boolean isConfigured = prefs.getBoolean("is_configured", false);

            if (isConfigured) {
                navGraph.setStartDestination(R.id.dashboardFragment);
            } else {
                navGraph.setStartDestination(R.id.connectionFragment);
            }

            navController.setGraph(navGraph);

            setupNavigation(navController);
        }
    }

    private void setupNavigation(NavController navController) {
        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Automatyczna obsługa menu (Dashboard, Settings)
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.dashboardFragment) {
                navController.navigate(R.id.dashboardFragment);
            } else if (id == R.id.settingsFragment) {
                navController.navigate(R.id.settingsFragment);
            }
            drawer.closeDrawers();
            return true;
        });

        // Obsługa przycisku "O aplikacji" na dole menu
        findViewById(R.id.btnNavAbout).setOnClickListener(v -> {
            navController.navigate(R.id.aboutFragment);
            drawer.closeDrawers();
        });

        // Blokowanie menu na ekranach konfiguracji
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.connectionFragment || id == R.id.scanFragment || 
                id == R.id.configFragment || id == R.id.deviceSetupFragment) {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            } else {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        });
    }

    public void openDrawer() {
        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        if (drawer != null) {
            drawer.openDrawer(GravityCompat.START);
        }
    }
}
