package central.mail.cache.model;

public class MailboxThreadMessageCache<T> {
    private T gid;
    private T mailboxId;
    private T mailboxGid;

    public T getGid() {
        return gid;
    }

    public void setGid(T gid) {
        this.gid = gid;
    }

    public T getMailboxGid() {
        return mailboxGid;
    }

    public void setMailboxGid(T mailboxGid) {
        this.mailboxGid = mailboxGid;
    }
}
