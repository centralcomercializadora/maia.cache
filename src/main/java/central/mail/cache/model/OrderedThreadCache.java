package central.mail.cache.model;

import java.util.UUID;

public class OrderedThreadCache<K> {
    private Comparable orderProperty;
    private K threadGid;
    private K messageGid;
    private K next;
    private K prev;

    public Comparable getOrderProperty() {
        return orderProperty;
    }

    public void setOrderProperty(Comparable orderProperty) {
        this.orderProperty = orderProperty;
    }

    public K getThreadGid() {
        return threadGid;
    }

    public void setThreadGid(K threadGid) {
        this.threadGid = threadGid;
    }

    public K getMessageGid() {
        return messageGid;
    }

    public void setMessageGid(K messageGid) {
        this.messageGid = messageGid;
    }

    public K getNext() {
        return next;
    }

    public void setNext(K next) {
        this.next = next;
    }

    public K getPrev() {
        return prev;
    }

    public void setPrev(K prev) {
        this.prev = prev;
    }
}
