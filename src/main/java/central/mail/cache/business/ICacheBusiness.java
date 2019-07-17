package central.mail.cache.business;


import central.mail.cache.model.*;
import cognitivesolutions.error.BusinessException;
import cognitivesolutions.result.Result;
import cognitivesolutions.session.RequestCommand;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public interface ICacheBusiness {
    void init() throws BusinessException;

    Result<List<MailboxCache<UUID>>> fetchMailboxes(RequestCommand ec) throws BusinessException;

    Result<MailboxCache<UUID>> fetchMailboxByName(String name, RequestCommand ec) throws BusinessException;

    void addMailbox(MailboxCache mailbox, RequestCommand ec) throws BusinessException;

    void addMessage(MessageCache<UUID, UUID> message, RequestCommand ec) throws BusinessException;

    void addMessageNoSync(MessageCache<UUID, UUID> message, RequestCommand ec) throws BusinessException;

    void processThreads(RequestCommand ec) throws BusinessException;

    Result<Iterator<MessageCache<UUID, UUID>>> fetchMessagesInMailboxByName(String name, RequestCommand ec) throws BusinessException;

    Result<Integer> getThreadsCount(RequestCommand ec) throws BusinessException;

    Result<Integer> getMessagesCount(RequestCommand ec) throws BusinessException;

    Result<SelectedMailboxCache<UUID>> selectMailbox(String name, RequestCommand ec) throws BusinessException;

    Result<SelectedMailboxCache<UUID>> selectMailbox(String name, Sort sort, SortType sortType, RequestCommand ec) throws BusinessException;

    Result<ThreadMessageCache<UUID>> fetchThreadMessageByGid(UUID gid, RequestCommand ec) throws BusinessException;

    void releaseUserCache(RequestCommand ec) throws BusinessException;

    void recoverUserCache(RequestCommand ec) throws BusinessException;

    Result<MessageCache<UUID, UUID>> fetchMessageByGid(UUID messageGid, RequestCommand ec) throws BusinessException;

    MessageCache<UUID, UUID> fetchMessageByMailboxIdAndUid(UUID mailboxGid, Long uid, RequestCommand ec) throws BusinessException;

    void removeMailbox(MailboxCache<UUID> mailbox, RequestCommand rc) throws BusinessException;
}
