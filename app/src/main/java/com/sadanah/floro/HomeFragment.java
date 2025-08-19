package com.sadanah.floro;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private Uri tempCameraUri;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Take Scan Button
        ImageButton btnTakeScan = view.findViewById(R.id.btnTakeScan);
        btnTakeScan.setOnClickListener(v -> checkCameraPermissionsAndLaunch());

        // 2. Articles Card
        MaterialCardView cardArticles = view.findViewById(R.id.cardArticles);
        cardArticles.setOnClickListener(v -> navigateToArticles());

        // 3. Navigation Grid Cards
        setupGridCards(view);

        // --- Activity Result Launchers ---
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> checkCameraPermissionsAndLaunch()
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean success) {
                        if (success != null && success && tempCameraUri != null) {
                            // Use MainActivity's handleImageUri or implement similar logic here
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).handleImageUri(tempCameraUri);
                            }
                        } else {
                            Toast.makeText(getContext(), "Camera canceled", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        return view;
    }

    private void checkCameraPermissionsAndLaunch() {
        if (hasCameraPermissions()) {
            startCamera();
        } else {
            List<String> perms = new ArrayList<>();
            perms.add(Manifest.permission.CAMERA);
            if (Build.VERSION.SDK_INT >= 33)
                perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            else
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);

            permissionLauncher.launch(perms.toArray(new String[0]));
        }
    }

    private boolean hasCameraPermissions() {
        boolean cameraOk = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PermissionChecker.PERMISSION_GRANTED;
        boolean readOk;
        if (Build.VERSION.SDK_INT >= 33) {
            readOk = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    == PermissionChecker.PERMISSION_GRANTED;
        } else {
            readOk = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PermissionChecker.PERMISSION_GRANTED;
        }
        return cameraOk && readOk;
    }

    private void startCamera() {
        try {
            File imageFile = File.createTempFile("camera_", ".jpg", requireActivity().getCacheDir());
            tempCameraUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    imageFile
            );
            takePictureLauncher.launch(tempCameraUri);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Could not start camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    // Navigate to ArticlesFragment
    private void navigateToArticles() {
        ArticlesFragment fragment = new ArticlesFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Navigate to Forum
    private void navigateToForums() {
        TopicFragment fragment = new TopicFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Navigate to Catalogue
    private void navigateToCatalogue() {
        SearchFragment fragment = new SearchFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Navigate to Info
    private void navigateToInfo() {
        InfoFragment fragment = new InfoFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Navigate to Map
    private void navigateToMap() {
        MapFragment fragment = new MapFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Setup the grid cards and assign click listeners
    private void setupGridCards(View view) {
        int[] cardIds = new int[]{
                R.id.card_scan,
                R.id.card_catalogue,
                R.id.card_forums,
                R.id.card_info,
                R.id.card_map,
                R.id.card_articles
        };

        for (int id : cardIds) {
            MaterialCardView card = view.findViewById(id);
            if (card != null) {
                card.setOnClickListener(v -> handleGridCardClick(id));
            }
        }
    }

    // Handle clicks on each card
    private void handleGridCardClick(int cardId) {
        if (cardId == R.id.card_scan) {
            checkCameraPermissionsAndLaunch();
        } else if (cardId == R.id.card_catalogue) {
            navigateToCatalogue();
        } else if (cardId == R.id.card_forums) {
            navigateToForums();
        } else if (cardId == R.id.card_info) {
            navigateToInfo();
        } else if (cardId == R.id.card_map) {
            navigateToMap();
        } else if (cardId == R.id.card_articles) {
            navigateToArticles();
        }
    }

}
