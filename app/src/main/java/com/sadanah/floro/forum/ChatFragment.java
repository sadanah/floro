package com.sadanah.floro.forum;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sadanah.floro.R;
import com.sadanah.floro.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private static final String ARG_TOPIC_ID = "topicId";
    private static final String ARG_TOPIC_NAME = "topicName";

    private String topicId;
    private String topicName;

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    private EditText editMessage;
    private Button btnSend;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public static ChatFragment newInstance(String topicId, String topicName) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TOPIC_ID, topicId);
        args.putString(ARG_TOPIC_NAME, topicName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        if (getArguments() != null) {
            topicId = getArguments().getString(ARG_TOPIC_ID);
            topicName = getArguments().getString(ARG_TOPIC_NAME);
        }

        recyclerView = view.findViewById(R.id.recyclerViewChat);
        editMessage = view.findViewById(R.id.editMessage);
        btnSend = view.findViewById(R.id.btnSend);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());

        return view;
    }

    private void loadMessages() {
        CollectionReference messagesRef = db.collection("chatMessages");
        messagesRef.whereEqualTo("topicId", topicId)
                .orderBy("timestamp")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) return;

                    messageList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatMessage message = doc.toObject(ChatMessage.class);
                        messageList.add(message);
                    }
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
                });
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String userId = auth.getCurrentUser().getUid();

        ChatMessage message = new ChatMessage();
        message.setMessageText(text);
        message.setTopicId(topicId);
        message.setUserId(userId);
        message.setTimestamp(System.currentTimeMillis());

        db.collection("chatMessages")
                .add(message)
                .addOnSuccessListener(documentReference -> editMessage.setText(""));
    }
}
