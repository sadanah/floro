package com.sadanah.floro;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TopicFragment extends Fragment {

    private RecyclerView recyclerView;
    private TopicAdapter adapter;
    private List<ChatTopic> topicList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_topic, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTopics);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set up adapter with click listener
        adapter = new TopicAdapter(topicList, topic -> {
            // --- ERROR CAUSE ---
            // Previously, topic.getTopicId() could be null if Firestore document
            // did not include a "topicId" field. Passing null to ChatFragment caused the crash.
            String topicId = topic.getTopicId();
            if (topicId == null || topicId.isEmpty()) {
                Log.e("TopicFragment", "Topic ID is null, cannot open chat");
                return; // Prevents crash
            }

            // Open ChatFragment with valid topicId
            ChatFragment chatFragment = ChatFragment.newInstance(topicId, topic.getTopicName());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        loadTopics(); // Load topics from Firestore

        return view;
    }

    private void loadTopics() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("chatTopics")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    topicList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatTopic topic = doc.toObject(ChatTopic.class);

                        // --- FIX: ensure topicId is never null ---
                        // Firestore may not store "topicId" field; use document ID as fallback
                        if (topic.getTopicId() == null || topic.getTopicId().isEmpty()) {
                            topic.setTopicId(doc.getId());
                        }

                        topicList.add(topic);
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}
