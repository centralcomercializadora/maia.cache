package central.mail.cache.impl.business;

import central.mail.cache.model.MailboxCache;
import central.mail.cache.model.MessageCache;

import java.io.Serializable;

public class UserCacheStore implements Serializable {
    private Long lastRefresh = null;
    private MailboxCache[] mailboxes;
    private MessageCache[] messages;

    public Long getLastRefresh() {
        return lastRefresh;
    }

    public void setLastRefresh(Long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    public MailboxCache[] getMailboxes() {
        return mailboxes;
    }

    public void setMailboxes(MailboxCache[] mailboxes) {
        this.mailboxes = mailboxes;
    }

    public MessageCache[] getMessages() {
        return messages;
    }

    public void setMessages(MessageCache[] messages) {
        this.messages = messages;
    }
}
