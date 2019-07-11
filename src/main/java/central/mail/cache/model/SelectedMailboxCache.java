package central.mail.cache.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectedMailboxCache<T> {
    private MailboxCache<T> mailbox;
    OrderedThreadCache<UUID> first = null;
    Map<UUID,OrderedThreadCache<UUID>> threadsByGid;

    public SelectedMailboxCache(MailboxCache<T> mailbox, OrderedThreadCache<UUID> first, Map<UUID, OrderedThreadCache<UUID>> threadsByGid) {
        this.mailbox = mailbox;
        this.first = first;
        this.threadsByGid = threadsByGid;
    }

    public MailboxCache<T> getMailbox() {
        return mailbox;
    }

    public void setMailbox(MailboxCache<T> mailbox) {
        this.mailbox = mailbox;
    }

    public OrderedThreadCache<UUID> getFirst() {
        return first;
    }

    public void setFirst(OrderedThreadCache<UUID> first) {
        this.first = first;
    }

    public Map<UUID, OrderedThreadCache<UUID>> getThreadsByGid() {
        return threadsByGid;
    }

    public void setThreadsByGid(Map<UUID, OrderedThreadCache<UUID>> threadsByGid) {
        this.threadsByGid = threadsByGid;
    }
}
