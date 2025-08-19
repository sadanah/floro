package com.sadanah.floro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

public class ArticleDetailsFragment extends Fragment {

    private static final String ARG_ARTICLE = "article";
    private Article article;

    public static ArticleDetailsFragment newInstance(Article article) {
        ArticleDetailsFragment fragment = new ArticleDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ARTICLE, article);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_article_details, container, false);

        // Retrieve the Article object
        if (getArguments() != null) {
            article = (Article) getArguments().getSerializable(ARG_ARTICLE);
        }

        // UI components
        TextView title = view.findViewById(R.id.detailTitle);
        TextView content = view.findViewById(R.id.detailContent);
        ImageView imageView = view.findViewById(R.id.detailImage);
        TextView priceView = view.findViewById(R.id.detailPrice);
        Button linkButton = view.findViewById(R.id.detailLinkButton);

        // Set title and content
        title.setText(article.title);
        content.setText(TextUtils.join("\n\n", article.content));

        // Load image if available
        if (article.image != null && !article.image.isEmpty()) {
            imageView.setVisibility(View.VISIBLE);
            // Ensure Glide knows it's in assets
            String assetPath = "file:///android_asset/images/" + article.image;
            Glide.with(requireContext())
                    .load(assetPath)
                    .into(imageView);
        } else {
            imageView.setVisibility(View.GONE);
        }


        if (article.price_lkr != null && article.price_lkr > 0) {
            priceView.setText("Price: LKR " + String.format("%.2f", article.price_lkr));
            priceView.setVisibility(View.VISIBLE);
        } else {
            priceView.setVisibility(View.GONE);
        }


        // Display external link button if available
        if (article.link != null && !article.link.isEmpty()) {
            linkButton.setVisibility(View.VISIBLE);
            linkButton.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.link));
                startActivity(browserIntent);
            });
        } else {
            linkButton.setVisibility(View.GONE);
        }

        return view;
    }
}
