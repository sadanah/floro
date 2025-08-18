package com.sadanah.floro.models;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String messageId;
    private String messageText;
    private String messageImage; // optional
    private String topicId;
    private String userId;
    private Timestamp timestamp;

    // Empty constructor for Firestore
    public ChatMessage() {}

    public ChatMessage(String messageId, String messageText, String messageImage,
                       String topicId, String userId, Timestamp timestamp) {
        this.messageId = messageId;
        this.messageText = messageText;
        this.messageImage = messageImage;
        this.topicId = topicId;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public String getMessageText() { return messageText; }
    public String getMessageImage() { return messageImage; }
    public String getTopicId() { return topicId; }
    public String getUserId() { return userId; }
    public Timestamp getTimestamp() { return timestamp; }
}
