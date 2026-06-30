package com.example.eia_app.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eia_app.R;
import com.example.eia_app.repositories.MqttRepository;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class SettingsFragment extends Fragment {

    private TextInputEditText etMqttHost, etMqttUser, etMqttPassword;
    private TextInputEditText etAiBaseUrl, etAiApiKey;
    private android.widget.AutoCompleteTextView actvAiProvider;
    private MqttRepository mqtt;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mqtt = MqttRepository.getInstance();
        prefs = requireActivity().getSharedPreferences("EIA_PREFS", Context.MODE_PRIVATE);

        // Nawigacja boczna
        com.google.android.material.navigation.NavigationView navigationView = view.findViewById(R.id.settings_nav_view);
        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(view);
        
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.settings_drawer_layout);
            
            if (id == R.id.dashboardFragment) {
                navController.navigate(R.id.dashboardFragment);
            }
            
            if (drawer != null) {
                drawer.closeDrawers();
            }
            return true;
        });

        // Otwieranie panelu bocznego
        view.findViewById(R.id.btnMenu).setOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.settings_drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });

        // Obsługa kliknięcia w "O aplikacji" na dole panelu bocznego
        View navAbout = view.findViewById(R.id.btnNavAbout);
        if (navAbout != null) {
            navAbout.setOnClickListener(v -> {
                androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.settings_drawer_layout);
                if (drawer != null) {
                    drawer.closeDrawers();
                }
                navController.navigate(R.id.aboutFragment);
            });
        }

        etMqttHost = view.findViewById(R.id.etMqttHost);
        etMqttUser = view.findViewById(R.id.etMqttUser);
        etMqttPassword = view.findViewById(R.id.etMqttPassword);

        // AI Views
        etAiBaseUrl = view.findViewById(R.id.etAiBaseUrl);
        etAiApiKey = view.findViewById(R.id.etAiApiKey);
        actvAiProvider = view.findViewById(R.id.actvAiProvider);

        setupAiProviderSpinner();
        loadSettings();

        view.findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());
    }

    private void setupAiProviderSpinner() {
        String[] providers = {"OpenAI API (ChatGPT)", "KoboldCPP", "Ollama", "LLMStudio", "Gemini API"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                providers
        );
        actvAiProvider.setAdapter(adapter);

        actvAiProvider.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            updateAiFields(selected);
        });
    }

    private void updateAiFields(String provider) {
        if ("Gemini API".equals(provider)) {
            etAiBaseUrl.setText("https://generativelanguage.googleapis.com/");
            etAiBaseUrl.setEnabled(false);
        } else if ("OpenAI API (ChatGPT)".equals(provider)) {
            etAiBaseUrl.setText("https://api.openai.com/v1/");
            etAiBaseUrl.setEnabled(true);
        } else {
            etAiBaseUrl.setEnabled(true);
            String currentUrl = etAiBaseUrl.getText() != null ? etAiBaseUrl.getText().toString() : "";
            if (currentUrl.contains("googleapis.com") || currentUrl.contains("api.openai.com")) {
                etAiBaseUrl.setText("http://localhost:11434/");
            }
        }
    }

    private void loadSettings() {
        etMqttHost.setText(prefs.getString("mqtt_host", "broker.hivemq.com"));
        etMqttUser.setText(prefs.getString("mqtt_user", ""));
        etMqttPassword.setText(prefs.getString("mqtt_pass", ""));

        // AI load
        String provider = prefs.getString("ai_provider", "OpenAI API (ChatGPT)");
        actvAiProvider.setText(provider, false);
        etAiBaseUrl.setText(prefs.getString("ai_base_url", "https://api.openai.com/v1/"));
        etAiApiKey.setText(prefs.getString("ai_api_key", ""));

        if ("Gemini API".equals(provider)) {
            etAiBaseUrl.setEnabled(false);
        }
    }

    private void saveSettings() {
        String host = Objects.requireNonNull(etMqttHost.getText()).toString().trim();
        String user = Objects.requireNonNull(etMqttUser.getText()).toString().trim();
        String pass = Objects.requireNonNull(etMqttPassword.getText()).toString().trim();

        // AI values
        String aiProvider = actvAiProvider.getText().toString();
        String aiBaseUrl = Objects.requireNonNull(etAiBaseUrl.getText()).toString().trim();
        String aiApiKey = Objects.requireNonNull(etAiApiKey.getText()).toString().trim();

        boolean hasError = false;

        if (host.isEmpty()) {
            etMqttHost.setError("Adres brokera MQTT jest wymagany");
            hasError = true;
        }

        if (user.isEmpty()) {
            etMqttUser.setError("Użytkownik MQTT jest wymagany");
            hasError = true;
        }

        if (pass.isEmpty()) {
            etMqttPassword.setError("Hasło MQTT jest wymagane");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        prefs.edit()
                .putString("mqtt_host", host)
                .putString("mqtt_user", user)
                .putString("mqtt_pass", pass)
                .putString("ai_provider", aiProvider)
                .putString("ai_base_url", aiBaseUrl)
                .putString("ai_api_key", aiApiKey)
                .apply();

        mqtt.disconnectFromBroker();
        mqtt.configure(host, user, pass);
        mqtt.connectToBroker();

        Toast.makeText(getContext(), "Ustawienia zapisane", Toast.LENGTH_SHORT).show();
    }
}
