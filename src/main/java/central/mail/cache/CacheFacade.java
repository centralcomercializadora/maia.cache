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
    public Result<SelectedMailboxCache<UUID>> selectMailbox(SelectType selectType, String name, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(selectType, name, rc);
    }

    @Override
    public Result<SelectedMailboxCache<UUID>> selectMailbox(SelectType selectType, String name, Sort sort, SortType sortType, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(selectType, name, sort, sortType, rc);
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

    @Override
    public void updateMessageFlags(UUID gid, long flags, RequestCommand ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.updateMessageFlags(gid, flags, ec);
    }

    @Override
    public void expungeMessage(UUID gid, RequestCommand ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.expungeMessage(gid, ec);
    }

    @Override
    public Result<SelectedMailboxCache<UUID>> selectMailbox(SelectType selectType, String name, FilterType filterType, RequestCommand ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(selectType, name, filterType, ec);
    }

    @Override
    public Result<SelectedMailboxCache<UUID>> selectMailbox(SelectType selectType, String name, Sort sort, SortType sortType, FilterType filterType, RequestCommand ec) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.selectMailbox(selectType, name, sort, sortType, filterType, ec);
    }

    @Override
    public Result<Long> getLastRefreshCache(RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.getLastRefreshCache(rc);

    }

    @Override
    public void setLastRefreshCache(Long time, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.setLastRefreshCache(time, rc);
    }

    @Override
    public Result<MailboxCache<UUID>> fetchMailboxById(UUID mailboxId, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchMailboxById(mailboxId, rc);
    }

    @Override
    public ThreadMessageCache<UUID> fetchThreadByMessageGid(UUID gid, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        return business.fetchThreadByMessageGid(gid, rc);
    }

    @Override
    public void restoreFromFile(String file, RequestCommand rc) throws BusinessException{
        var business = Registry.getInstance(ICacheBusiness.class);
        business.restoreFromFile(file, rc);
    }


    @Override
    public void saveToFile(String path, RequestCommand rc) throws BusinessException {
        var business = Registry.getInstance(ICacheBusiness.class);
        business.saveToFile(path, rc);
    }

}
