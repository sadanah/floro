package com.sadanah.floro;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.graphics.BitmapFactory;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseUser user;
    private Button btnLogout;
    private TextView userDetails;

    // Bottom nav + FAB
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fab;

    // TFLite
    private Interpreter tflite;
    private List<String> labels;
    private GpuDelegate gpuDelegate;

    private static final int MODEL_IMAGE_WIDTH = 224;
    private static final int MODEL_IMAGE_HEIGHT = 224;

    // Activity Result
    private Uri tempCameraUri;
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> showAddPhotoSheet());

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean success) {
                    if (success != null && success && tempCameraUri != null) {
                        handleImageUri(tempCameraUri);
                    } else {
                        Toast.makeText(MainActivity.this, "Camera canceled", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> pickGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleImageUri(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Firebase views
        auth = FirebaseAuth.getInstance();
        btnLogout = findViewById(R.id.btn_logout);
        userDetails = findViewById(R.id.user_details);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);

        bottomNavigationView.setBackground(null);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);

        user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
            return;
        } else {
            userDetails.setText(user.getEmail());
        }

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });

        // Load default fragment
//        if (savedInstanceState == null) {
//            loadFragment(new HomeFragment());
//        }

        // Bottom navigation switching
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment f = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) f = new HomeFragment();
            else if (id == R.id.nav_search) f = new SearchFragment();
            else if (id == R.id.nav_articles) f = new ArticlesFragment();
            else if (id == R.id.nav_forum) f = new TopicFragment();
            if (f != null) loadFragment(f);
            return true;
        });

        // FAB click -> open bottom sheet for upload
        fab = findViewById(R.id.fab);
        if (fab == null) {
            Log.e("MainActivity", "FAB not found!");
        } else {
            Log.d("MainActivity", "FAB found");
            fab.setOnClickListener(v -> {
                //Toast.makeText(this, "FAB clicked!", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "FAB clicked");
                showAddPhotoSheet();
            });
        }

        // Load TFLite model
        try {
            initTflite();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }

    //---------- Bottom Sheet ----------
    private void showAddPhotoSheet() {
        if (!hasMediaPermissions()) {
            requestMediaPermissions();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.bottom_sheet_add_photo);

        Button btnTake = dialog.findViewById(R.id.btn_take_photo);
        Button btnPick = dialog.findViewById(R.id.btn_pick_gallery);

        if (btnTake != null) btnTake.setOnClickListener(v -> {
            dialog.dismiss();
            startCamera();
        });

        if (btnPick != null) btnPick.setOnClickListener(v -> {
            dialog.dismiss();
            pickFromGallery();
        });


        dialog.show();
    }


    private boolean hasMediaPermissions() {
        boolean cameraOk = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean readOk;
        if (Build.VERSION.SDK_INT >= 33)
            readOk = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        else
            readOk = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return cameraOk && readOk;
    }

    private void requestMediaPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.READ_MEDIA_IMAGES);
        else perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionLauncher.launch(perms.toArray(new String[0]));
    }

    // ---------- Camera ----------
    private void startCamera() {
        try {
            File imageFile = File.createTempFile("camera_", ".jpg", getCacheDir());
            tempCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            takePictureLauncher.launch(tempCameraUri);
        } catch (IOException e) {
            Toast.makeText(this, "Could not start camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Gallery ----------
    private void pickFromGallery() {
        pickGalleryLauncher.launch("image/*");
    }

    // ---------- Handle Image ----------
    public void handleImageUri(@NonNull Uri uri) {
        try {
            Bitmap bitmap = decodeBitmap(uri);
            if (bitmap == null) {
                Toast.makeText(this, "Unable to decode image", Toast.LENGTH_SHORT).show();
                return;
            }

            InferenceResult result = runInference(bitmap);
            String msg = "Prediction: " + result.label + " (" + String.format("%.1f", result.confidence * 100) + "%)";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            // Navigate to ResultFragment
            ResultFragment frag = ResultFragment.newInstance(result.label, result.confidence);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .addToBackStack("result")
                    .commit();


        } catch (IOException e) {
            Toast.makeText(this, "Error decoding image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    private Bitmap decodeBitmap(Uri uri) throws IOException {
        if (uri == null) return null;
        ContentResolver cr = getContentResolver();
        Bitmap bitmap;

        // For Android 9 (API 28) and up, use ImageDecoder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source src = ImageDecoder.createSource(cr, uri);
            bitmap = ImageDecoder.decodeBitmap(src, (decoder, info, source) -> {
                // Resize while decoding to MODEL_IMAGE_WIDTH x MODEL_IMAGE_HEIGHT to save memory
                decoder.setTargetSize(MODEL_IMAGE_WIDTH, MODEL_IMAGE_HEIGHT);

                // Use software allocator to avoid issues with hardware bitmaps which may be immutable/restricted
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });

            if (bitmap == null) throw new IOException("Failed to decode image");

            // Ensure bitmap is ARGB_8888 and mutable so getPixels / pixel access works
            if (bitmap.getConfig() != Bitmap.Config.ARGB_8888 || !bitmap.isMutable()) {
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                if (bitmap == null) throw new IOException("Failed to make mutable bitmap copy");
            }

        } else {
            // Legacy path for older devices
            InputStream is = null;
            try {
                is = cr.openInputStream(uri);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

                // Optionally, we can sample down large images: (simple approach)
                // First decode bounds
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, opts);
                if (is != null) {
                    is.close();
                    is = cr.openInputStream(uri);
                }

                // compute inSampleSize
                opts.inSampleSize = calculateInSampleSize(opts, MODEL_IMAGE_WIDTH, MODEL_IMAGE_HEIGHT);
                opts.inJustDecodeBounds = false;

                bitmap = BitmapFactory.decodeStream(is, null, opts);
                if (bitmap == null) throw new IOException("Failed to decode bitmap (legacy)");

                if (!bitmap.isMutable() || bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                    Bitmap tmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    if (tmp == null) throw new IOException("Failed to make mutable copy");
                    bitmap.recycle();
                    bitmap = tmp;
                }
            } finally {
                if (is != null) try {
                    is.close();
                } catch (IOException ignored) {
                }
            }

            // final safety resize to model size
            if (bitmap.getWidth() != MODEL_IMAGE_WIDTH || bitmap.getHeight() != MODEL_IMAGE_HEIGHT) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, MODEL_IMAGE_WIDTH, MODEL_IMAGE_HEIGHT, true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                    bitmap = scaled;
                }
            }
        }

        return bitmap;
    }


    //Helper to compute a power-of-two inSampleSize for BitmapFactory

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    // ---------- TFLite ----------
    private void initTflite() throws IOException {
        MappedByteBuffer model = FileUtil.loadMappedFile(this, "orchid_custom_model.tflite");
        labels = FileUtil.loadLabels(this, "labels.txt");

        Interpreter.Options options = new Interpreter.Options();
        try {
            try (CompatibilityList compatList = new CompatibilityList()) {
                if (compatList.isDelegateSupportedOnThisDevice()) {
                    gpuDelegate = new GpuDelegate();
                    options.addDelegate(gpuDelegate);
                }
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignore) {
        }

        tflite = new Interpreter(model, options);
    }

    private static class InferenceResult {
        final String label;
        final float confidence;

        InferenceResult(String l, float c) {
            label = l;
            confidence = c;
        }
    }

    private InferenceResult runInference(Bitmap bitmap) {
        if (tflite == null) throw new IllegalStateException("Model not loaded");

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, MODEL_IMAGE_WIDTH, MODEL_IMAGE_HEIGHT, true);

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * MODEL_IMAGE_WIDTH * MODEL_IMAGE_HEIGHT * 3);
        inputBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[MODEL_IMAGE_WIDTH * MODEL_IMAGE_HEIGHT];
        resized.getPixels(intValues, 0, MODEL_IMAGE_WIDTH, 0, 0, MODEL_IMAGE_WIDTH, MODEL_IMAGE_HEIGHT);

        for (int pixel : intValues) {
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;
            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }

        float[][] output = new float[1][labels.size()];
        tflite.run(inputBuffer, output);

        int bestIdx = 0;
        float best = output[0][0];
        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > best) {
                best = output[0][i];
                bestIdx = i;
            }
        }

        String bestLabel = labels.get(bestIdx);
        return new InferenceResult(bestLabel, best);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }

        // Handle bottom navigation item clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (id == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

    }

}
