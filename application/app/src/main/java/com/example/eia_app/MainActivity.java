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

        // Pobieramy kontroler nawigacji
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // Pobieramy graf nawigacji
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);

            // Sprawdzamy, czy użytkownik jest już skonfigurowany
            SharedPreferences prefs = getSharedPreferences("EIA_PREFS", MODE_PRIVATE);
            boolean isConfigured = prefs.getBoolean("is_configured", false);

            if (isConfigured) {
                // Jeśli tak, ustawiamy dashboard jako startowy ekran
                navGraph.setStartDestination(R.id.dashboardFragment);
            } else {
                // Jeśli nie, zostajemy przy ekranie połączenia (lub ustawiamy go jawnie)
                navGraph.setStartDestination(R.id.connectionFragment);
            }

            // Aplikujemy zmieniony graf do kontrolera
            navController.setGraph(navGraph);
        }
    }
}
