package com.eia.app.fragments;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eia.app.MainActivity;
import com.eia.app.R;

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

        // Otwieranie panelu bocznego
        view.findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });

        // Obsługa linków w Credits i Source Code
        TextView tvCredits = view.findViewById(R.id.tvCredits);
        TextView tvSource = view.findViewById(R.id.tvSourceLink);

        if (tvCredits != null) {
            tvCredits.setText(Html.fromHtml(getString(R.string.about_credits_list), Html.FROM_HTML_MODE_COMPACT));
            tvCredits.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if (tvSource != null) {
            tvSource.setText(Html.fromHtml(getString(R.string.about_source_link), Html.FROM_HTML_MODE_COMPACT));
            tvSource.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
