package central.mail.cache.impl.business;

import bee.configuration.IConfiguration;
import bee.error.BusinessException;
import bee.error.IExceptionHandler;
import bee.result.Result;
import bee.session.ExecutionContext;
import central.mail.cache.business.ICacheBusiness;
import central.mail.cache.model.MailboxCache;
import central.mail.cache.model.MailboxMessageCache;
import central.mail.cache.model.MessageCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static bee.result.Result.ok;


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
    public void addMessage(UUID mailboxId, MessageCache<UUID, UUID> message, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        userCache.add(mailboxId, message);
    }


    @Override
    public Result<Iterator<MailboxMessageCache<UUID, UUID>>> fetchMailboxMessagesInMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return userCache.fetchMailboxMessagesInMailboxByName(name, ec);
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
    public Result<MailboxCache<UUID>> selectMailbox(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var userCache = this.getUserCache(true, false, ec);
        return userCache.selectMailbox(name);
    }
}



