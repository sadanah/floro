package com.sadanah.floro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MapFragment extends Fragment {

    private WebView webViewMap;
    private static final String MAP_URL = "https://www.google.com/maps/d/u/0/edit?mid=1jFXUirQbcofJxWeCitbxHnqFIXY9EKg&usp=sharing";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        webViewMap = view.findViewById(R.id.webViewMap);
        WebSettings webSettings = webViewMap.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Open links inside the WebView
        webViewMap.setWebViewClient(new WebViewClient());
        webViewMap.loadUrl(MAP_URL);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webViewMap != null) {
            webViewMap.destroy();
        }
    }
}
