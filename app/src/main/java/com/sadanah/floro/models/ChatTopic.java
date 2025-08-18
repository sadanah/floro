package com.sadanah.floro.models;

public class ChatTopic {
    private String topicId;
    private String topicName;

    // Empty constructor needed for Firestore
    public ChatTopic() {}

    public ChatTopic(String topicId, String topicName) {
        this.topicId = topicId;
        this.topicName = topicName;
    }

    public String getTopicId() { return topicId; }
    public String getTopicName() { return topicName; }
}
