package central.mail.cache.model;

import java.util.UUID;

public class MailboxMessageCache<K, T> {
    private T messageGid;
    private T mailboxGid;
    private String mailboxName;
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

    public String getMailboxName() {
        return mailboxName;
    }

    public void setMailboxName(String mailboxName) {
        this.mailboxName = mailboxName;
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
