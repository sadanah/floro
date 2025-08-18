package com.sadanah.floro;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.Timestamp;

public class ChatFragment extends Fragment {

    private static final String ARG_TOPIC_ID = "topicId";
    private static final String ARG_TOPIC_NAME = "topicName";

    private String topicId;
    private String topicName;
    private com.google.firebase.firestore.DocumentReference topicRef;

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    private EditText editMessage;
    private ImageButton btnSend;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    /** Use this method to create a new ChatFragment for a specific topic */
    public static ChatFragment newInstance(String topicId, String topicName) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TOPIC_ID, topicId);
        args.putString(ARG_TOPIC_NAME, topicName);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            topicId = getArguments().getString(ARG_TOPIC_ID);
            topicName = getArguments().getString(ARG_TOPIC_NAME);
        }

        if (topicId != null) {
            topicRef = db.collection("chatTopics").document(topicId);
        } else {
            throw new IllegalArgumentException("ChatFragment requires a non-null topicId!");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize Firebase only if not already initialized
        if (FirebaseApp.getApps(requireContext()).isEmpty()) {
            FirebaseApp.initializeApp(requireContext());
        }

        recyclerView = view.findViewById(R.id.recyclerViewChat);
        editMessage = view.findViewById(R.id.editMessage);
        btnSend = view.findViewById(R.id.btnSend);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);

        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());

        return view;
    }



    private void loadMessages() {
        if (topicRef == null) return;

        db.collection("chatMessages")
                .whereEqualTo("topicId", topicRef)
                .orderBy("timestamp")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) return;

                    messageList.clear();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            ChatMessage msg = doc.toObject(ChatMessage.class);
                            messageList.add(msg);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }


    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text) || topicRef == null || auth.getCurrentUser() == null) return;

        ChatMessage message = new ChatMessage();
        message.setUserId(auth.getCurrentUser().getUid());
        message.setTopicId(topicRef);
        message.setMessageText(text);
        message.setTimestamp(new Timestamp(System.currentTimeMillis() / 1000, 0));

        db.collection("chatMessages")
                .add(message)
                .addOnSuccessListener(docRef -> editMessage.setText(""));
    }


}
