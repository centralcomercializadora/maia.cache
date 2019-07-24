package central.mail.cache.impl.business;


import central.mail.cache.errors.MailboxNotFoundError;
import central.mail.cache.model.*;
import cognitivesolutions.error.BusinessException;
import cognitivesolutions.result.Result;
import cognitivesolutions.session.RequestCommand;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


import static cognitivesolutions.result.Result.error;
import static cognitivesolutions.result.Result.ok;
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

    public static final Attribute<MessageCache<UUID, UUID>, Long> MESSAGE_UIDVALIDITY = new SimpleAttribute<>("messageUidValidity") {
        public Long getValue(MessageCache<UUID, UUID> ob, QueryOptions queryOptions) {
            return ob.getMailboxUidValidity();
        }
    };

    public static final Attribute<MessageCache<UUID, UUID>, Long> MESSAGE_UID = new SimpleAttribute<>("messageUid") {
        public Long getValue(MessageCache<UUID, UUID> ob, QueryOptions queryOptions) {
            return ob.getUid();
        }
    };

    public static final Attribute<MessageCache<UUID, UUID>, UUID> MESSAGE_MAILBOXGID = new SimpleAttribute<>("messageMailboxGid") {
        public UUID getValue(MessageCache<UUID, UUID> ob, QueryOptions queryOptions) {
            return ob.getMailboxGid();
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
    private IndexedCollection<ThreadMessageCache<UUID>> threads = new ConcurrentIndexedCollection<>();
    private IndexedCollection<MailboxThreadMessageCache<UUID>> mailboxThreads = new ConcurrentIndexedCollection<>();
    private IndexedCollection<ThreadMessageIdCache<UUID>> messageIdThreads = new ConcurrentIndexedCollection<>();
    private Map<UUID, UUID> threadByMessageGid = new ConcurrentHashMap<>();
    private Map<String, SelectedMailboxCache<UUID>> selectedMailboxesByMailboxGid = new ConcurrentHashMap<>();
    private ReentrantLock lock = new ReentrantLock();

    private Long lastRefresh = null;
    private UUID mailboxGuidSelected;


    public UserCache() {


        this.mailboxes.addIndex(HashIndex.onAttribute(MAILBOX_NAME));
        this.mailboxes.addIndex(HashIndex.onAttribute(MAILBOX_ID));

        this.messages.addIndex(HashIndex.onAttribute(MESSAGE_ID));
        this.messages.addIndex(HashIndex.onAttribute(MESSAGE_UIDVALIDITY));
        this.messages.addIndex(HashIndex.onAttribute(MESSAGE_UID));
        this.messages.addIndex(HashIndex.onAttribute(MESSAGE_MAILBOXGID));

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
        try {
            this.lock.lock();

            UUID mailboxId = message.getMailboxGid();

            // todos los mensajes deben tener messageid
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID() + "-auto");
            }

            var currentMessage = this.getMessageById(message.getGid());
            if (currentMessage == null) {
                this.messages.add(message);
            }

            if (syncThreads) {
                this.processThreadsMessage(message, true, true);
            }
        } finally {
            if (this.lock.isLocked()) {
                this.lock.unlock();
            }
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
                this.addMailboxThread(thread, message);
            }
        }
    }

    private void addMailboxThread(ThreadMessageCache<UUID> thread, MessageCache<UUID, UUID> message) {
        var current = this.fetchMailboxThreadMessageByMailboxAndThread(message.getMailboxGid(), thread.getGid());
        if (current == null) {
            current = new MailboxThreadMessageCache<>();
            current.setGid(thread.getGid());
            current.setMailboxGid(message.getMailboxGid());
            this.mailboxThreads.add(current);
        }
    }

    // un thread es un grupo de mensajes
    // sea el mensaje x con message-id mx
    // sea el mensaje y con message-id my y references : (mx)
    // sea el thread tx el thread del mensae mx
    // el mensaje y se agrupa en el thread tx pq hace referencia al message-id el mensage x dentro del header references o inreplyto
    // si no existe un thread al que el my pertenezca creo un nuevo thread

    public synchronized void processThreads() {

        try {
            this.lock.lock();


            this.threads.clear();
            this.messageIdThreads.clear();
            this.mailboxThreads.clear();
            this.threadByMessageGid.clear();
            this.selectedMailboxesByMailboxGid.clear();

            for (var message : this.messages) {
                // busco los threads segun las referencias que tengo
                processThreadsMessage(message, false, false);
            }

            //sync all flags
            for (var thread : this.threads) {
                syncThreadFlags(thread);
            }


            processMailboxThreadMessages();
        } finally {
            if (this.lock.isLocked()) {
                this.lock.unlock();
            }
        }
    }

    private void processThreadsMessage(MessageCache<UUID, UUID> message, boolean syncAllFlags, boolean syncMailboxThreads) {
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

        if (syncAllFlags) {
            syncThreadFlags(thread);
        }

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

        if (syncMailboxThreads) {
            this.selectedMailboxesByMailboxGid.remove(message.getMailboxGid().toString());
            this.addMailboxThread(thread, message);
        }
    }

    private void syncThreadFlags(ThreadMessageCache<UUID> thread) {
        thread.setFlags(0l);
        thread.setExpunged(true);
        for (var messageGidInThread : thread.getMessages()) {
            var messageInThread = this.getMessageById(messageGidInThread);
            if (messageInThread != null) {
                if (!messageInThread.isExpunged()) {
                    thread.setExpunged(false);
                    thread.setFlags(thread.getFlags() | messageInThread.getFlags());
                    if ((messageInThread.getFlags() & MessageCache.SEEN) == 0) {
                        // el mensaje no esta leido
                        thread.setFlags(thread.getFlags() | MessageCache.UNSEEN);
                    }

                }
            }
        }
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

    public List<MailboxCache<UUID>> getMailboxes() {
        return new ArrayList<MailboxCache<UUID>>(this.mailboxes).stream().filter(x -> !x.isRemoved()).collect(Collectors.toList());
    }

    public Result<MailboxCache<UUID>> getMailboxByName(String name) {

        ResultSet<MailboxCache<UUID>> rs = null;

        try {
            rs = this.mailboxes.retrieve(equal(MAILBOX_NAME, name));
            if (rs.isEmpty()) {
                return error(new MailboxNotFoundError());
            }
            var it = rs.iterator();
            while (it.hasNext()) {
                var res = (MailboxCache<UUID>) it.next();
                if (res == null) {
                    return error(new MailboxNotFoundError());
                }
                if (res.isRemoved()) {
                    continue;
                }
                return ok(res);
            }


        } finally {
            rs.close();
        }

        return error(new MailboxNotFoundError());
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


    public Result<Iterator<MessageCache<UUID, UUID>>> fetchMessagesInMailboxByName(String name, RequestCommand ec) throws BusinessException {
        var mailbox = this.getMailboxByName(name);
        if (mailbox.isError()) {
            return error(mailbox.getErrores());
        }
        ResultSet<MessageCache<UUID, UUID>> rs = null;
        try {
            rs = this.messages.retrieve(equal(MESSAGE_MAILBOXGID, mailbox.ok().getId()));
            if (rs.isEmpty()) {
                return ok(new ArrayList<MessageCache<UUID, UUID>>().iterator());
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

    public MessageCache<UUID, UUID> getMessageByMailboxIdAndUid(UUID mailboxGid, Long uid) {

        ResultSet<MessageCache<UUID, UUID>> rs = null;

        try {
            rs = this.messages.retrieve(and(equal(MESSAGE_MAILBOXGID, mailboxGid), equal(MESSAGE_UID, uid)));
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


    public Result<SelectedMailboxCache<UUID>> selectMailbox(String name, Sort sort, SortType sortType, FilterType filterType) throws BusinessException {
        // consultar el mailbox por nombre
        var mailbox = this.getMailboxByName(name);
        if (mailbox.isError()) {
            return error(mailbox.getErrores());
        }
        return this.selectMailbox(mailbox.ok(), sort, sortType, filterType);
    }

    public Result<SelectedMailboxCache<UUID>> selectMailboxMessages(String name, Sort sort, SortType sortType, FilterType filterType) throws BusinessException {
        // consultar el mailbox por nombre
        var mailbox = this.getMailboxByName(name);
        if (mailbox.isError()) {
            return error(mailbox.getErrores());
        }
        return this.selectMailboxMessages(mailbox.ok(), sort, sortType, filterType);
    }

    public Result<SelectedMailboxCache<UUID>> selectMailbox(MailboxCache<UUID> mailbox, Sort sort, SortType sortType, FilterType filterType) {
        //saber si debo volver a procesar todo el cache
        var selectedMailbox = this.selectedMailboxesByMailboxGid.get(mailbox.getId().toString());
        if (selectedMailbox != null && selectedMailbox.getFilterType().equals(filterType)) {
            // debo refrescar el selected?
            return ok(selectedMailbox);
        }

        try {
            this.lock.lock();

            selectedMailbox = this.selectedMailboxesByMailboxGid.get(mailbox.getId().toString());

            if (selectedMailbox != null && selectedMailbox.getFilterType().equals(filterType)) {
                // debo refrescar el selected?
                return ok(selectedMailbox);
            }


            //filtrar los threas que esten en el mailbox y ordenarlos

            //filtro
            var threadsInMailboxRs = this.mailboxThreads.retrieve(equal(MAILBOXTHREAD_MAILBOXID, mailbox.getId()));

            //los agrego a una lista temporal
            var threads = new ArrayList<OrderedThreadCache<UUID>>();
            var gids = new HashSet<UUID>();

            var threadsUnSeen = 0;
            var totalThreads = 0;

            if (threadsInMailboxRs != null) {
                var it = threadsInMailboxRs.iterator();
                while (it.hasNext()) {
                    var mailboxThreadMessage = it.next();
                    var thread = this.fetchThreadMessageByGid(mailboxThreadMessage.getGid());
                    if (thread != null) {

                        if (thread.isExpunged()) {
                            continue;
                        }

                        //verifico los filter
                        switch (filterType) {
                            case UNSEEN:
                                if ((thread.getFlags() & MessageCache.SEEN) != 0l) {
                                    continue;
                                }
                                break;
                            case FLAGGED:
                                if ((thread.getFlags() & MessageCache.FLAGGED) == 0l) {
                                    continue;
                                }
                                break;
                        }


                        if (!gids.contains(thread.getGid())) {
                            gids.add(thread.getGid());

                            var message = this.getMessageById(thread.getMainMessageGid());

                            var orderedThread = new OrderedThreadCache();

                            // depende del ordenamiento que quiera darle

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

                            //contadores
                            if ((thread.getFlags() & MessageCache.UNSEEN) > 0l) {
                                threadsUnSeen++;
                            }
                            totalThreads++;

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

            selectedMailbox = new SelectedMailboxCache<>(mailbox, first, threadsByGid);
            selectedMailbox.setFilterType(filterType);
            selectedMailbox.setTotal(new AtomicLong(totalThreads));
            selectedMailbox.setUnseen(new AtomicLong(threadsUnSeen));
            this.selectedMailboxesByMailboxGid.put(mailbox.getId().toString(), selectedMailbox);
            return ok(selectedMailbox);
        } finally {
            if (this.lock.isLocked()) {
                this.lock.unlock();
            }
        }
    }

    public void updateMessageFlags(UUID gid, long flags) {
        // obtengo el mensaje,
        var message = this.getMessageById(gid);
        if (message == null) {
            return;
        }
        // actualizo los flags del mensaje,
        message.setFlags(flags);
        // obtengo el thread donde esta el mensaje

        var threadGid = this.threadByMessageGid.get(message.getGid());
        if (threadGid == null) {
            return;
        }

        var threadResponse = this.fetchThreadMessageByGid(threadGid);

        if (threadResponse == null) {
            return;
        }

        syncThreadFlags(threadResponse);

        // remuevo los selected de los mailboxes donde esta el thread
        var threadsInMailboxRs = this.mailboxThreads.retrieve(equal(MAILBOXTHREAD_GID, threadResponse.getGid()));
        if (threadsInMailboxRs != null) {
            var it = threadsInMailboxRs.iterator();
            while (it.hasNext()) {
                this.selectedMailboxesByMailboxGid.remove(it.next().getMailboxGid().toString());
            }
        }
    }


    public ThreadMessageCache<UUID> fetchThreadByMessageGid(UUID gid) {


        // obtengo el thread donde esta el mensaje
        var threadGid = this.threadByMessageGid.get(gid);
        if (threadGid == null) {
            return null;
        }

        var threadResponse = this.fetchThreadMessageByGid(threadGid);

        return threadResponse;
    }

    public void expungeMessage(UUID gid) {
        // obtengo el mensaje,
        var message = this.getMessageById(gid);
        if (message == null) {
            return;
        }
        // actualizo los flags del mensaje,
        message.setExpunged(true);
        // obtengo el thread donde esta el mensaje

        var threadGid = this.threadByMessageGid.get(message.getGid());
        if (threadGid == null) {
            return;
        }

        var threadResponse = this.fetchThreadMessageByGid(threadGid);

        if (threadResponse == null) {
            return;
        }

        syncThreadFlags(threadResponse);

        // remuevo los selected de los mailboxes donde esta el thread
        var threadsInMailboxRs = this.mailboxThreads.retrieve(equal(MAILBOXTHREAD_GID, threadResponse.getGid()));
        if (threadsInMailboxRs != null) {
            var it = threadsInMailboxRs.iterator();
            while (it.hasNext()) {
                this.selectedMailboxesByMailboxGid.remove(it.next().getMailboxGid().toString());
            }
        }
    }

    public void removeMailbox(MailboxCache<UUID> mailbox) {
        var currentMailbox = getMailboxById(mailbox.getId());
        currentMailbox.setRemoved(true);
    }


    public Result<SelectedMailboxCache<UUID>> selectMailboxMessages(MailboxCache<UUID> mailbox, Sort sort, SortType sortType, FilterType filterType) {
        //saber si debo volver a procesar todo el cache
        var selectedMailbox = this.selectedMailboxesByMailboxGid.get(mailbox.getId().toString());
        if (selectedMailbox != null && selectedMailbox.getFilterType().equals(filterType)) {
            // debo refrescar el selected?
            return ok(selectedMailbox);
        }

        try {
            this.lock.lock();

            selectedMailbox = this.selectedMailboxesByMailboxGid.get(mailbox.getId().toString());

            if (selectedMailbox != null && selectedMailbox.getFilterType().equals(filterType)) {
                // debo refrescar el selected?
                return ok(selectedMailbox);
            }


            //filtrar los threas que esten en el mailbox y ordenarlos

            //filtro
            var messagesInMailboxRs = this.messages.retrieve(equal(MESSAGE_MAILBOXGID, mailbox.getId()));

            //los agrego a una lista temporal
            var threads = new ArrayList<OrderedThreadCache<UUID>>();
            var gids = new HashSet<UUID>();

            var threadsUnSeen = 0;
            var totalThreads = 0;

            if (messagesInMailboxRs != null) {
                var it = messagesInMailboxRs.iterator();
                while (it.hasNext()) {
                    var mailboxMessage = it.next();
                    var message = this.getMessageById(mailboxMessage.getGid());
                    if (message != null) {

                        if (message.isExpunged()) {
                            continue;
                        }

                        //verifico los filter
                        switch (filterType) {
                            case UNSEEN:
                                if ((message.getFlags() & MessageCache.SEEN) != 0l) {
                                    continue;
                                }
                                break;
                            case FLAGGED:
                                if ((message.getFlags() & MessageCache.FLAGGED) == 0l) {
                                    continue;
                                }
                                break;
                        }


                        if (!gids.contains(message.getGid())) {
                            gids.add(message.getGid());

                            var orderedThread = new OrderedThreadCache();

                            // depende del ordenamiento que quiera darle

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


                            orderedThread.setMessageGid(message.getGid());
                            orderedThread.setThreadGid(message.getGid());
                            threads.add(orderedThread);

                            //contadores
                            if ((message.getFlags() & MessageCache.UNSEEN) > 0l) {
                                threadsUnSeen++;
                            }
                            totalThreads++;

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

            selectedMailbox = new SelectedMailboxCache<>(mailbox, first, threadsByGid);
            selectedMailbox.setFilterType(filterType);
            selectedMailbox.setTotal(new AtomicLong(totalThreads));
            selectedMailbox.setUnseen(new AtomicLong(threadsUnSeen));
            this.selectedMailboxesByMailboxGid.put(mailbox.getId().toString(), selectedMailbox);
            return ok(selectedMailbox);
        } finally {
            if (this.lock.isLocked()) {
                this.lock.unlock();
            }
        }
    }
}
