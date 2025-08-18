package com.sadanah.floro.forum;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sadanah.floro.R;
import com.sadanah.floro.models.ChatTopic;

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
            // Open ChatFragment and pass topicId and topicName
            ChatFragment chatFragment = ChatFragment.newInstance(topic.getTopicId(), topic.getTopicName());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack("chat")
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        loadTopics();

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
                        topicList.add(topic);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

}
