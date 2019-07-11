package central.mail.cache.model;

public class ThreadMessageIdCache<K> {
    private String messageId;
    private K threadGid;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public K getThreadGid() {
        return threadGid;
    }

    public void setThreadGid(K threadGid) {
        this.threadGid = threadGid;
    }
}
