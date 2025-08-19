package com.sadanah.floro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
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
        TextView tvDesc  = view.findViewById(R.id.tv_description);
        LinearLayout treatmentContainer = view.findViewById(R.id.treatment_container);
        LinearLayout productsContainer = view.findViewById(R.id.products_container);

        tvTitle.setText(diseaseName == null ? "Unknown" : diseaseName);
        tvConf.setText(String.format(Locale.US, "Confidence: %.1f%%", confidence * 100f));

        DBHelper db = new DBHelper(requireContext());
        DBHelper.DiseaseDetails details = null;
        if (diseaseName != null) {
            details = db.getDiseaseDetailsByName(diseaseName);
            if (details == null) {
                details = db.getDiseaseDetailsForModelLabel(diseaseName);
            }
        }

        if (details == null) {
            tvPlant.setText("Plant: —");
            tvType.setText("Type: —");
            tvDesc.setText("No disease information found.");
            treatmentContainer.removeAllViews();
            productsContainer.removeAllViews();
            return;
        }

        // Plant info
        tvPlant.setText("Plant: " + (details.plant != null ? details.plant.plantName : "—"));
        tvType.setText("Type: " + (details.disease.diseaseType != null ? details.disease.diseaseType : "—"));
        tvDesc.setText(details.disease.diseaseDescription != null ? details.disease.diseaseDescription : "—");

        // Load treatment from JSON file
        treatmentContainer.removeAllViews();
        if (details.disease.diseaseTreatment != null) {
            try {
                JSONObject treatmentJson = loadJsonFromAssets(details.disease.diseaseTreatment);
                if (treatmentJson != null) {
                    addTreatmentSection(treatmentContainer, "Cultural Care", treatmentJson.getJSONArray("culturalCare"));
                    addTreatmentSection(treatmentContainer, "Chemical Control", treatmentJson.getJSONArray("chemicalControl"));
                    addTreatmentSection(treatmentContainer, "Eco-Friendly Methods", treatmentJson.getJSONArray("ecoFriendly"));
                }
            } catch (Exception e) {
                TextView error = makeSmallText("Failed to load treatment info.");
                treatmentContainer.addView(error);
            }
        }

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

    private void addTreatmentSection(LinearLayout container, String sectionTitle, JSONArray items) {
        TextView header = new TextView(requireContext());
        header.setText(sectionTitle);
        header.setTextSize(16f);
        header.setPadding(0, dp(8), 0, dp(4));
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(header);

        for (int i = 0; i < items.length(); i++) {
            try {
                JSONObject obj = items.getJSONObject(i);
                String title = obj.getString("title");
                JSONArray steps = obj.getJSONArray("steps");

                // Section subtitle
                TextView subTitle = new TextView(requireContext());
                subTitle.setText(title);
                subTitle.setTextSize(15f);
                subTitle.setPadding(dp(8), dp(4), 0, dp(2));
                subTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                container.addView(subTitle);

                // Steps as bullet points
                for (int j = 0; j < steps.length(); j++) {
                    String step = steps.getString(j);
                    TextView stepView = new TextView(requireContext());
                    stepView.setText("• " + step);
                    stepView.setTextSize(14f);
                    stepView.setPadding(dp(16), dp(2), 0, dp(2));
                    container.addView(stepView);
                }

            } catch (Exception ignored) {}
        }
    }

    private JSONObject loadJsonFromAssets(String fileName) {
        try {
            Log.d("DEBUG_JSON", "Attempting to open: " + fileName); // <- add this line
            InputStream is = requireContext().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            Log.d("DEBUG_JSON", fileName + " content:\n" + json); // optional: see file content
            return new JSONObject(json).getJSONObject("treatment");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private TextView makeSmallText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(14f);
        return tv;
    }

    private View makeProductRow(DBHelper.Product p) {
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
            link.setTextColor(0xFF2962FF);
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
