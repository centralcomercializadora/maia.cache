package central.mail.cache;

import central.mail.cache.business.ICacheBusiness;
import central.mail.cache.model.*;
import cognitivesolutions.error.BusinessException;
import cognitivesolutions.registry.Registry;
import cognitivesolutions.result.Result;
import cognitivesolutions.session.RequestCommand;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;



public class CacheFacade implements ICacheFacade {
    @Override
    public Result<List<MailboxCache<UUID>>> fetchMailboxes(RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMailboxes(rc);
    }

    @Override
    public void addMailbox(MailboxCache mailbox, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.addMailbox(mailbox, rc);
    }

    @Override
    public void removeMailbox(MailboxCache<UUID> mailbox, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.removeMailbox(mailbox, rc);
    }

    @Override
    public void addMessage(MessageCache<UUID, UUID> message, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.addMessage(message, rc);
    }

    @Override
    public void addMessageNoSync(MessageCache<UUID, UUID> message, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.addMessageNoSync(message, rc);
    }

    @Override
    public Result<MailboxCache<UUID>> fetchMailboxByName(String name, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMailboxByName(name, rc);
    }

    @Override
    public Result<Iterator<MessageCache<UUID, UUID>>> fetchMessagesInMailboxByName(String name, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMessagesInMailboxByName(name, rc);
    }

    @Override
    public Result<Integer> getThreadsCount(RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.getThreadsCount(rc);
    }

    @Override
    public Result<Integer> getMessagesCount(RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.getMessagesCount(rc);
    }

    @Override
    public Result<SelectedMailboxCache<UUID>> selectMailbox(String name, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(name, rc);
    }

    @Override
    public Result<SelectedMailboxCache<UUID>> selectMailbox(String name, Sort sort, SortType sortType, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(name, sort, sortType, rc);
    }

    @Override
    public void releaseUserCache(RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.releaseUserCache(rc);
    }

    @Override
    public void recoverUserCache(RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.recoverUserCache(rc);
    }

    @Override
    public synchronized void processThreads(RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.processThreads(rc);
    }

    @Override
    public Result<ThreadMessageCache<UUID>> fetchThreadMessageByGid(UUID gid, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchThreadMessageByGid(gid, rc);
    }

    @Override
    public Result<MessageCache<UUID, UUID>> fetchMessageByGid(UUID messageGid, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMessageByGid(messageGid, rc);
    }

    @Override
    public MessageCache<UUID, UUID> fetchMessageByMailboxIdAndUid(UUID mailboxGid, Long uid, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMessageByMailboxIdAndUid(mailboxGid, uid, rc);
    }
}
