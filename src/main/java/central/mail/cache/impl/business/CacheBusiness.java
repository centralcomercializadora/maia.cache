package central.mail.cache.impl.business;

import bee.configuration.IConfiguration;
import bee.error.BusinessException;
import bee.error.IExceptionHandler;
import bee.result.Result;
import bee.session.ExecutionContext;
import central.mail.cache.business.ICacheBusiness;
import central.mail.cache.model.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static bee.result.Result.ok;
import static central.mail.cache.model.SortType.ASC;
import static central.mail.cache.model.SortType.DESC;


@Singleton
public class CacheBusiness implements ICacheBusiness {

    private static final Logger logger = LoggerFactory.getLogger(CacheBusiness.class);
    private final IExceptionHandler exceptionHandler;
    private final IConfiguration configuration;

    private Map<UUID, UserCache> cache;

    private static final ReentrantLock generalLock = new ReentrantLock();
    private static final Map<UUID, ReentrantLock> cacheLocks = new ConcurrentHashMap<>();

    @Inject
    public CacheBusiness(IExceptionHandler exceptionHandler, IConfiguration configuration) {
        this.exceptionHandler = exceptionHandler;
        this.configuration = configuration;
    }


    @Override
    public void init() throws BusinessException {

        try {
            if (cache == null) {
                cache = new ConcurrentHashMap<>();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ReentrantLock getLock(ExecutionContext rc) throws BusinessException {
        ReentrantLock res = this.cacheLocks.get(rc.getUserGuid());
        if (res == null) {
            try {
                this.generalLock.lockInterruptibly();
                res = this.cacheLocks.get(rc.getUserGuid());
                if (res == null) {
                    res = new ReentrantLock();
                    this.cacheLocks.put((UUID) rc.getUserGuid(), res);
                }
            } catch (InterruptedException e) {
                this.exceptionHandler.handleErrorAsBusinessException(CacheBusiness.class, e, rc);
            } finally {
                if (this.generalLock.isLocked()) {
                    this.generalLock.unlock();
                }
            }
        }

        return res;
    }

    private UserCache getUserCache(boolean create, boolean refresh, ExecutionContext<UUID, UUID> requestCommand) throws BusinessException {
        Long start = (System.currentTimeMillis());
        UserCache res = null;

        try {
            res = this.cache.get(requestCommand.getUserGuid());
        } catch (Exception e) {
            e.printStackTrace();
            this.cache.remove(requestCommand.getUserGuid());
        }


        if (res == null && create) {
            ReentrantLock lock = this.cacheLocks.get(requestCommand.getUserGuid());
            if (lock == null) {
                try {
                    logger.debug("{}: Lock General", requestCommand.getUserGuid());
                    generalLock.lock();
                    lock = this.cacheLocks.get(requestCommand.getUserGuid());
                    if (lock == null) {
                        lock = new ReentrantLock();
                        this.cacheLocks.put(requestCommand.getUserGuid(), lock);
                    }
                } finally {
                    if (generalLock.isLocked()) {
                        generalLock.unlock();
                        logger.debug("{}: UnLock General", requestCommand.getUserGuid());
                    }

                }
            }


            try {
                logger.debug("{}: Lock User", requestCommand.getUserGuid());
                lock.lock();
                try {
                    res = this.cache.get(requestCommand.getUserGuid());
                } catch (Exception e) {
                    e.printStackTrace();
                    this.cache.remove(requestCommand.getUserGuid());
                }

                if (res == null) {
                    res = new UserCache();
                    this.cache.put(requestCommand.getUserGuid(), res);
                }


            } finally {
                if (lock.isLocked()) {
                    lock.unlock();
                    logger.debug("{}: UnLock User", requestCommand.getUserGuid());
                }
            }


        }

        //logger.debug("{}: Get User Cache Response in {} ms", requestCommand.getUserGuid(), (System.currentTimeMillis() - start));

        return res;
    }

    public void forceRefresh(ExecutionContext<UUID, UUID> requestCommand) throws BusinessException {

        ReentrantLock lock = null;

        Long start = System.currentTimeMillis();

        try {

            lock = this.getLock(requestCommand);
            lock.lock();
            UserCache userCache = this.getUserCache(true, false, requestCommand);
            userCache.setLastRefresh(0l);
            this.cache.put(requestCommand.getUserGuid(), userCache);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock != null && lock.isLocked()) {
                lock.unlock();
            }
        }

    }

    @Override
    public Result<List<MailboxCache>> fetchMailboxes(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return ok(userCache.getMailboxes());
    }

    @Override
    public Result<MailboxCache<UUID>> fetchMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return userCache.getMailboxByName(name);
    }

    @Override
    public void addMailbox(MailboxCache mailbox, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        userCache.addMailbox(mailbox);
    }

    @Override
    public void addMessage(MessageCache<UUID, UUID> message, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        userCache.add(message);
    }

    @Override
    public void addMessageNoSync(MessageCache<UUID, UUID> message, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        userCache.addNoSync(message);
    }

    @Override
    public synchronized void processThreads(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        userCache.processThreads();
    }


    @Override
    public Result<Iterator<MessageCache<UUID, UUID>>> fetchMessagesInMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return userCache.fetchMessagesInMailboxByName(name, ec);
    }

    @Override
    public Result<Integer> getThreadsCount(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return ok(userCache.getThreadsCount());
    }

    @Override
    public Result<Integer> getMessagesCount(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return ok(userCache.getMessagesCount());
    }

    @Override
    public Result<SelectedMailboxCache<UUID>> selectMailbox(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return userCache.selectMailbox(name, Sort.DATE, DESC);
    }

    @Override
    public Result<SelectedMailboxCache<UUID>> selectMailbox(String name, Sort sort, SortType sortType, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return userCache.selectMailbox(name, sort, sortType);
    }

    @Override
    public Result<ThreadMessageCache<UUID>> fetchThreadMessageByGid(UUID gid, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return ok(userCache.fetchThreadMessageByGid(gid));
    }

    @Override
    public void releaseUserCache(ExecutionContext<UUID, UUID> ec) throws BusinessException {

        // debe estar en lock pq se elimina la data del usuario del mapa de usuarios

        var file = new File("./data/file");

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {

            if (!file.exists()) {
                file.createNewFile();
            }

            fos = new FileOutputStream(file, false);
            oos = new ObjectOutputStream(fos);

            oos.writeObject(this.getUserCache(true, false, ec).toStore());

            oos.close();
            fos.close();

            oos = null;
            fos = null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //
    }

    @Override
    public void recoverUserCache(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        // debe estar en lock pq se elimina la data del usuario del mapa de usuarios

        var file = new File("./data/file");

        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {

            if (file.exists()) {
                file.createNewFile();
            }

            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);


            UserCache userCache = new UserCache();
            userCache.init((UserCacheStore) ois.readObject());
            userCache.processThreads();
            this.cache.put(ec.getUserGuid(), userCache);

            ois.close();
            fis.close();

            ois = null;
            fis = null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //
    }

    @Override
    public Result<MessageCache<UUID, UUID>> fetchMessageByGid(UUID messageGid, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return ok(userCache.getMessageById(messageGid));
    }

    @Override
    public MessageCache<UUID, UUID> fetchMessageByMailboxIdAndUid(UUID mailboxGid, Long uid, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return userCache.getMessageByMailboxIdAndUid(mailboxGid, uid);
    }
}



