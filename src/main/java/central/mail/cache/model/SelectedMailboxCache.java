package central.mail.cache.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SelectedMailboxCache<T> {
    private MailboxCache<T> mailbox;
    private OrderedThreadCache<UUID> first = null;
    private Map<UUID, OrderedThreadCache<UUID>> threadsByGid;
    private AtomicLong total = new AtomicLong();
    private AtomicLong unseen = new AtomicLong();
    private FilterType filterType;

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

    public AtomicLong getTotal() {
        return total;
    }

    public void setTotal(AtomicLong total) {
        this.total = total;
    }

    public AtomicLong getUnseen() {
        return unseen;
    }

    public void setUnseen(AtomicLong unseen) {
        this.unseen = unseen;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }

    public OrderedThreadCache<UUID> getThreadByGid(UUID gid) {
        return this.threadsByGid.get(gid);
    }
}
