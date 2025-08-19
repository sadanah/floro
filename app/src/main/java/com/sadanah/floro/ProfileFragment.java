package com.sadanah.floro;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextInputEditText editTextFirstName, editTextLastName, editTextCity, editTextPhone, editTextEmail;
    private Button buttonSave;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userDocId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Bind UI
        editTextFirstName = view.findViewById(R.id.editTextFirstName);
        editTextLastName  = view.findViewById(R.id.editTextLastName);
        editTextCity      = view.findViewById(R.id.editTextCity);
        editTextPhone     = view.findViewById(R.id.editTextPhone);
        editTextEmail     = view.findViewById(R.id.editTextEmail);
        buttonSave        = view.findViewById(R.id.buttonSave);
        progressBar       = view.findViewById(R.id.progressBar);

        // Init Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Find user document by authUid
        db.collection("users")
                .whereEqualTo("authUid", user.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        userDocId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        // Populate fields
                        Map<String, Object> data = queryDocumentSnapshots.getDocuments().get(0).getData();
                        editTextFirstName.setText((String) data.get("firstName"));
                        editTextLastName.setText((String) data.get("lastName"));
                        editTextCity.setText((String) data.get("city"));
                        editTextPhone.setText((String) data.get("phoneNumber"));
                        editTextEmail.setText((String) data.get("email"));
                    }
                });

        // Save changes click
        buttonSave.setOnClickListener(v -> saveChanges());

        return view;
    }

    private void saveChanges() {
        if (TextUtils.isEmpty(userDocId)) return;

        final String firstName = editTextFirstName.getText().toString().trim();
        final String lastName  = editTextLastName.getText().toString().trim();
        final String city      = editTextCity.getText().toString().trim();
        final String phone     = editTextPhone.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || city.isEmpty() || phone.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("city", city);
        updates.put("phoneNumber", phone);

        db.collection("users").document(userDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);
                    Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
