package central.mail.cache.model;

import java.util.ArrayList;
import java.util.List;

public class MailboxThread {
    private MessageGID message;
    private List<MessageGID> threads = new ArrayList<>();
    private List<MessageGID> referencedBy = new ArrayList<>();

    public MessageGID getMessage() {
        return message;
    }

    public void setMessage(MessageGID message) {
        this.message = message;
    }

    public List<MessageGID> getThreads() {
        return threads;
    }

    public void setThreads(List<MessageGID> threads) {
        this.threads = threads;
    }

    public List<MessageGID> getReferencedBy() {
        return referencedBy;
    }

    public void setReferencedBy(List<MessageGID> referencedBy) {
        this.referencedBy = referencedBy;
    }
}
