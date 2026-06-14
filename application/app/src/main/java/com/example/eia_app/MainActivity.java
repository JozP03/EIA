package com.example.eia_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);

            SharedPreferences prefs = getSharedPreferences("EIA_PREFS", MODE_PRIVATE);
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
