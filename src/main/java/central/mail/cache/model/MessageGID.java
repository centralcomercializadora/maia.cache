package central.mail.cache.model;

import java.util.UUID;

public class MessageGID {
    private UUID gid;
    private String mailboxName;
    private Long uid;

    public MessageGID(String mailboxName, Long uid) {
        this.mailboxName = mailboxName;
        this.uid = uid;
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

    public String toId() {
        StringBuffer sb = new StringBuffer();
        sb.append("m(").append(this.mailboxName).append(")|uid(").append(uid).append(")");
        return sb.toString();
    }


    @Override
    public String toString() {
        return "MessageGID{" + toId() + "}";
    }
}
