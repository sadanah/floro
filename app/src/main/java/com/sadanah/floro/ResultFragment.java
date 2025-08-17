package com.sadanah.floro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class ResultFragment extends Fragment {

    private static final String ARG_DISEASE_NAME = "arg_disease_name";
    private static final String ARG_CONFIDENCE   = "arg_confidence"; // 0..1

    public static ResultFragment newInstance(@NonNull String diseaseName, float confidence) {
        Bundle b = new Bundle();
        b.putString(ARG_DISEASE_NAME, diseaseName);
        b.putFloat(ARG_CONFIDENCE, confidence);
        ResultFragment f = new ResultFragment();
        f.setArguments(b);
        return f;
    }

    private String diseaseName;
    private float confidence;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            diseaseName = args.getString(ARG_DISEASE_NAME);
            confidence = args.getFloat(ARG_CONFIDENCE, 0f);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvConf  = view.findViewById(R.id.tv_confidence);
        TextView tvPlant = view.findViewById(R.id.tv_plant);
        TextView tvType  = view.findViewById(R.id.tv_type);
        TextView tvDescH = view.findViewById(R.id.tv_description_header);
        TextView tvDesc  = view.findViewById(R.id.tv_description);
        TextView tvTreatH= view.findViewById(R.id.tv_treatment_header);
        TextView tvTreat = view.findViewById(R.id.tv_treatment);
        TextView tvProdH = view.findViewById(R.id.tv_products_header);
        LinearLayout productsContainer = view.findViewById(R.id.products_container);

        tvTitle.setText(diseaseName == null ? "Unknown" : diseaseName);
        tvConf.setText(String.format(Locale.US, "Confidence: %.1f%%", confidence * 100f));

        DBHelper db = new DBHelper(requireContext());

        DBHelper.DiseaseDetails details = null;
        if (diseaseName != null) {
            // Try a straight lookup by name first
            details = db.getDiseaseDetailsByName(diseaseName);
            // If that fails, try the label-normalizing helper
            if (details == null) {
                details = db.getDiseaseDetailsForModelLabel(diseaseName);
            }
        }

        if (details == null) {
            // Handle "healthy" or unknown gracefully
            tvPlant.setText("Plant: —");
            tvType.setText("Type: —");
            tvDesc.setText("No disease information found.");
            tvTreat.setText("—");
            tvDescH.setVisibility(View.VISIBLE);
            tvTreatH.setVisibility(View.VISIBLE);
            tvProdH.setVisibility(View.VISIBLE);
            productsContainer.removeAllViews();
            return;
        }

        // Plant
        if (details.plant != null) {
            tvPlant.setText("Plant: " + details.plant.plantName);
        } else {
            tvPlant.setText("Plant: —");
        }

        // Disease info
        tvType.setText("Type: " + (details.disease.diseaseType == null ? "—" : details.disease.diseaseType));
        tvDesc.setText(details.disease.diseaseDescription == null ? "—" : details.disease.diseaseDescription);
        tvTreat.setText(details.disease.diseaseTreatment == null ? "—" : details.disease.diseaseTreatment);

        // Products
        productsContainer.removeAllViews();
        if (details.products.isEmpty()) {
            TextView none = makeSmallText("No linked products.");
            productsContainer.addView(none);
        } else {
            for (DBHelper.Product p : details.products) {
                View row = makeProductRow(p);
                productsContainer.addView(row);
            }
        }
    }

    private TextView makeSmallText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(14f);
        return tv;
    }

    private View makeProductRow(DBHelper.Product p) {
        // Simple vertical row: Name (bold-ish), optional price, clickable link if present
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView name = new TextView(requireContext());
        name.setText(p.productName);
        name.setTextSize(16f);

        row.addView(name);

        if (p.productPrice != null) {
            TextView price = makeSmallText(String.format(Locale.US, "Approx. price: %.2f", p.productPrice));
            row.addView(price);
        }

        if (p.productLink != null && !p.productLink.isEmpty()) {
            TextView link = makeSmallText(p.productLink);
            link.setTextColor(0xFF2962FF); // default link-ish color
            link.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(p.productLink)));
                } catch (Exception ignored) {}
            });
            row.addView(link);
        }
        return row;
    }

    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }
}
