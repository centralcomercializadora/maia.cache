package central.mail.cache;

import bee.error.BusinessException;
import bee.result.Result;
import bee.session.ExecutionContext;
import central.mail.cache.model.*;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public interface ICacheFacade {
    Result<List<MailboxCache>> fetchMailboxes(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    void addMailbox(MailboxCache mailbox, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    void addMessage(MessageCache<UUID, UUID> message, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<MailboxCache<UUID>> fetchMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    public Result<Iterator<MailboxMessageCache<UUID, UUID>>> fetchMailboxMessagesInMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<Integer> getThreadsCount(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<Integer> getMessagesCount(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<SelectedMailboxCache<UUID>> selectMailbox(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    void releaseUserCache(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    void recoverUserCache(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    void processThreads(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<ThreadMessageCache<UUID>> fetchThreadMessageByGid(UUID gid, ExecutionContext<UUID, UUID> ec) throws BusinessException;
}
