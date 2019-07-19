package central.mail.cache.model;

import javax.mail.Flags;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MessageCache<K, T> implements Serializable {
    public final static long SEEN = (long) 0b000000000001;
    public final static long ANSWERED = (long) 0b000000000010;
    public final static long FLAGGED = (long) 0b000000000100;
    public final static long DELETED = (long) 0b000000001000;
    public final static long DRAFT = (long) 0b000000010000;
    public final static long RECENT = (long) 0b000000100000;
    public final static long UNSEEN = (long) 0b000001000000;

    private K gid;
    private T mailboxGid;
    private Long mailboxUidValidity;
    private Long uid;
    private Long flags = 0l;
    private String[] userFlags;
    private Long internalDate;
    private String messageId;
    private String inReplyTo;
    private String references;
    private Long messageDate;
    private String from;
    private String subject;
    private String to;
    private String cc;
    private String bcc;
    private boolean expunged;


    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Long getFlags() {
        return flags;
    }

    public void setFlags(Long flags) {
        this.flags = flags;
    }

    public void setFlags(Flags.Flag[] systemFlags) {

        if (systemFlags == null) {
            return;
        }

        for (Flags.Flag flag : systemFlags) {
            if (flag == Flags.Flag.SEEN) {
                flags = flags | MessageCache.SEEN;
            } else if (flag == Flags.Flag.ANSWERED) {
                flags = flags | MessageCache.ANSWERED;
            }
            if (flag == Flags.Flag.FLAGGED) {
                flags = flags | MessageCache.FLAGGED;
            } else if (flag == Flags.Flag.DELETED) {
                flags = flags | MessageCache.DELETED;
            } else if (flag == Flags.Flag.RECENT) {
                flags = flags | MessageCache.RECENT;
            } else if (flag == Flags.Flag.DRAFT) {
                flags = flags | MessageCache.DRAFT;
            }
        }
    }

    public void setUserFlags(String[] userFlags) {
        this.userFlags = userFlags;
    }

    public String[] getUserFlags() {
        return userFlags;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public String getReferences() {
        return references;
    }


    public Long getInternalDate() {
        return internalDate;
    }

    public void setInternalDate(Long internalDate) {
        this.internalDate = internalDate;
    }

    public Long getMessageDate() {
        if (messageDate != null) {
            return messageDate;
        } else {
            return internalDate;
        }

    }

    public void setMessageDate(Long messageDate) {
        this.messageDate = messageDate;
    }

    public Long getMailboxUidValidity() {
        return mailboxUidValidity;
    }

    public void setMailboxUidValidity(Long mailboxUidValidity) {
        this.mailboxUidValidity = mailboxUidValidity;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public K getGid() {
        return gid;
    }

    public void setGid(K gid) {
        this.gid = gid;
    }

    public void setBcc(String bcc) {
        this.bcc = bcc;
    }

    public String getBcc() {
        return bcc;
    }

    public T getMailboxGid() {
        return mailboxGid;
    }

    public void setMailboxGid(T mailboxGid) {
        this.mailboxGid = mailboxGid;
    }

    public boolean isExpunged() {
        return expunged;
    }

    public void setExpunged(boolean expunged) {
        this.expunged = expunged;
    }
}
