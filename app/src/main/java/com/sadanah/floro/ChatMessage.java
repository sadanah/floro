package com.sadanah.floro;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.Timestamp;

public class ChatMessage {

    private String userId;                    // Firestore string field
    private DocumentReference topicId;        // Firestore reference field
    private String messageText;
    private String messageImage;              // NEW: match Firestore field
    private Timestamp timestamp;

    // No-arg constructor required by Firestore
    public ChatMessage() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public DocumentReference getTopicId() {
        return topicId;
    }

    public void setTopicId(DocumentReference topicId) {
        this.topicId = topicId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageImage() {
        return messageImage;
    }

    public void setMessageImage(String messageImage) {
        this.messageImage = messageImage;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
