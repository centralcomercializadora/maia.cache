package central.mail.cache.impl.business;


import bee.error.BusinessException;
import bee.result.Result;
import bee.session.ExecutionContext;
import central.mail.cache.errors.MailboxNotFoundError;
import central.mail.cache.model.*;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static bee.result.Result.error;
import static bee.result.Result.ok;
import static com.googlecode.cqengine.query.QueryFactory.and;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.in;

public class UserCache {

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

    public static final Attribute<ThreadMessageIdCache<UUID>, String> MESSAGEIDTHREAD_MESSAGEID = new SimpleAttribute<ThreadMessageIdCache<UUID>, String>("messageId") {
        @Override
        public String getValue(ThreadMessageIdCache<UUID> o, QueryOptions queryOptions) {
            return o.getMessageId();
        }
    };


    public Object sync = new Object();

    private String[] comparables = new String[]{"FROM", "SUBJECT", "DATE", "TO"};

    private IndexedCollection<MailboxCache<UUID>> mailboxes = new ConcurrentIndexedCollection<>();
    private IndexedCollection<MessageCache<UUID, UUID>> messages = new ConcurrentIndexedCollection<>();
    private IndexedCollection<MailboxMessageCache<UUID, UUID>> mailboxMessages = new ConcurrentIndexedCollection<>();
    private IndexedCollection<ThreadMessageCache<UUID>> threads = new ConcurrentIndexedCollection<>();
    private IndexedCollection<MailboxThreadMessageCache<UUID>> mailboxThreads = new ConcurrentIndexedCollection<>();
    private IndexedCollection<ThreadMessageIdCache<UUID>> messageIdThreads = new ConcurrentIndexedCollection<>();
    private Map<UUID, UUID> threadByMessageGid = new ConcurrentHashMap<>();

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

        this.messageIdThreads.addIndex(HashIndex.onAttribute(MESSAGEIDTHREAD_MESSAGEID));
    }

    public synchronized void addMailbox(MailboxCache mailbox) throws BusinessException {
        this.mailboxes.add(mailbox);
    }

    public synchronized void add(MessageCache<UUID, UUID> message) throws BusinessException {
        add(message, true);
    }

    public synchronized void add(MessageCache<UUID, UUID> message, boolean syncThreads) throws BusinessException {
        synchronized (sync) {
            UUID mailboxId = message.getMailboxGid();

            // todos los mensajes deben tener messageid
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID() + "-auto");
            }

            var mailbox = this.getMailboxById(mailboxId);

            var currentMessage = this.getMessageById(message.getGid());
            if (currentMessage == null) {
                this.messages.add(message);
            }

            var mailboxMessage = new MailboxMessageCache<UUID, UUID>();
            mailboxMessage.setMessageGid(message.getGid());
            mailboxMessage.setMailboxGid(mailbox.getId());
            this.mailboxMessages.add(mailboxMessage);
        }
    }


    public synchronized void addNoSync(MessageCache<UUID, UUID> message) throws BusinessException {
        add(message, false);
    }


    // un thread pertenece a un mailbox si almenos uno de los mensajes agrupados en el pertenece al mailbox
    public synchronized void processMailboxThreadMessages() {
        for (var thread : this.threads) {
            for (var messageGid : thread.getMessages()) {
                var message = this.getMessageById(messageGid);
                if (message == null) {
                    continue;
                }

                var current = this.fetchMailboxThreadMessageByMailboxAndThread(message.getMailboxGid(), thread.getGid());
                if (current == null) {
                    current = new MailboxThreadMessageCache<>();
                    current.setGid(thread.getGid());
                    current.setMailboxGid(message.getMailboxGid());
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

    public synchronized void processThreads() {

        for (var message : this.messages) {


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

            ThreadMessageIdCache<UUID> currentThreadMessageId = null;


            for (String reference : references) {

                if (reference.trim().isEmpty()) {
                    continue;
                }

                currentThreadMessageId = this.fetchThreadMessageIdByMessageId(reference);

                if (currentThreadMessageId != null) {
                    break;
                }
            }

            if (currentThreadMessageId == null) {
                currentThreadMessageId = this.fetchThreadMessageIdByMessageId(message.getMessageId());
            }


            ThreadMessageCache<UUID> thread = null;

            if (currentThreadMessageId != null) {
                thread = this.fetchThreadMessageByGid(currentThreadMessageId.getThreadGid());
            }

            if (thread == null) {
                thread = new ThreadMessageCache<>();
                thread.setGid(UUID.randomUUID());
                thread.setMessageId(message.getMessageId());
                this.threads.add(thread);
            }

            if (thread.getMainMessageGid() == null) {
                thread.setMainMessageGid(message.getGid());
                thread.setMainMessageDate(message.getMessageDate());
            } else {
                if (message.getMessageDate() >= thread.getMainMessageDate()) {
                    thread.setMainMessageDate(message.getMessageDate());
                    thread.setMainMessageGid(message.getGid());
                }
            }

            thread.getMessages().add(message.getGid());
            threadByMessageGid.put(message.getGid(), thread.getGid());

            thread.setFlags(thread.getFlags() | message.getFlags());


            // las referencias y el messageid debe apuntar al thread
            for (var reference : references) {
                var threadMessageId = new ThreadMessageIdCache<UUID>();
                threadMessageId.setMessageId(reference);
                threadMessageId.setThreadGid(thread.getGid());
                //solo lo agrego si no esta
                if (this.fetchThreadMessageIdByMessageId(reference) == null) {
                    this.messageIdThreads.add(threadMessageId);
                }

            }
            if (this.fetchThreadMessageIdByMessageId(message.getMessageId()) == null) {
                var threadMessageId = new ThreadMessageIdCache<UUID>();
                threadMessageId.setMessageId(message.getMessageId());
                threadMessageId.setThreadGid(thread.getGid());
                //solo lo agrego si no esta

                this.messageIdThreads.add(threadMessageId);
            }


        }

        processMailboxThreadMessages();
    }

    public UserCacheStore toStore() throws IOException, ClassNotFoundException {
        System.out.println("escribiendo al cache");
        long start = System.currentTimeMillis();
        UserCacheStore store = new UserCacheStore();
        store.setMailboxes(this.mailboxes.toArray(new MailboxCache[]{}));
        store.setMessages(this.messages.toArray(new MessageCache[]{}));
        store.setLastRefresh(this.lastRefresh);
        return store;
    }

    public void init(UserCacheStore store) throws IOException {
        long start = System.currentTimeMillis();
        try {
            for (MailboxCache<UUID> mailboxCache : store.getMailboxes()) {
                this.addMailbox(mailboxCache);
            }

            for (MessageCache<UUID, UUID> message : store.getMessages()) {
                this.addNoSync(message);
            }
            this.lastRefresh = store.getLastRefresh();
            this.processThreads();
        } catch (BusinessException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }

        System.out.println("INITIALIZANDO:::" + store.getMessages().length + " messages >>>" + (System.currentTimeMillis() - start) + "ms");

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
                return error(new MailboxNotFoundError());
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

    public ThreadMessageIdCache<UUID> fetchThreadMessageIdByMessageId(String messageId) {

        ResultSet<ThreadMessageIdCache<UUID>> rs = null;

        try {
            rs = this.messageIdThreads.retrieve(equal(MESSAGEIDTHREAD_MESSAGEID, messageId));
            if (rs.isEmpty()) {
                return null;
            }
            var res = (ThreadMessageIdCache<UUID>) rs.iterator().next();
            return res;
        } finally {
            rs.close();
        }
    }

    public ThreadMessageCache<UUID> fetchThreadMessageByGid(UUID gid) {

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


    public Result<SelectedMailboxCache<UUID>> selectMailbox(String name, Sort sort, SortType sortType) throws BusinessException {
        // consultar el mailbox por nombre
        var mailbox = this.getMailboxByName(name);
        if (mailbox.isError()) {
            return error(mailbox.getErrores());
        }
        return this.selectMailbox(mailbox.ok(), sort, sortType);
    }

    public Result<SelectedMailboxCache<UUID>> selectMailbox(MailboxCache<UUID> mailbox, Sort sort, SortType sortType) {
        //saber si debo volver a procesar todo el cache


        //filtrar los threas que esten en el mailbox y ordenarlos

        //filtro
        var threadsInMailboxRs = this.mailboxThreads.retrieve(equal(MAILBOXTHREAD_MAILBOXID, mailbox.getId()));

        //los agrego a una lista temporal
        var threads = new ArrayList<OrderedThreadCache<UUID>>();
        var gids = new HashSet<UUID>();

        if (threadsInMailboxRs != null) {
            var it = threadsInMailboxRs.iterator();
            while (it.hasNext()) {
                var mailboxThreadMessage = it.next();
                var thread = this.fetchThreadMessageByGid(mailboxThreadMessage.getGid());
                if (thread != null) {
                    if (!gids.contains(thread.getGid())) {
                        gids.add(thread.getGid());

                        var message = this.getMessageById(thread.getMainMessageGid());

                        var orderedThread = new OrderedThreadCache();


                        // depende del ordenamiento que quira darle


                        switch (sort) {
                            case DATE:
                                orderedThread.setOrderProperty(message.getMessageDate());
                                if (orderedThread.getOrderProperty() == null) {
                                    orderedThread.setOrderProperty(0l);
                                }
                                break;
                            case FROM:
                                orderedThread.setOrderProperty(message.getFrom());
                                if (orderedThread.getOrderProperty() == null) {
                                    orderedThread.setOrderProperty("");
                                }
                                break;
                            case SUBJECT:
                                orderedThread.setOrderProperty(message.getSubject());
                                if (orderedThread.getOrderProperty() == null) {
                                    orderedThread.setOrderProperty("");
                                }
                                break;
                        }


                        orderedThread.setMessageGid(thread.getMainMessageGid());
                        orderedThread.setThreadGid(thread.getGid());
                        threads.add(orderedThread);
                    }
                }
            }
        }

        // ordeno los threads
        var sortAux = (sortType == SortType.DESC) ? -1 : 1;
        Collections.sort(threads, (o, r) -> sortAux * o.getOrderProperty().compareTo(r.getOrderProperty()));

        OrderedThreadCache<UUID> first = null;
        Map<UUID, OrderedThreadCache<UUID>> threadsByGid = new ConcurrentHashMap<>();

        var size = threads.size();

        for (int i = 0; i < size; i++) {
            var current = threads.get(i);

            if (i == 0) {
                first = current;
            }

            threadsByGid.put(current.getThreadGid(), current);

            if ((i + 1) < threads.size()) {
                var next = threads.get(i + 1);
                current.setNext(next.getThreadGid());
                next.setPrev(current.getThreadGid());
                threadsByGid.put(next.getThreadGid(), next);
            }
        }

        System.out.println("threads all: " + this.threads.size());
        System.out.println("threads: " + this.mailboxThreads.size());
        System.out.println("mailbox threads: " + threads.size());
        return ok(new SelectedMailboxCache<>(mailbox, first, threadsByGid));
    }

}
