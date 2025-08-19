package com.sadanah.floro;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class RegisterActivity extends AppCompatActivity {

    // UI components (match your XML)
    private EditText editTextFirstName, editTextLastName, editTextCity, editTextPhone, editTextEmail, editTextPassword;
    private Button buttonRegister;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Init Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName  = findViewById(R.id.editTextLastName);
        editTextCity      = findViewById(R.id.editTextCity);
        editTextPhone     = findViewById(R.id.editTextPhone);
        editTextEmail     = findViewById(R.id.editTextEmail);
        editTextPassword  = findViewById(R.id.editTextPassword);
        buttonRegister    = findViewById(R.id.buttonRegister);
        progressBar       = findViewById(R.id.progressBar);

        buttonRegister.setOnClickListener(v -> attemptRegister());

        TextView toRegister = findViewById(R.id.toLogin);
        toRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to RegisterActivity
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void attemptRegister() {
        final String firstName = safeText(editTextFirstName);
        final String lastName  = safeText(editTextLastName);
        final String city      = safeText(editTextCity);
        final String phone     = safeText(editTextPhone);
        final String email     = safeText(editTextEmail);
        final String password  = safeText(editTextPassword);

        // Validate
        if (TextUtils.isEmpty(firstName)) { editTextFirstName.setError("Required"); return; }
        if (TextUtils.isEmpty(lastName))  { editTextLastName.setError("Required");  return; }
        if (TextUtils.isEmpty(city))      { editTextCity.setError("Required");      return; }
        if (TextUtils.isEmpty(phone))     { editTextPhone.setError("Required");     return; }
        if (TextUtils.isEmpty(email))     { editTextEmail.setError("Required");     return; }
        if (TextUtils.isEmpty(password))  { editTextPassword.setError("Required");  return; }
        if (password.length() < 6)        { editTextPassword.setError("Min 6 chars"); return; }

        setLoading(true);

        // Create Firebase Auth user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, (Task<AuthResult> task) -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        Toast.makeText(this,
                                "Registration failed: " + (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Optional: Send email verification
                        firebaseUser.sendEmailVerification();

                        final String authUid = firebaseUser.getUid();
                        final String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

                        // Reserve custom UID
                        reserveNextUserId().addOnSuccessListener(customUserId -> {
                            Map<String, Object> doc = new HashMap<>();
                            doc.put("firstName", firstName);
                            doc.put("lastName", lastName);
                            doc.put("city", city);
                            doc.put("phoneNumber", phone);
                            doc.put("email", email);
                            doc.put("passwordHash", passwordHash);
                            doc.put("authUid", authUid);

                            db.collection("users").document(customUserId)
                                    .set(doc)
                                    .addOnSuccessListener(aVoid -> {
                                        setLoading(false);
                                        Toast.makeText(RegisterActivity.this,
                                                "Account created. Please verify your email.", Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        Toast.makeText(RegisterActivity.this,
                                                "Failed to save profile: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });

                        }).addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(RegisterActivity.this,
                                    "Failed to reserve user ID: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(!loading);
    }

    /**
     * Atomically reserves the next user document ID (u_0001, u_0002, ...) using Firestore transaction.
     */
    private Task<String> reserveNextUserId() {
        final DocumentReference countersRef = db.collection("meta").document("counters");
        return db.runTransaction((Transaction.Function<String>) transaction -> {
            DocumentSnapshot snap = transaction.get(countersRef);
            long next;
            if (snap.exists() && snap.getLong("nextUserNumber") != null) {
                next = snap.getLong("nextUserNumber");
            } else {
                next = 1L;
            }
            transaction.set(countersRef, Collections.singletonMap("nextUserNumber", next + 1L), SetOptions.merge());
            return String.format(Locale.US, "u_%04d", next);
        });
    }
}
