package central.mail.cache.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ThreadMessageCache<T> {
    private String messageId;
    private List<T> messages = new ArrayList<>();
    private T gid;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public List<T> getMessages() {
        return messages;
    }

    public void setMessages(List<T> messages) {
        this.messages = messages;
    }

    public T getGid() {
        return gid;
    }

    public void setGid(T gid) {
        this.gid = gid;
    }
}
