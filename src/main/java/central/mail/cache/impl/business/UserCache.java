package central.mail.cache.impl.business;


import bee.error.BusinessException;
import bee.result.Result;
import bee.session.ExecutionContext;
import central.mail.cache.errors.MailboxNotFoundError;
import central.mail.cache.model.*;
import central.mail.store.model.Mailbox;
import central.mail.store.model.MailboxMessage;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.MultiValueAttribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static bee.result.Result.error;
import static bee.result.Result.ok;
import static com.googlecode.cqengine.query.QueryFactory.and;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.in;

public class UserCache implements Externalizable {

    public static final Attribute<MailboxCache<UUID>, String> MAILBOX_NAME = new SimpleAttribute<>("mailboxName") {
        public String getValue(MailboxCache<UUID> ob, QueryOptions queryOptions) {
            return ob.getName();
        }
    };

    public static final Attribute<MailboxCache<UUID>, UUID> MAILBOX_ID = new SimpleAttribute<>("mailboxId") {
        public UUID getValue(MailboxCache<UUID> ob, QueryOptions queryOptions) {
            return ob.getId();
        }
    };

    public static final Attribute<ThreadMessageCache<UUID>, UUID> THREAD_GID = new SimpleAttribute<>("threadId") {
        public UUID getValue(ThreadMessageCache<UUID> ob, QueryOptions queryOptions) {
            return ob.getGid();
        }
    };

    public static final Attribute<ThreadMessageCache<UUID>, String> THREAD_MESSAGEID = new SimpleAttribute<>("threadMessageId") {
        public String getValue(ThreadMessageCache<UUID> ob, QueryOptions queryOptions) {
            return ob.getMessageId();
        }
    };


    public static final Attribute<MessageCache<UUID, UUID>, UUID> MESSAGE_ID = new SimpleAttribute<>("messageId") {
        public UUID getValue(MessageCache<UUID, UUID> ob, QueryOptions queryOptions) {
            return ob.getGid();
        }
    };

    public static final Attribute<MailboxMessageCache<UUID, UUID>, UUID> MESSAGE_MAILBOXID = new SimpleAttribute<MailboxMessageCache<UUID, UUID>, UUID>("mailboxMessageId") {
        @Override
        public UUID getValue(MailboxMessageCache<UUID, UUID> o, QueryOptions queryOptions) {
            return o.getMailboxGid();
        }
    };

    public static final Attribute<MailboxThreadMessageCache<UUID>, UUID> MAILBOXTHREAD_MAILBOXID = new SimpleAttribute<MailboxThreadMessageCache<UUID>, UUID>("threadMailboxGid") {
        @Override
        public UUID getValue(MailboxThreadMessageCache<UUID> o, QueryOptions queryOptions) {
            return o.getMailboxGid();
        }
    };

    public static final Attribute<MailboxThreadMessageCache<UUID>, UUID> MAILBOXTHREAD_GID = new SimpleAttribute<MailboxThreadMessageCache<UUID>, UUID>("threadGid") {
        @Override
        public UUID getValue(MailboxThreadMessageCache<UUID> o, QueryOptions queryOptions) {
            return o.getGid();
        }
    };

    public Object sync = new Object();

    private String[] comparables = new String[]{"FROM", "SUBJECT", "DATE", "TO"};

    private IndexedCollection<MailboxCache<UUID>> mailboxes = new ConcurrentIndexedCollection<>();
    private IndexedCollection<MessageCache<UUID, UUID>> messages = new ConcurrentIndexedCollection<>();
    private IndexedCollection<MailboxMessageCache<UUID, UUID>> mailboxMessages = new ConcurrentIndexedCollection<>();
    private IndexedCollection<ThreadMessageCache<UUID>> threads = new ConcurrentIndexedCollection<>();
    private IndexedCollection<MailboxThreadMessageCache<UUID>> mailboxThreads = new ConcurrentIndexedCollection<>();
    private Long lastRefresh = null;
    private UUID mailboxGuidSelected;

    public UserCache() {


        this.mailboxes.addIndex(HashIndex.onAttribute(MAILBOX_NAME));
        this.mailboxes.addIndex(HashIndex.onAttribute(MAILBOX_ID));

        this.mailboxMessages.addIndex(HashIndex.onAttribute(MESSAGE_MAILBOXID));
        this.messages.addIndex(HashIndex.onAttribute(MESSAGE_ID));

        this.threads.addIndex(HashIndex.onAttribute(THREAD_GID));
        this.threads.addIndex(HashIndex.onAttribute(THREAD_MESSAGEID));

        this.mailboxThreads.addIndex(HashIndex.onAttribute(MAILBOXTHREAD_MAILBOXID));
        this.mailboxThreads.addIndex(HashIndex.onAttribute(MAILBOXTHREAD_GID));


    }

    public synchronized void addMailbox(MailboxCache mailbox) throws BusinessException {
        this.mailboxes.add(mailbox);
    }

    public synchronized void add(UUID mailboxId, MessageCache<UUID, UUID> message) throws BusinessException {
        synchronized (sync) {

            // todos los mensajes deben tener messageid
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID() + "-auto");
            }

            var mailbox = this.getMailboxById(mailboxId);


            var currentMessage = this.getMessageById(message.getGid());
            if (currentMessage == null) {
                this.messages.add(message);
                processThreads(message);
                currentMessage = message;
            }

            var mailboxMessage = new MailboxMessageCache<UUID, UUID>();
            mailboxMessage.setMessageGid(message.getGid());
            mailboxMessage.setMailboxGid(mailbox.getId());
            mailboxMessage.setMailboxName(mailbox.getName());
            this.mailboxMessages.add(mailboxMessage);

            currentMessage.getMailboxMessages().add(mailboxMessage);

        }
    }


    // un thread pertenece a un mailbox si almenos uno de los mensajes agrupados en el pertenece al mailbox
    public synchronized void processMailboxThreadMessage(ThreadMessageCache<UUID> thread) {
        for (var messageGid : thread.getMessages()) {
            var message = this.getMessageById(messageGid);
            if (message == null) {
                continue;
            }
            for (var mailbox : message.getMailboxMessages()) {
                var current = this.fetchMailboxThreadMessageByMailboxAndThread(mailbox.getMailboxGid(), thread.getGid());
                if (current == null) {
                    current = new MailboxThreadMessageCache<>();
                    current.setGid(thread.getGid());
                    current.setMailboxGid(mailbox.getMailboxGid());
                    this.mailboxThreads.add(current);
                }
            }
        }

    }

    // un thread es un grupo de mensajes
    // sea el mensaje x con message-id mx
    // sea el mensaje y con message-id my y references : (mx)
    // sea el thread tx el thread del mensae mx
    // el mensaje y se agrupa en el thread tx pq hace referencia al message-id el mensage x dentro del header references o inreplyto
    // si no existe un thread al que el my pertenezca creo un nuevo thread

    public synchronized void processThreads(MessageCache<UUID, UUID> message) {


        // busco los threads segun las referencias que tengo
        Set<String> references = new HashSet<>();

        if (message.getInReplyTo() != null) {
            String messageIdInReplyTo = message.getInReplyTo().trim();
            references.add(messageIdInReplyTo);
        }

        if (message.getReferences() != null) {
            String[] referencesId = message.getReferences().replaceAll(",", " ").trim().split(" ");
            if (referencesId != null && referencesId.length > 0) {
                for (String referenceId : referencesId) {
                    references.add(referenceId.trim());
                }
            }
        }

        ThreadMessageCache<UUID> thread = null;

        for (String reference : references) {

            if (reference.trim().isEmpty()) {
                continue;
            }

            thread = this.fetchThreadMessageByMessageId(reference);

            if (thread != null) {
                break;
            }
        }

        if (thread == null) {
            thread = new ThreadMessageCache<>();
            thread.setGid(UUID.randomUUID());
            thread.setMessageId(message.getMessageId());
            this.threads.add(thread);
        }

        thread.getMessages().add(message.getGid());

        processMailboxThreadMessage(thread);

        /*String messageId = message.getMessageId().trim();

        references.add(messageId);*/




        /*List<MessageGID> messagesInThread = null;
        if (threadId != null) {
            messagesInThread = threadMessages.get(threadId);

        } else {
            threadId = UUID.randomUUID().toString();
            //threads.put(message.getMessageId(), threadId);
        }

        if (messagesInThread == null) {
            messagesInThread = new ArrayList<>();
            threadMessages.put(threadId, messagesInThread);
        }

        //MessageGID gid = new MessageGID(message.getMailboxName(), message.getUid());
        //messagesInThread.add(gid);


        for (String referece : references) {
            //if (!this.threads.containsKey(referece)) {
            //    this.threads.put(referece, threadId);
            //}

        }

        */
    }

    public synchronized void sortMailbox(String mailboxName, String type) {
        /*synchronized (sync) {
            Boolean toSort = sortDateByMailbox.get(mailboxName);
            if (toSort == null || toSort) {

                //Obtengo los mensajes del mailbox y los ordeno

                List<MailboxMessageCache> messages = this.messagesByMailboxOrderedByDate.get(mailboxName);
                if (messages != null) {
                    Collections.sort(messages, (x, y) -> {
                        int c = Long.compare(x.getMessageDate(), y.getMessageDate());
                        if (c != 0) return c;
                        return Long.compare(x.getUid(), y.getUid());
                    });

                    //Process next and prev
                    int size = messages.size();
                    for (int i = 0; i < size; i++) {
                        MailboxMessageCache current = messages.get(i);
                        MailboxMessageCache next = null;
                        if (i < (size - 1)) {
                            next = messages.get(i + 1);
                        }

                        if (next != null) {
                            current.setNext(new MessageGID(next.getMailboxName(), next.getUid()));
                            next.setPrev(new MessageGID(current.getMailboxName(), current.getUid()));
                        }
                    }
                }

                sortDateByMailbox.put(mailboxName, false);
            }
        }*/
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        System.out.println("escribiendo al cache");
        long start = System.currentTimeMillis();
        Store store = new Store();
        store.setMailboxes(Arrays.asList(this.mailboxes.toArray(new MailboxCache[]{})));
        store.setMessages(Arrays.asList(this.messages.toArray(new MessageCache[]{})));
        store.lastRefresh = this.lastRefresh;
        out.writeObject(store);
        System.out.println("WRITE:::" + store.getMessages().size() + " messages >>>" + (System.currentTimeMillis() - start) + "ms");
    }

    private void init(Store store) throws IOException {
        long start = System.currentTimeMillis();
        try {
            for (MailboxCache mailboxCache : store.getMailboxes()) {
                this.addMailbox(mailboxCache);
            }

            for (MessageCache message : store.getMessages()) {
                //this.add(this.mailboxesByName.get(message.getMailboxName()), message);
            }
            this.lastRefresh = store.lastRefresh;
        } catch (BusinessException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }

        System.out.println("INITIALIZANDO:::" + store.getMessages().size() + " messages >>>" + (System.currentTimeMillis() - start) + "ms");

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        System.out.println("leyendo del cache");
        long start = System.currentTimeMillis();
        Store store = (Store) in.readObject();
        System.out.println("READ:::" + (System.currentTimeMillis() - start));
        init(store);
    }


    public static class Store implements Serializable {
        private Long lastRefresh = null;
        private Collection<MailboxCache> mailboxes;
        private Collection<MessageCache> messages;

        public Collection<MailboxCache> getMailboxes() {
            return mailboxes;
        }

        public void setMailboxes(Collection<MailboxCache> mailboxes) {
            this.mailboxes = mailboxes;
        }

        public Collection<MessageCache> getMessages() {
            return messages;
        }

        public void setMessages(Collection<MessageCache> messages) {
            this.messages = messages;
        }
    }

    public Long getLastRefresh() {
        return lastRefresh;
    }

    public void setLastRefresh(Long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    public List<MailboxCache> getMailboxes() {
        return new ArrayList(this.mailboxes);
    }

    public Result<MailboxCache<UUID>> getMailboxByName(String name) {

        ResultSet<MailboxCache<UUID>> rs = null;

        try {
            rs = this.mailboxes.retrieve(equal(MAILBOX_NAME, name));
            if (rs.isEmpty()) {
                return null;
            }
            var res = (MailboxCache) rs.iterator().next();
            if (res == null) {
                return error(new MailboxNotFoundError());
            }
            return ok(res);
        } finally {
            rs.close();
        }
    }

    public MailboxCache<UUID> getMailboxById(UUID id) {

        ResultSet<MailboxCache<UUID>> rs = null;

        try {
            rs = this.mailboxes.retrieve(equal(MAILBOX_ID, id));
            if (rs.isEmpty()) {
                return null;
            }
            var res = (MailboxCache) rs.iterator().next();
            return res;
        } finally {
            rs.close();
        }
    }


    public Result<Iterator<MailboxMessageCache<UUID, UUID>>> fetchMailboxMessagesInMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var mailbox = this.getMailboxByName(name);
        if (mailbox.isError()) {
            return error(mailbox.getErrores());
        }
        ResultSet<MailboxMessageCache<UUID, UUID>> rs = null;
        try {
            rs = this.mailboxMessages.retrieve(equal(MESSAGE_MAILBOXID, mailbox.ok().getId()));
            if (rs.isEmpty()) {
                return ok(new ArrayList<MailboxMessageCache<UUID, UUID>>().iterator());
            }
            return ok(rs.iterator());
        } finally {
        }
    }


    public MessageCache<UUID, UUID> getMessageById(UUID id) {

        ResultSet<MessageCache<UUID, UUID>> rs = null;

        try {
            rs = this.messages.retrieve(equal(MESSAGE_ID, id));
            if (rs.isEmpty()) {
                return null;
            }
            var res = (MessageCache<UUID, UUID>) rs.iterator().next();
            return res;
        } finally {
            rs.close();
        }
    }

    public ThreadMessageCache<UUID> fetchThreadMessageByMessageId(String messageId) {

        ResultSet<ThreadMessageCache<UUID>> rs = null;

        try {
            rs = this.threads.retrieve(equal(THREAD_MESSAGEID, messageId));
            if (rs.isEmpty()) {
                return null;
            }
            var res = (ThreadMessageCache<UUID>) rs.iterator().next();
            return res;
        } finally {
            rs.close();
        }
    }

    public ThreadMessageCache<UUID> fetchThreadMessageByGd(UUID gid) {

        ResultSet<ThreadMessageCache<UUID>> rs = null;

        try {
            rs = this.threads.retrieve(equal(THREAD_GID, gid));
            if (rs.isEmpty()) {
                return null;
            }
            var res = (ThreadMessageCache<UUID>) rs.iterator().next();
            return res;
        } finally {
            rs.close();
        }
    }

    public MailboxThreadMessageCache<UUID> fetchMailboxThreadMessageByMailboxAndThread(UUID threadGid, UUID mailboxGid) {

        ResultSet<MailboxThreadMessageCache<UUID>> rs = null;

        try {
            rs = this.mailboxThreads.retrieve(and(equal(MAILBOXTHREAD_GID, threadGid), equal(MAILBOXTHREAD_MAILBOXID, mailboxGid)));
            if (rs.isEmpty()) {
                return null;
            }
            var res = (MailboxThreadMessageCache<UUID>) rs.iterator().next();
            return res;
        } finally {
            rs.close();
        }
    }


    public Integer getThreadsCount() {
        return this.threads.size();
    }

    public Integer getMessagesCount() {
        return this.messages.size();
    }


    public Result<MailboxCache<UUID>> selectMailbox(String name) throws BusinessException {
        // consultar el mailbox por nombre
        var mailbox = this.getMailboxByName(name);
        if (mailbox.isError()) {
            return mailbox;
        }
        return this.selectMailbox(mailbox.ok());
    }

    public Result<MailboxCache<UUID>> selectMailbox(MailboxCache<UUID> mailbox) {
        //filtrar los threas que esten en el mailbox y ordenarlos

        //filtro
        var threadsInMailboxRs = this.mailboxThreads.retrieve(equal(MAILBOXTHREAD_MAILBOXID, mailbox.getId()));

        //los agrego a una lista temporal
        var threads = new ArrayList<ThreadMessageCache<UUID>>();

        if (threadsInMailboxRs != null) {
            var it = threadsInMailboxRs.iterator();
            while (it.hasNext()) {
                var mailboxThreadMessage = it.next();
                var thread = this.fetchThreadMessageByGd(mailboxThreadMessage.getGid());
                if (thread != null) {
                    threads.add(thread);
                }
            }
        }

        System.out.println(threads.size());
        return ok(mailbox);
    }
}
