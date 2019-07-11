package central.mail.cache.model;

import java.io.Serializable;
import java.util.UUID;

public class MailboxMessageCache<K, T> implements Serializable {
    private T messageGid;
    private T mailboxGid;
    private Long uid;
    private boolean expunged;


    public T getMessageGid() {
        return messageGid;
    }

    public void setMessageGid(T messageGid) {
        this.messageGid = messageGid;
    }

    public T getMailboxGid() {
        return mailboxGid;
    }

    public void setMailboxGid(T mailboxGid) {
        this.mailboxGid = mailboxGid;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public boolean isExpunged() {
        return expunged;
    }

    public void setExpunged(boolean expunged) {
        this.expunged = expunged;
    }
}
