package com.sadanah.floro.forum;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sadanah.floro.R;
import com.sadanah.floro.models.ChatTopic;

import java.util.List;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {

    private List<ChatTopic> topicList;
    private final OnTopicClickListener listener;

    public interface OnTopicClickListener {
        void onTopicClick(ChatTopic topic);
    }

    public TopicAdapter(List<ChatTopic> topicList, OnTopicClickListener listener) {
        this.topicList = topicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_topic, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        ChatTopic topic = topicList.get(position);
        holder.topicName.setText(topic.getTopicName());

        holder.itemView.setOnClickListener(v -> listener.onTopicClick(topic));
    }

    @Override
    public int getItemCount() {
        return topicList.size();
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        TextView topicName;

        TopicViewHolder(View itemView) {
            super(itemView);
            topicName = itemView.findViewById(R.id.textTopicName);
        }
    }
}
