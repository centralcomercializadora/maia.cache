package central.mail.cache;

import bee.error.BusinessException;
import bee.registry.Registry;
import bee.result.Result;
import bee.session.ExecutionContext;
import central.mail.cache.business.ICacheBusiness;
import central.mail.cache.model.MailboxCache;
import central.mail.cache.model.MailboxMessageCache;
import central.mail.cache.model.MessageCache;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static bee.result.Result.ok;

public class CacheFacade implements ICacheFacade {
    @Override
    public Result<List<MailboxCache>> fetchMailboxes(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMailboxes(ec);
    }

    @Override
    public void addMailbox(MailboxCache mailbox, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.addMailbox(mailbox, ec);
    }

    @Override
    public void addMessage(UUID mailboxId, MessageCache<UUID, UUID> message, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.addMessage(mailboxId, message, ec);
    }

    @Override
    public Result<MailboxCache<UUID>> fetchMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMailboxByName(name, ec);
    }

    @Override
    public Result<Iterator<MailboxMessageCache<UUID, UUID>>> fetchMailboxMessagesInMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMailboxMessagesInMailboxByName(name, ec);
    }

    @Override
    public Result<Integer> getThreadsCount(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.getThreadsCount(ec);
    }

    @Override
    public Result<Integer> getMessagesCount(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.getMessagesCount(ec);
    }

    @Override
    public Result<MailboxCache<UUID>> selectMailbox(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(name, ec);
    }
}
