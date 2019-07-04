package central.mail.cache.business;

import bee.error.BusinessException;
import bee.result.Result;
import bee.session.ExecutionContext;
import central.mail.cache.model.MailboxCache;
import central.mail.cache.model.MailboxMessageCache;
import central.mail.cache.model.MessageCache;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public interface ICacheBusiness {
    void init() throws BusinessException;

    Result<List<MailboxCache>> fetchMailboxes(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<MailboxCache<UUID>> fetchMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    void addMailbox(MailboxCache mailbox, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    void addMessage(UUID mailboxId, MessageCache<UUID, UUID> message, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<Iterator<MailboxMessageCache<UUID, UUID>>> fetchMailboxMessagesInMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<Integer> getThreadsCount(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<Integer> getMessagesCount(ExecutionContext<UUID, UUID> ec) throws BusinessException;

    Result<MailboxCache<UUID>> selectMailbox(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException;
}
