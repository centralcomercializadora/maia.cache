package central.mail.cache.model;

import java.util.concurrent.atomic.AtomicLong;

public class MailboxCache<T> {
    private T id;
    private String name;
    private long uidValidity;
    private AtomicLong total = new AtomicLong(0l);
    private AtomicLong unseen = new AtomicLong(0l);
    private AtomicLong recent = new AtomicLong(0l);
    private Long maxUid = 0l;
    private Long maxDate = 0l;
    private Long modseq = 0l;
    private boolean toOrderByUid = false;
    private boolean toOrderByDate = false;
    private Long uidNext;
    private Long lastModSeq;
    private Long lastUidNext;

    public T getId() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public AtomicLong getRecent() {
        return recent;
    }

    public void setRecent(AtomicLong recent) {
        this.recent = recent;
    }

    private synchronized void updateCount(Long flags, boolean increment, boolean countTotal) {

        if (countTotal) {
            if (increment) {
                total.incrementAndGet();
            } else {
                total.decrementAndGet();
            }
        }

        if (flags != null) {
            if ((MessageCache.UNSEEN & flags.longValue()) == MessageCache.UNSEEN) {
                if (increment) {
                    unseen.incrementAndGet();
                } else {
                    unseen.decrementAndGet();
                }
            }

            if ((MessageCache.RECENT & flags.longValue()) == MessageCache.RECENT) {
                if (increment) {
                    recent.incrementAndGet();
                } else {
                    recent.decrementAndGet();
                }
            }
        }

    }


    public Long getModseq() {
        return modseq;
    }

    public void setModseq(Long modseq) {
        this.modseq = modseq;
    }

    public void setUidNext(Long uidNext) {
        this.uidNext = uidNext;
    }

    public Long getUidNext() {
        return uidNext;
    }

    public Long getLastModSeq() {
        return lastModSeq;
    }

    public void setLastModSeq(Long lastModSeq) {
        this.lastModSeq = lastModSeq;
    }

    public Long getLastUidNext() {
        return lastUidNext;
    }

    public void setLastUidNext(Long lastUidNext) {
        this.lastUidNext = lastUidNext;
    }

    public long getUidValidity() {
        return uidValidity;
    }

    public void setUidValidity(long uidValidity) {
        this.uidValidity = uidValidity;
    }

    public Long getMaxUid() {
        return maxUid;
    }

    public void setMaxUid(Long maxUid) {
        this.maxUid = maxUid;
    }

    public Long getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(Long maxDate) {
        this.maxDate = maxDate;
    }

    public boolean isToOrderByUid() {
        return toOrderByUid;
    }

    public void setToOrderByUid(boolean toOrderByUid) {
        this.toOrderByUid = toOrderByUid;
    }

    public boolean isToOrderByDate() {
        return toOrderByDate;
    }

    public void setToOrderByDate(boolean toOrderByDate) {
        this.toOrderByDate = toOrderByDate;
    }

    @Override
    public String toString() {
        return "MailboxCache{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", total=" + total +
                ", unseen=" + unseen +
                ", recent=" + recent +
                ", maxUid=" + maxUid +
                ", maxDate=" + maxDate +
                ", modseq=" + modseq +
                ", toOrderByUid=" + toOrderByUid +
                ", toOrderByDate=" + toOrderByDate +
                ", uidNext=" + uidNext +
                ", lastModSeq=" + lastModSeq +
                ", lastUidNext=" + lastUidNext +
                '}';
    }
}


