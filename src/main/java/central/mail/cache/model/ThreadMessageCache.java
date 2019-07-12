package central.mail.cache.model;

import java.util.*;

public class ThreadMessageCache<T> {
    private String messageId;
    private Set<T> messages = new LinkedHashSet<>();
    private T gid;
    private T mainMessageGid = null;
    private Long mainMessageDate = 0l;
    private Long flags = 0l;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Set<T> getMessages() {
        return messages;
    }

    public void setMessages(Set<T> messages) {
        this.messages = messages;
    }

    public T getGid() {
        return gid;
    }

    public void setGid(T gid) {
        this.gid = gid;
    }

    public T getMainMessageGid() {
        return mainMessageGid;
    }

    public void setMainMessageGid(T mainMessageGid) {
        this.mainMessageGid = mainMessageGid;
    }

    public Long getMainMessageDate() {
        return mainMessageDate;
    }

    public void setMainMessageDate(Long mainMessageDate) {
        this.mainMessageDate = mainMessageDate;
    }

    public Long getFlags() {
        return flags;
    }

    public void setFlags(Long flags) {
        this.flags = flags;
    }
}
