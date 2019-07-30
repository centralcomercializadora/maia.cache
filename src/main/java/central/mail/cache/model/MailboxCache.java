package central.mail.cache.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class MailboxCache<T> implements Serializable {
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
    private boolean removed = false;
    private boolean junk;
    private boolean trash;

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

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean isJunk() {
        return junk;
    }

    public void setJunk(boolean junk) {
        this.junk = junk;
    }

    public boolean isTrash() {
        return trash;
    }

    public void setTrash(boolean trash) {
        this.trash = trash;
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
                ", uidNext=" + uidNext
                + '}';
    }
}


