package com.sadanah.floro;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdminDashFragment extends Fragment {

    private EditText editTextTopicName;
    private Button btnAddTopic, btnUserStats;
    private RecyclerView recyclerViewMessages, recyclerViewUsers;

    private FirebaseFirestore db;

    private MessagesAdapter messagesAdapter;
    private UsersAdapter usersAdapter;

    private ArrayList<ChatMessage> messageList = new ArrayList<>();
    private ArrayList<UserProfile> userList = new ArrayList<>();

    public AdminDashFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dash, container, false);

        db = FirebaseFirestore.getInstance();

        editTextTopicName = view.findViewById(R.id.editTextTopicName);
        btnAddTopic = view.findViewById(R.id.btnAddTopic);
        btnUserStats = view.findViewById(R.id.btnUserStats);
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);

        // Setup RecyclerViews
        messagesAdapter = new MessagesAdapter(messageList);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMessages.setAdapter(messagesAdapter);

        usersAdapter = new UsersAdapter(userList);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(usersAdapter);

        // Listeners
        btnAddTopic.setOnClickListener(v -> addTopic());
        btnUserStats.setOnClickListener(v -> showUserStats());

        loadMessages();
        loadUsers();

        return view;
    }

    /*** Add new chat topic ***/
    private void addTopic() {
        String topicName = editTextTopicName.getText().toString().trim();
        if (topicName.isEmpty()) {
            Toast.makeText(getContext(), "Enter a topic name", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> topic = new HashMap<>();
        topic.put("topicName", topicName);

        db.collection("chatTopics")
                .add(topic)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Topic added", Toast.LENGTH_SHORT).show();
                    editTextTopicName.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /*** Load messages from Firestore ***/
    private void loadMessages() {
        db.collection("chatMessages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    messageList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatMessage msg = new ChatMessage(
                                doc.getId(),
                                doc.getString("messageText"),
                                doc.getString("messageImage"),
                                doc.getString("userId"),
                                doc.getDocumentReference("topicId") // ✅ Use getDocumentReference
                        );
                        messageList.add(msg);
                    }
                    messagesAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load messages: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    /*** Load users from Firestore ***/
    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        UserProfile user = new UserProfile(
                                doc.getId(),
                                doc.getString("firstName"),
                                doc.getString("lastName"),
                                doc.getString("email"),
                                doc.getString("city"),
                                doc.getString("phoneNumber")
                        );
                        userList.add(user);
                    }
                    usersAdapter.notifyDataSetChanged();
                });
    }

    /*** Show simple stats of users ***/
    private void showUserStats() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalUsers = queryDocumentSnapshots.size();
                    Map<String, Integer> cityCount = new HashMap<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String city = doc.getString("city");
                        if (city != null) {
                            cityCount.put(city, cityCount.getOrDefault(city, 0) + 1);
                        }
                    }
                    StringBuilder stats = new StringBuilder("Total users: " + totalUsers + "\n\n");
                    for (Map.Entry<String, Integer> entry : cityCount.entrySet()) {
                        stats.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    }

                    new AlertDialog.Builder(getContext())
                            .setTitle("User Statistics")
                            .setMessage(stats.toString())
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    /*** ChatMessage Model ***/
    static class ChatMessage {
        String id, messageText, messageImage, userId;
        DocumentReference topicRef;

        public ChatMessage(String id, String messageText, String messageImage, String userId, DocumentReference topicRef) {
            this.id = id;
            this.messageText = messageText;
            this.messageImage = messageImage;
            this.userId = userId;
            this.topicRef = topicRef;
        }
    }

    /*** UserProfile Model ***/
    static class UserProfile {
        String id, firstName, lastName, email, city, phoneNumber;

        public UserProfile(String id, String firstName, String lastName, String email, String city, String phoneNumber) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.city = city;
            this.phoneNumber = phoneNumber;
        }
    }

    /*** RecyclerView Adapter for Messages ***/
    class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {
        ArrayList<ChatMessage> messages;

        MessagesAdapter(ArrayList<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            holder.bind(msg);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            Button btnDeleteMessage;

            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                btnDeleteMessage = itemView.findViewById(R.id.btnDeleteMessage);
            }

            void bind(ChatMessage msg) {
                btnDeleteMessage.setOnClickListener(v -> {
                    db.collection("chatMessages").document(msg.id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Message deleted", Toast.LENGTH_SHORT).show();
                                messages.remove(getAdapterPosition());
                                notifyItemRemoved(getAdapterPosition());
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                });
            }
        }
    }

    /*** RecyclerView Adapter for Users ***/
    class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
        ArrayList<UserProfile> users;

        UsersAdapter(ArrayList<UserProfile> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserProfile user = users.get(position);
            holder.bind(user);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            Button btnDeleteUser;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
            }

            void bind(UserProfile user) {
                btnDeleteUser.setOnClickListener(v -> {
                    db.collection("users").document(user.id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "User deleted", Toast.LENGTH_SHORT).show();
                                users.remove(getAdapterPosition());
                                notifyItemRemoved(getAdapterPosition());
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                });
            }
        }
    }
}
