package central.mail.cache.model;

import com.googlecode.cqengine.resultset.ResultSet;

import java.util.Iterator;
import java.util.UUID;

public class MessageIterator implements Iterator<MessageCache<UUID, UUID>> {
    private final ResultSet<MessageCache<UUID, UUID>> resultset;
    private final Iterator<MessageCache<UUID, UUID>> iterator;

    public MessageIterator(ResultSet<MessageCache<UUID, UUID>> resultset) {
        this.resultset = resultset;
        this.iterator = resultset.iterator();
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public MessageCache<UUID, UUID> next() {
        return this.iterator.next();
    }
}
