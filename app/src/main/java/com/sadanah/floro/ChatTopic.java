package com.sadanah.floro;

public class ChatTopic {
    private String topicId;
    private String topicName;

    public ChatTopic() {} // Firestore needs no-arg constructor

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {  // <-- add this
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }
}

