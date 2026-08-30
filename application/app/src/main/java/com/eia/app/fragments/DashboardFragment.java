package com.eia.app.fragments;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.eia.app.R;
import com.eia.app.adapters.DeviceAdapter;
import com.eia.app.adapters.ChatAdapter;
import com.eia.app.models.ChatMessage;
import com.eia.app.models.Device;
import com.eia.app.providers.AiFactory;
import com.eia.app.providers.AiProvider;
import com.eia.app.repositories.MqttRepository;
import com.eia.app.viewModels.DashboardViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    
    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (viewModel != null) {
            viewModel.initMqttConnection();
        }
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
        
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.dashboard_drawer_layout);
            
            if (id == R.id.settingsFragment) {
                navController.navigate(R.id.settingsFragment);
            }
            
            if (drawer != null) {
                drawer.closeDrawers();
            }
            return true;
        });

        // przycisk +
        view.findViewById(R.id.btnAddDevice).setOnClickListener(v -> {
            navController.navigate(R.id.action_dashboardFragment_to_connectionFragment);
        });

        //panel boczny
        view.findViewById(R.id.btnMenu).setOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.dashboard_drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });

        // przysk o aplikacji
        View navAbout = view.findViewById(R.id.btnNavAbout);
        if (navAbout != null) {
            navAbout.setOnClickListener(v -> {
                androidx.drawerlayout.widget.DrawerLayout drawer = view.findViewById(R.id.dashboard_drawer_layout);
                if (drawer != null) {
                    drawer.closeDrawers();
                }
                navController.navigate(R.id.aboutFragment);
            });
        }

        androidx.recyclerview.widget.RecyclerView rv = view.findViewById(R.id.rvDeviceList);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyList);

        com.eia.app.adapters.DeviceAdapter adapter = new com.eia.app.adapters.DeviceAdapter(device -> {
            // przejście do szczegółów urządzenia
            Bundle args = new Bundle();
            args.putString("deviceId", device.getId());
            navController.navigate(R.id.action_dashboardFragment_to_deviceDetailsFragment, args);
        }, device -> {
            showDeviceActions(device);
        });

        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        viewModel.getDevices().observe(getViewLifecycleOwner(), newDevices -> {
            if (newDevices != null) {
                // ListAdapter
                adapter.submitList(new ArrayList<>(newDevices));

                // Obsługa napisu pustej listy
                if (newDevices.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                }
            }
        });

        // Obsługa dymka AI
        View fabAi = view.findViewById(R.id.fabAiChat);
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("EIA_PREFS", android.content.Context.MODE_PRIVATE);
        String aiKey = prefs.getString("ai_api_key", "");
        
        Log.d("DashboardFragment", "Klucz AI: [" + aiKey + "]");

        if (!aiKey.trim().isEmpty()) {
            fabAi.setVisibility(View.VISIBLE);
            fabAi.setOnClickListener(v -> {
                showAiChat();
            });
        }

    }

    private void showAiChat() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_ai_chat, null);
        
        view.findViewById(R.id.btnCloseChat).setOnClickListener(v -> bottomSheet.dismiss());

        androidx.recyclerview.widget.RecyclerView rv = view.findViewById(R.id.rvChatMessages);
        ChatAdapter chatAdapter = new ChatAdapter();
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        rv.setAdapter(chatAdapter);

        android.widget.ProgressBar progressBar = view.findViewById(R.id.pbAiLoading);
        android.widget.EditText etMessage = view.findViewById(R.id.etChatMessage);
        view.findViewById(R.id.btnSendMessage).setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                chatAdapter.addMessage(new ChatMessage(text, ChatMessage.Type.USER));
                rv.scrollToPosition(chatAdapter.getItemCount() - 1);
                etMessage.setText("");

                progressBar.setVisibility(View.VISIBLE);

                AiProvider provider = AiFactory.getProvider(requireContext());
                provider.askAi(text, new AiProvider.AiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                chatAdapter.addMessage(new ChatMessage(response, ChatMessage.Type.AI));
                                rv.scrollToPosition(chatAdapter.getItemCount() - 1);
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                chatAdapter.addMessage(new ChatMessage("Błąd AI: " + error, ChatMessage.Type.AI));
                                rv.scrollToPosition(chatAdapter.getItemCount() - 1);
                            });
                        }
                    }
                });
            }
        });

        bottomSheet.setContentView(view);
        bottomSheet.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        bottomSheet.show();
    }

    private void showDeviceActions(Device device) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_device_actions, null);

        android.widget.EditText etName = view.findViewById(R.id.etDeviceName);
        etName.setText(device.getName());

        view.findViewById(R.id.btnSaveName).setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty()) {
                device.setName(newName);
                viewModel.saveDevice(device);
                bottomSheet.dismiss();
            } else {
                Toast.makeText(getContext(), "Nazwa nie może być pusta!", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnDeleteDevice).setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Usuń urządzenie")
                    .setMessage("Czy na pewno chcesz usunąć " + device.getName() + "?")
                    .setPositiveButton("Usuń", (dialog, which) -> {
                        viewModel.deleteDevice(device.getId());
                        bottomSheet.dismiss();
                    })
                    .setNegativeButton("Anuluj", null)
                    .show();
        });

        bottomSheet.setContentView(view);
        bottomSheet.show();
    }

    @Override
    public void onDestroy() {

        MqttRepository.getInstance().disconnectFromBroker();
        super.onDestroy();
    }
}
