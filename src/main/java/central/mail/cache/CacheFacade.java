package central.mail.cache;

import bee.error.BusinessException;
import bee.registry.Registry;
import bee.result.Result;
import bee.session.ExecutionContext;
import central.mail.cache.business.ICacheBusiness;
import central.mail.cache.model.*;


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
    public void addMessage(MessageCache<UUID, UUID> message, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.addMessage(message, ec);
    }

    @Override
    public void addMessageNoSync(MessageCache<UUID, UUID> message, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.addMessageNoSync(message, ec);
    }

    @Override
    public Result<MailboxCache<UUID>> fetchMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMailboxByName(name, ec);
    }

    @Override
    public Result<Iterator<MessageCache<UUID, UUID>>> fetchMessagesInMailboxByName(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMessagesInMailboxByName(name, ec);
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
    public Result<SelectedMailboxCache<UUID>> selectMailbox(String name, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(name, ec);
    }

    @Override
    public Result<SelectedMailboxCache<UUID>> selectMailbox(String name, Sort sort, SortType sortType, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(name, sort, sortType, ec);
    }

    @Override
    public void releaseUserCache(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.releaseUserCache(ec);
    }

    @Override
    public void recoverUserCache(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.recoverUserCache(ec);
    }

    @Override
    public synchronized void processThreads(ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.processThreads(ec);
    }

    @Override
    public Result<ThreadMessageCache<UUID>> fetchThreadMessageByGid(UUID gid, ExecutionContext<UUID, UUID> ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchThreadMessageByGid(gid, ec);
    }

    @Override
    public Result<MessageCache<UUID,UUID>> fetchMessageByGid(UUID messageGid, ExecutionContext<UUID, UUID> ec) throws BusinessException{
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMessageByGid(messageGid, ec);
    }
}
