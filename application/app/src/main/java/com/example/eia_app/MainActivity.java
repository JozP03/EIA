package com.example.eia_app;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import com.example.eia_app.repositories.MqttRepository;

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
        }
    }
}
