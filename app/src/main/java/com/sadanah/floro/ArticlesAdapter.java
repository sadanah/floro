package com.sadanah.floro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // for image loading

import java.util.List;

public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ArticleViewHolder> {

    private final List<Article> articles;

    public ArticlesAdapter(List<Article> articles) {
        this.articles = articles;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        Article article = articles.get(position);
        holder.title.setText(article.title);
        holder.subtitle.setText(article.subtitle);

        if (article.image != null && !article.image.isEmpty()) {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load("file:///android_asset/images/" + article.image)
                    .into(holder.image);
        } else {
            holder.image.setVisibility(View.GONE);
        }

        // Optional: handle clicks
        holder.itemView.setOnClickListener(v -> {
            // e.g., open a detail fragment or activity
        });
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, subtitle;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.articleImage);
            title = itemView.findViewById(R.id.articleTitle);
            subtitle = itemView.findViewById(R.id.articleSubtitle);
        }
    }
}

