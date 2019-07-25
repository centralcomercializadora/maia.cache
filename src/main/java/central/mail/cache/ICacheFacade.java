package central.mail.cache;

import central.mail.cache.model.*;
import cognitivesolutions.error.BusinessException;
import cognitivesolutions.result.Result;
import cognitivesolutions.session.RequestCommand;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface ICacheFacade {
    Result<List<MailboxCache<UUID>>> fetchMailboxes(RequestCommand ec) throws BusinessException;

    void addMailbox(MailboxCache mailbox, RequestCommand ec) throws BusinessException;

    void removeMailbox(MailboxCache<UUID> mailbox, RequestCommand rc) throws BusinessException;

    void addMessage(MessageCache<UUID, UUID> message, RequestCommand ec) throws BusinessException;

    void addMessageNoSync(MessageCache<UUID, UUID> message, RequestCommand ec) throws BusinessException;

    Result<MailboxCache<UUID>> fetchMailboxByName(String name, RequestCommand ec) throws BusinessException;

    public Result<Iterator<MessageCache<UUID, UUID>>> fetchMessagesInMailboxByName(String name, RequestCommand ec) throws BusinessException;

    Result<Integer> getThreadsCount(RequestCommand ec) throws BusinessException;

    Result<Integer> getMessagesCount(RequestCommand ec) throws BusinessException;

    Result<SelectedMailboxCache<UUID>> selectMailbox(SelectType selectType, String name, RequestCommand rc) throws BusinessException;

    Result<SelectedMailboxCache<UUID>> selectMailbox(SelectType selectType, String name, Sort sort, SortType sortType, RequestCommand rc) throws BusinessException;

    void releaseUserCache(RequestCommand ec) throws BusinessException;

    void recoverUserCache(RequestCommand ec) throws BusinessException;

    void processThreads(RequestCommand ec) throws BusinessException;

    Result<ThreadMessageCache<UUID>> fetchThreadMessageByGid(UUID gid, RequestCommand ec) throws BusinessException;

    Result<MessageCache<UUID, UUID>> fetchMessageByGid(UUID messageGid, RequestCommand ec) throws BusinessException;

    MessageCache<UUID, UUID> fetchMessageByMailboxIdAndUid(UUID mailboxGid, Long uid, RequestCommand ec) throws BusinessException;

    void updateMessageFlags(UUID gid, long flags, RequestCommand rc) throws BusinessException;

    void expungeMessage(UUID gid, RequestCommand rc) throws BusinessException;

    Result<SelectedMailboxCache<UUID>> selectMailbox(SelectType selectType, String name, FilterType filterType, RequestCommand ec) throws BusinessException;

    Result<SelectedMailboxCache<UUID>> selectMailbox(SelectType selectType, String name, Sort sort, SortType sortType, FilterType filterType, RequestCommand ec) throws BusinessException;

    Result<Long> getLastRefreshCache(RequestCommand rc) throws BusinessException;

    void setLastRefreshCache(Long time, RequestCommand rc) throws BusinessException;

    Result<Long> getLastSyncCache(RequestCommand rc) throws BusinessException;

    void setLastSyncCache(Long time, RequestCommand rc) throws BusinessException;

    Result<Boolean> isCacheLoaded(RequestCommand rc) throws BusinessException;

    void setCacheLoaded(boolean loaded, RequestCommand rc) throws BusinessException;

    Result<MailboxCache<UUID>> fetchMailboxById(UUID mailboxId, RequestCommand rc) throws BusinessException;

    ThreadMessageCache<UUID> fetchThreadByMessageGid(UUID gid, RequestCommand rc) throws BusinessException;

    void restoreFromFile(String file, RequestCommand rc) throws BusinessException;

    void saveToFile(String path, RequestCommand rc) throws BusinessException;

    ReentrantReadWriteLock.ReadLock lockForRead(RequestCommand rc) throws BusinessException;

    void releaseReadLock(ReentrantReadWriteLock.ReadLock lock,RequestCommand rc) throws BusinessException;

    ReentrantReadWriteLock.WriteLock lockForWrite(RequestCommand rc) throws BusinessException;

    void releaseWriteLock(ReentrantReadWriteLock.WriteLock lock,RequestCommand rc) throws BusinessException;
}
