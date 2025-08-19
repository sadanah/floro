package com.sadanah.floro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ArticlesFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<Article> articles = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_articles, container, false);
        recyclerView = view.findViewById(R.id.articlesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadArticlesFromAssets("internal_articles.json"); // or external_articles.json
        return view;
    }

    private void loadArticlesFromAssets(String fileName) {
        try {
            InputStream is = requireContext().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(jsonStr);
            JSONArray arr = obj.getJSONArray("articles");

            for (int i = 0; i < arr.length(); i++) {
                JSONObject a = arr.getJSONObject(i);
                Article article = new Article();
                article.id = a.getInt("id");
                article.title = a.getString("title");
                article.subtitle = a.optString("subtitle", "");
                article.content = new ArrayList<>();
                JSONArray contentArray = a.getJSONArray("content");
                for (int j = 0; j < contentArray.length(); j++)
                    article.content.add(contentArray.getString(j));
                article.image = a.optString("image", null);
                article.tags = new ArrayList<>();
                JSONArray tagsArray = a.optJSONArray("tags");
                if (tagsArray != null)
                    for (int j = 0; j < tagsArray.length(); j++)
                        article.tags.add(tagsArray.getString(j));
                article.date = a.optString("date", "");
                article.link = a.optString("link", null);
                if (!a.isNull("price_lkr")) article.price_lkr = a.getDouble("price_lkr");
                articles.add(article);
            }

            recyclerView.setAdapter(new ArticlesAdapter(articles));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
