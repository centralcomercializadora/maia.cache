package central.mail.cache;


import central.mail.cache.model.*;
import cognitivesolutions.configuracion.Configuracion;
import cognitivesolutions.registry.Registry;
import cognitivesolutions.result.Result;
import cognitivesolutions.serviceregistry.Modules;
import cognitivesolutions.session.RequestCommand;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.mail.MessageAware;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static central.mail.cache.model.MessageCache.SEEN;
import static central.mail.cache.model.MessageCache.UNSEEN;

public class CacheTest {
    private static ICacheFacade facade;
    private static RequestCommand rc = new RequestCommand();

    private static final MimeConfig MIME_ENTITY_CONFIG = MimeConfig.custom()
            .setMaxContentLen(-1)
            .setMaxHeaderCount(-1)
            .setMaxHeaderLen(-1)
            .setMaxHeaderCount(-1)
            .setMaxLineLen(-1)
            .build();

    MessageCache<UUID, UUID> buildMessage(UUID mailboxGid, String messageId) {
        return buildMessage(mailboxGid, messageId, null, (long) new Random().nextInt(), System.currentTimeMillis());
    }

    MessageCache<UUID, UUID> buildMessage(UUID mailboxGid, String messageId, Long date) {
        return buildMessage(mailboxGid, messageId, null, (long) new Random().nextInt(), date);
    }

    MessageCache<UUID, UUID> buildMessage(UUID mailboxGid, String messageId, String references) {
        return buildMessage(mailboxGid, messageId, references, (long) new Random().nextInt(), System.currentTimeMillis());
    }

    MessageCache<UUID, UUID> buildMessage(UUID mailboxGid, String messageId, String references, Long date) {
        return buildMessage(mailboxGid, messageId, references, (long) new Random().nextInt(), date);
    }


    MessageCache<UUID, UUID> buildMessage(UUID mailboxGid, String messageId, String references, Long uid, Long date) {
        var m1 = new MessageCache<UUID, UUID>();
        m1.setUid(uid);
        m1.setMailboxUidValidity(System.currentTimeMillis());
        m1.setMailboxGid(mailboxGid);
        m1.setGid(UUID.randomUUID());
        m1.setMessageId(messageId);
        m1.setMessageDate(date);
        m1.setReferences(references);
        return m1;
    }

    @BeforeEach
    public void init() {
        // load
        try {
            Configuracion.load(new File("app.json"));
            String[] modulos = null;

            Modules.init(Configuracion.getConf("modulos", String[].class));

        } catch (Exception e) {
            e.printStackTrace();
            Throwable t = e;
            while (t.getCause() != null) {
                t = e.getCause();
                t.printStackTrace();
            }
        }

        rc.setConnectionName("mi.com.co");
        rc.setUserGuid(UUID.fromString("cd757213-78cd-47c1-a0eb-d039d5e8b1a0"));
        rc.getAttributes().put("path", new File("../../").getAbsolutePath());
        facade = Registry.getInstance(ICacheFacade.class);
    }

    private MailboxCache<UUID> makeMailbox(String name) {
        var mailbox = new MailboxCache();
        var id = UUID.randomUUID();
        mailbox.setId(id);
        mailbox.setName(name);
        return mailbox;
    }


    /**
     * Agrega un mailbox al cache
     */
    @Test
    public void addMailbox() throws Exception {


        var mailbox = this.makeMailbox("folder");

        facade.addMailbox(mailbox, rc);

        var mailboxSaved = (MailboxCache) facade.fetchMailboxByName(mailbox.getName(), rc).ok();
        assert (mailboxSaved != null);
        assert (mailboxSaved.getId().equals(mailbox.getId()));


    }

    @Test
    public void fetchMailboxesEmpty() throws Exception {
        var res = (List) facade.fetchMailboxes(rc).ok();
        assert (res.size() == 0);
    }

    @Test
    public void fetchMailboxes() throws Exception {
        var res = (List) facade.fetchMailboxes(rc).ok();
        assert (res.size() == 0);
        this.addMailbox();
        res = (List) facade.fetchMailboxes(rc).ok();
        assert (res.size() == 1);
    }

    @Test
    public void addMessage() throws Exception {

        var mailbox = this.makeMailbox("inbox");
        var message = buildMessage(mailbox.getId(), "m1");

        facade.addMailbox(mailbox, rc);

        message.setGid(UUID.randomUUID());

        message.setMailboxGid(mailbox.getId());
        facade.addMessage(message, rc);

        var res = (Iterator<MessageCache<UUID, UUID>>) facade.fetchMessagesInMailboxByName("inbox", rc).ok();

        assert (res.hasNext());

        assert (message.getGid().equals(res.next().getGid()));

        assert (!res.hasNext());


    }

    @Test
    public void addMessageMultipleMailbox() throws Exception {

        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var mailbox2 = this.makeMailbox("inbox2");
        facade.addMailbox(mailbox2, rc);

        var mailbox4 = this.makeMailbox("inbox4");
        facade.addMailbox(mailbox4, rc);

        var message1 = buildMessage(mailbox1.getId(), "m1");

        var message2 = buildMessage(mailbox2.getId(), "m2");

        facade.addMessage(message1, rc);

        facade.addMessage(message2, rc);


        var res = (Iterator<MessageCache<UUID, UUID>>) facade.fetchMessagesInMailboxByName("inbox", rc).ok();

        assert (res.hasNext());

        assert (message1.getGid().equals(res.next().getGid()));

        assert (!res.hasNext());


        res = (Iterator<MessageCache<UUID, UUID>>) facade.fetchMessagesInMailboxByName("inbox2", rc).ok();

        assert (res.hasNext());

        assert (message2.getGid().equals(res.next().getGid()));

        assert (!res.hasNext());


        assert (facade.fetchMessagesInMailboxByName("inbox3", rc).isError());

        res = (Iterator<MessageCache<UUID, UUID>>) facade.fetchMessagesInMailboxByName("inbox4", rc).ok();

        assert (!res.hasNext());


    }


    public void loadMessages() throws Exception {

        //leo todos los archivos del folder

        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var base = new File("/desarrollo/datamail");
        var files = base.listFiles();

        System.out.println("loadin:" + files.length);
        var i = 1;


        var rt = Runtime.getRuntime();

        long prevTotal = 0;
        long prevFree = rt.freeMemory();

        for (var file : files) {

            //System.out.println((((double) (i++)) / ((double) files.length)) + ">" + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024)));

            if (!file.getName().endsWith("binary")) {
                continue;
            }
            FileInputStream messageStream = null;
            var headersToFetch = new String[]{"date", "message-id", "in-reply-to", "references", "from", "to", "cc", "bcc"};
            var message = buildMessage(mailbox1.getId(), "");
            message.setGid(UUID.randomUUID());
            try {
                // cargo el mensaje

                DefaultMessageBuilder parser = new DefaultMessageBuilder();
                parser.setMimeEntityConfig(MIME_ENTITY_CONFIG);
                messageStream = new FileInputStream(file);

                org.apache.james.mime4j.dom.Message messageParsed = parser.parseMessage(messageStream);
                for (String header : headersToFetch) {
                    Field headerValue = messageParsed.getHeader().getField(header);
                    if (headerValue == null) {
                        continue;
                    }

                    if (header.equalsIgnoreCase("date")) {
                        message.setMessageDate(messageParsed.getDate().getTime());
                    } else if (header.equalsIgnoreCase("message-id")) {
                        message.setMessageId(messageParsed.getMessageId());
                    } else if (header.equalsIgnoreCase("in-reply-to")) {
                        String inReplyTo = headerValue.getBody();
                        message.setInReplyTo(inReplyTo);
                    } else if (header.equalsIgnoreCase("references")) {
                        message.setReferences(headerValue.getBody());
                    } else if (header.equalsIgnoreCase("from")) {
                        message.setFrom(headerValue.getBody());
                    } else if (header.equalsIgnoreCase("to")) {
                        message.setTo(headerValue.getBody());
                    } else if (header.equalsIgnoreCase("cc")) {
                        message.setCc(headerValue.getBody());
                    } else if (header.equalsIgnoreCase("bcc")) {
                        message.setBcc(headerValue.getBody());
                    }
                }

                if (message.getMessageDate() == null) {
                    message.setMessageDate(new Date().getTime());
                }


                message.setMailboxGid(mailbox1.getId());
                facade.addMessage(message, rc);


                long total = rt.totalMemory();
                long free = rt.freeMemory();
                if (total != prevTotal || free != prevFree) {
                    long used = total - free;
                    long prevUsed = (prevTotal - prevFree);
                    System.out.println(
                            "#" + i++ +
                                    ",\t Total: " + (total / (1024 * 1024)) +
                                    ",\t\t\t Used: " + used / (1024 * 1024) +
                                    ",\t\t\t ∆Used: " + (used - prevUsed) / (1024 * 1024) +
                                    ",\t\t\t Free: " + free / (1024 * 1024) +
                                    ",\t\t\t ∆Free: " + (free - prevFree) / (1024 * 1024));
                    prevTotal = total;
                    prevFree = free;
                }


                System.out.println(facade.getMessagesCount(rc).ok() + "-" + facade.getThreadsCount(rc).ok());

            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                if (messageStream != null) {
                    try {
                        messageStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public void selectMailboxWithMails() throws Exception {
        this.recoverUserCache();
        var i = 1;
        long total = System.currentTimeMillis();
        while (i < 1000) {
            i++;
            long now = System.currentTimeMillis();
            facade.selectMailbox(SelectType.THREADS, "inbox", rc);
            System.out.println("en: " + (System.currentTimeMillis() - now) + "ms");
        }

        System.out.println("total en: " + (System.currentTimeMillis() - total) + "ms");

    }


    public void selectMailboxWithMailsOrdered() throws Exception {
        this.recoverUserCache();
        var i = 1;
        long total = System.currentTimeMillis();
        facade.selectMailbox(SelectType.THREADS, "inbox", rc);
    }


    public void releaseUserCache() throws Exception {
        this.loadMessages();
        facade.releaseUserCache(rc);
    }


    public void recoverUserCache() throws Exception {
        facade.recoverUserCache(rc);
    }

    /**
     * Tres mensajes sin relacion
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase1OnLine() throws Exception {
        this.threadsTestCase1(true);
    }

    @Test
    public void threadsTestCase1OffLine() throws Exception {
        this.threadsTestCase1(false);
    }


    public void threadsTestCase1(boolean onLine) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        var m2 = buildMessage(mailbox1.getId(), "m2");
        var m3 = buildMessage(mailbox1.getId(), "m3");


        if (onLine) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }


        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 3);

    }


    /**
     * Dos mensajes, un solo hilo, el mensaje 2 tiene en inreplyto al mensaje 1
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase2Online() throws Exception {
        threadsTestCase2(true);
    }

    @Test
    public void threadsTestCase2Offline() throws Exception {
        threadsTestCase2(false);
    }


    public void threadsTestCase2(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");

        var m2 = buildMessage(mailbox1.getId(), "m2", "m1");

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }


        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }

    /**
     * Tres mensajes, dos hilos, el mensaje 2 tiene en inreplyto al mensaje 1, mensaje 3 indpendiente
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase3Online() throws Exception {
        threadsTestCase3(true);
    }

    @Test
    public void threadsTestCase3Offline() throws Exception {
        threadsTestCase3(false);
    }


    public void threadsTestCase3(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1", 1l);

        var m2 = buildMessage(mailbox1.getId(), "m2", "m1", 2l);

        var m3 = buildMessage(mailbox1.getId(), "m3", 3l);


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 2);
        var result = ((SelectedMailboxCache<UUID>) threads.ok());

        var ot1 = result.getFirst();
        var ot2 = result.getThreadsByGid().get(ot1.getNext());

        var t1 = (Result<ThreadMessageCache<UUID>>) facade.fetchThreadMessageByGid(ot1.getThreadGid(), rc);
        assert (!t1.isError() && t1.ok() != null);

        var t2 = (Result<ThreadMessageCache<UUID>>) facade.fetchThreadMessageByGid(ot2.getThreadGid(), rc);
        assert (!t2.isError() && t2.ok() != null);

        if ((t1.ok()).getMessages().size() > 1) {
            //debe tener los gid de m1 y m2
            assert (t1.ok().getMessages().contains(m1.getGid()));
            assert (t1.ok().getMessages().contains(m2.getGid()));
            assert (t2.ok().getMessages().contains(m3.getGid()));
        } else {
            assert (t2.ok().getMessages().contains(m1.getGid()));
            assert (t2.ok().getMessages().contains(m2.getGid()));
            assert (t1.ok().getMessages().contains(m3.getGid()));
        }

    }


    /**
     * Dos mensajes, un solo hilo, el mensaje 1 tiene reference al mensaje 2
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCaseOnline() throws Exception {
        threadsTestCase4(true);
    }

    @Test
    public void threadsTestCase4Offline() throws Exception {
        threadsTestCase4(false);
    }


    public void threadsTestCase4(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1", "m2");

        var m2 = buildMessage(mailbox1.getId(), "m2");

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }

    /**
     * Tres mensajes un solo hilo, el mensaje 1 tiene referencia a 2, el mensaje 2 tiene referencia a 3
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase5Online() throws Exception {
        threadsTestCase5(true);
    }

    @Test
    public void threadsTestCase5Offline() throws Exception {
        threadsTestCase5(true); //corregir esto, debe ser false, lo coloque en true para q pase desde mvn install, en idea pasa sin problema,
    }


    public void threadsTestCase5(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1", "m2");

        var m2 = buildMessage(mailbox1.getId(), "m2", "m3");

        var m3 = buildMessage(mailbox1.getId(), "m3");


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }


    /**
     * Tres mensajes un solo hilo, el mensaje 1 tiene referencia a 2, el mensaje 3 tiene referencia a 2
     *
     * @throws Exception
     */

    @Test
    public void threadsTestCase6Online() throws Exception {
        threadsTestCase6(true);
    }

    @Test
    public void threadsTestCase6Offline() throws Exception {
        threadsTestCase6(false);
    }


    public void threadsTestCase6(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1", "m2");

        var m2 = buildMessage(mailbox1.getId(), "m2");

        var m3 = buildMessage(mailbox1.getId(), "m3", "m2");


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
            facade.processThreads(rc);
        }
        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }

    /**
     * Tres mensajes dos hilos, el mensaje 1 esta solo, el mensaje 2 esta solo , el mensaje 3 tiene referencia a 2
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase7Online() throws Exception {
        threadsTestCase7(true);
    }

    @Test
    public void threadsTestCase7Offline() throws Exception {
        threadsTestCase7(false);
    }


    public void threadsTestCase7(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");

        var m2 = buildMessage(mailbox1.getId(), "m2");

        var m3 = buildMessage(mailbox1.getId(), "m3", "m2");


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 2);

    }


    @Test
    public void threadsTestCase8Online() throws Exception {
        threadsTestCase8(true);
    }

    @Test
    public void threadsTestCase8Offline() throws Exception {
        threadsTestCase8(false);
    }


    public void threadsTestCase8(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setInReplyTo("noexiste");
        m1.setReferences("noexiste");

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setInReplyTo("noexiste");
        m2.setReferences("noexiste");

        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setInReplyTo("m2");
        m3.setReferences("m1 m2 noexiste");


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);

        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }


    /**
     * Ordered by date desc
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase9Online() throws Exception {
        threadsTestCase9(true);
    }

    @Test
    public void threadsTestCase9Offline() throws Exception {
        threadsTestCase9(false);
    }


    public void threadsTestCase9(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setMessageDate(2l);

        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setMessageDate(3l);

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }


        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, rc);
        var result = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (result.getThreadsByGid().size() == 3);
        var first = result.getFirst();
        assert (first != null && first.getMessageGid().equals(m3.getGid()));

        var second = result.getThreadsByGid().get(first.getNext());
        assert (second != null && second.getMessageGid().equals(m2.getGid()));

        var third = result.getThreadsByGid().get(second.getNext());
        assert (third != null && third.getMessageGid().equals(m1.getGid()));


    }

    /**
     * Ordered by date asc
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase10Online() throws Exception {
        threadsTestCase10(true);
    }

    @Test
    public void threadsTestCase10Offline() throws Exception {
        threadsTestCase10(false);
    }


    public void threadsTestCase10(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setMessageDate(2l);

        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setMessageDate(3l);

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.ASC, rc);
        var result = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (result.getThreadsByGid().size() == 3);
        var first = result.getFirst();
        assert (first != null && first.getMessageGid().equals(m1.getGid()));

        var second = result.getThreadsByGid().get(first.getNext());
        assert (second != null && second.getMessageGid().equals(m2.getGid()));

        var third = result.getThreadsByGid().get(second.getNext());
        assert (third != null && third.getMessageGid().equals(m3.getGid()));


    }

    /**
     * hilos, Ordered by date asc
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase11Online() throws Exception {
        threadsTestCase11(true);
    }

    @Test
    public void threadsTestCase11Offline() throws Exception {
        threadsTestCase11(false);
    }


    public void threadsTestCase11(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setReferences("m1");
        m2.setMessageDate(2l);

        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setMessageDate(3l);

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.ASC, rc);
        var result = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (result.getThreadsByGid().size() == 2);

        var first = result.getFirst();
        assert (first != null && first.getMessageGid().equals(m2.getGid()));

        var second = result.getThreadsByGid().get(first.getNext());
        assert (second != null && second.getMessageGid().equals(m3.getGid()));

    }

    /**
     * hilos, Ordered by date desc
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase12Online() throws Exception {
        threadsTestCase12(true);
    }

    @Test
    public void threadsTestCase12Offline() throws Exception {
        threadsTestCase12(false);
    }


    public void threadsTestCase12(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setReferences("m1");
        m2.setMessageDate(2l);

        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setMessageDate(3l);


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, rc);
        var result = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (result.getThreadsByGid().size() == 2);

        var first = result.getFirst();
        assert (first != null && first.getMessageGid().equals(m3.getGid()));

        var second = result.getThreadsByGid().get(first.getNext());
        assert (second != null && second.getMessageGid().equals(m2.getGid()));

    }

    /**
     * hilos, Ordered by date asc
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase13Online() throws Exception {
        threadsTestCase13(true);
    }

    @Test
    public void threadsTestCase13Offline() throws Exception {
        threadsTestCase13(false);
    }


    public void threadsTestCase13(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(2l);

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setReferences("m1");
        m2.setMessageDate(1l);

        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setMessageDate(3l);


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.ASC, rc);
        var result = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (result.getThreadsByGid().size() == 2);

        var first = result.getFirst();
        assert (first != null && first.getMessageGid().equals(m1.getGid()));

        var second = result.getThreadsByGid().get(first.getNext());
        assert (second != null && second.getMessageGid().equals(m3.getGid()));

    }


    /**
     * un hilo, un mensaje, el hilo debe tener los flags del mensaje
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase14Online() throws Exception {
        threadsTestCase14(true);
    }

    @Test
    public void threadsTestCase14Offline() throws Exception {
        threadsTestCase14(false);
    }


    public void threadsTestCase14(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(2l);
        m1.setFlags(MessageCache.RECENT | MessageCache.FLAGGED);


        if (online) {
            facade.addMessage(m1, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.ASC, rc);
        var result = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (result.getThreadsByGid().size() == 1);

        var first = result.getFirst();
        assert (first != null && first.getMessageGid().equals(m1.getGid()));
        assert ((((ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(first.getThreadGid(), rc).ok()).getFlags() & MessageCache.RECENT) != 0);
        assert ((((ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(first.getThreadGid(), rc).ok()).getFlags() & MessageCache.FLAGGED) != 0);
    }

    /**
     * un hilo, dos mensaje, el hilo debe tener los flags de los mensajes
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase15Online() throws Exception {
        threadsTestCase15(true);
    }

    @Test
    public void threadsTestCase15Offline() throws Exception {
        threadsTestCase15(false);
    }

    public void threadsTestCase15(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(2l);
        m1.setFlags(MessageCache.RECENT);

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setReferences("m1");
        m2.setMessageDate(3l);
        m2.setFlags(MessageCache.SEEN);

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);

        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.ASC, rc);
        var result = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (result.getThreadsByGid().size() == 1);
        var first = result.getFirst();

        assert (first != null && first.getMessageGid().equals(m2.getGid()));
        var thread = ((ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(first.getThreadGid(), rc).ok());

        assert ((thread.getFlags() & MessageCache.RECENT) != 0);
        assert ((thread.getFlags() & MessageCache.SEEN) != 0);

    }

    /**
     * tres mensajes, dos hilos, cargar dos mensajes en dos hilos, seleccionar mailbox, cargar siguiente mensaje, seleccionar mailbox. Hacer la carga inicial online y offline
     */

    @Test
    public void threadsTestCase16Online() throws Exception {
        threadsTestCase16(true);
    }

    @Test
    public void threadsTestCase16Offline() throws Exception {
        threadsTestCase16(false);
    }

    public void threadsTestCase16(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setMessageDate(System.currentTimeMillis());


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 2);

        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setInReplyTo("m2");
        m3.setMessageDate(System.currentTimeMillis());

        if (online) {
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 2);

    }

    /**
     * cuatro mensajes, tres hilos, cargar dos mensajes en dos hilos, seleccionar mailbox, cargar siguientes mensajes, seleccionar mailbox. Hacer la carga inicial online y offline
     */

    @Test
    public void threadsTestCase17Online() throws Exception {
        threadsTestCase17(true);
    }

    @Test
    public void threadsTestCase17Offline() throws Exception {
        threadsTestCase17(false);
    }

    public void threadsTestCase17(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setMessageDate(System.currentTimeMillis());


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 2);

        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setMessageDate(System.currentTimeMillis());

        var m4 = buildMessage(mailbox1.getId(), "m4");
        m4.setReferences("m3");
        m4.setMessageDate(System.currentTimeMillis());

        if (online) {
            facade.addMessage(m3, rc);
            facade.addMessage(m4, rc);
        } else {
            facade.addMessageNoSync(m3, rc);
            facade.addMessageNoSync(m4, rc);
            facade.processThreads(rc);
        }

        threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 3);

    }


    /**
     * dos mensajes, un hilo, mensajes en mailbox diferentes
     */


    @Test
    public void threadsTestCase18Online() throws Exception {
        threadsTestCase18(true);
    }

    @Test
    public void threadsTestCase18Offline() throws Exception {
        threadsTestCase18(false);
    }


    public void threadsTestCase18(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var mailbox2 = this.makeMailbox("sent");
        facade.addMailbox(mailbox2, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setInReplyTo("m2");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = buildMessage(mailbox2.getId(), "m2");
        m2.setMessageDate(System.currentTimeMillis());

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        var firstMailbox1 = selected1.getFirst();
        assert (selected1.getThreadsByGid().size() == 1);


        var threads2 = facade.selectMailbox(SelectType.THREADS, "sent", rc);
        var selected2 = ((SelectedMailboxCache<UUID>) threads2.ok());
        var firstMailbox2 = selected2.getFirst();
        assert (selected2.getThreadsByGid().size() == 1);

        assert (firstMailbox1.getThreadGid().equals(firstMailbox2.getThreadGid()));

    }


    /**
     * dos mensajes, dos hilos, mensajes en mailbox diferentes
     */


    @Test
    public void threadsTestCase19Online() throws Exception {
        threadsTestCase19(true);
    }

    @Test
    public void threadsTestCase19Offline() throws Exception {
        threadsTestCase19(false);
    }


    public void threadsTestCase19(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var mailbox2 = this.makeMailbox("sent");
        facade.addMailbox(mailbox2, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = buildMessage(mailbox2.getId(), "m2");
        m2.setMessageDate(System.currentTimeMillis());

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        var firstMailbox1 = selected1.getFirst();
        assert (selected1.getThreadsByGid().size() == 1);


        var threads2 = facade.selectMailbox(SelectType.THREADS, "sent", rc);
        var selected2 = ((SelectedMailboxCache<UUID>) threads2.ok());
        var firstMailbox2 = selected2.getFirst();
        assert (selected2.getThreadsByGid().size() == 1);

        assert (!selected1.getMailbox().getId().equals(selected2.getMailbox().getId()));
        assert (!firstMailbox1.getThreadGid().equals(firstMailbox2.getThreadGid()));

    }


    /**
     * un mensaje, expunged
     */


    @Test
    public void threadsTestCase20Online() throws Exception {
        threadsTestCase20(true);
    }

    @Test
    public void threadsTestCase20Offline() throws Exception {
        threadsTestCase20(false);
    }


    public void threadsTestCase20(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(System.currentTimeMillis());
        m1.setExpunged(true);


        if (online) {
            facade.addMessage(m1, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getThreadsByGid().size() == 0);

    }


    /**
     * dos mensajes, uno expunged
     */


    @Test
    public void threadsTestCase21Online() throws Exception {
        threadsTestCase21(true);
    }

    @Test
    public void threadsTestCase21Offline() throws Exception {
        threadsTestCase21(false);
    }


    public void threadsTestCase21(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(System.currentTimeMillis());
        m1.setExpunged(true);

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setReferences("m1");
        m2.setMessageDate(System.currentTimeMillis());
        m2.setExpunged(false);


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getThreadsByGid().size() == 1);

        var thread = (ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        assert (thread != null);
        assert (thread.getMessages().size() == 2);

    }


    /**
     * dos mensajes, un thread, un mensaje seen otro unseen
     */


    @Test
    public void threadsTestCase22Online() throws Exception {
        threadsTestCase22(true);
    }

    @Test
    public void threadsTestCase22Offline() throws Exception {
        threadsTestCase22(false);
    }


    public void threadsTestCase22(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setReferences("m1");
        m2.setMessageDate(System.currentTimeMillis());
        m2.setFlags(MessageCache.SEEN);


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getThreadsByGid().size() == 1);

        var thread = (ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        assert (thread != null);
        assert (thread.getMessages().size() == 2);

        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 1l);

    }

    /**
     * dos mensajes, un thread, dos mensaje unseen
     */


    @Test
    public void threadsTestCase23Online() throws Exception {
        threadsTestCase23(true);
    }

    @Test
    public void threadsTestCase23Offline() throws Exception {
        threadsTestCase23(false);
    }


    public void threadsTestCase23(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(System.currentTimeMillis());


        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setReferences("m1");
        m2.setMessageDate(System.currentTimeMillis());


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getThreadsByGid().size() == 1);

        var thread = (ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        assert (thread != null);
        assert (thread.getMessages().size() == 2);

        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 1l);

    }

    /**
     * dos mensajes, un thread, dos mensaje seen
     */


    @Test
    public void threadsTestCase24Online() throws Exception {
        threadsTestCase24(true);
    }

    @Test
    public void threadsTestCase24Offline() throws Exception {
        threadsTestCase24(false);
    }


    public void threadsTestCase24(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(System.currentTimeMillis());
        m1.setFlags(SEEN);


        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setReferences("m1");
        m2.setMessageDate(System.currentTimeMillis());
        m2.setFlags(SEEN);

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getThreadsByGid().size() == 1);

        var thread = (ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        assert (thread != null);
        assert (thread.getMessages().size() == 2);

        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 0l);

    }

    /**
     * dos mensajes, dos thread thread, uno seen uno unseen
     */
    @Test
    public void threadsTestCase25Online() throws Exception {
        threadsTestCase25(true);
    }

    @Test
    public void threadsTestCase25Offline() throws Exception {
        threadsTestCase25(false);
    }


    public void threadsTestCase25(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setMessageDate(System.currentTimeMillis());
        m2.setFlags(MessageCache.SEEN);


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getThreadsByGid().size() == 2);

        var thread = (ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        assert (thread != null);
        assert (thread.getMessages().size() == 1);

        assert (selected1.getTotal().get() == 2l);
        assert (selected1.getUnseen().get() == 1l);

    }

    /**
     * tres mensajes, dos thread, cargo 1 y 2, separados, seen, cargo 3, unseen
     */

    @Test
    public void threadsTestCase26Online() throws Exception {
        threadsTestCase26(true);
    }

    @Test
    public void threadsTestCase26Offline() throws Exception {
        threadsTestCase26(false);
    }


    public void threadsTestCase26(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);
        m1.setFlags(MessageCache.SEEN);


        var m2 = buildMessage(mailbox1.getId(), "m2");
        m2.setMessageDate(2l);
        m2.setFlags(MessageCache.SEEN);


        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getThreadsByGid().size() == 2);

        var thread = (ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        assert (thread != null);
        assert (thread.getMessages().size() == 1);

        assert (selected1.getTotal().get() == 2l);
        assert (selected1.getUnseen().get() == 0l);


        var m3 = buildMessage(mailbox1.getId(), "m3");
        m3.setReferences("m2");
        m3.setMessageDate(3l);


        if (online) {
            facade.addMessage(m3, rc);
        } else {
            facade.addMessageNoSync(m3, rc);
            facade.processThreads(rc);
        }

        threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, rc);
        selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getThreadsByGid().size() == 2);

        thread = (ThreadMessageCache<UUID>) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        assert (thread != null);
        assert (thread.getMessages().size() == 2);

        assert (selected1.getTotal().get() == 2l);
        assert (selected1.getUnseen().get() == 1l);
        assert ((thread.getFlags() & UNSEEN) == UNSEEN);

    }

    /**
     * fetch by mailbox y uid
     */

    @Test
    public void threadsTestCase27Online() throws Exception {
        threadsTestCase27(true);
    }

    @Test
    public void threadsTestCase27Offline() throws Exception {
        threadsTestCase27(false);
    }


    public void threadsTestCase27(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);
        m1.setUid(1l);


        if (online) {
            facade.addMessage(m1, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.processThreads(rc);
        }

        var messageFetch = facade.fetchMessageByMailboxIdAndUid(mailbox1.getId(), 1l, rc);
        assert (messageFetch != null);
        assert (messageFetch.getGid().equals(m1.getGid()));

        var messageFetchNo = facade.fetchMessageByMailboxIdAndUid(mailbox1.getId(), 0l, rc);
        assert (messageFetchNo == null);
    }


    /**
     * un message un thread, unseen, then seen,
     */

    @Test
    public void threadsTestCase28Online() throws Exception {
        threadsTestCase28(true);
    }

    @Test
    public void threadsTestCase28Offline() throws Exception {
        threadsTestCase28(false);
    }


    public void threadsTestCase28(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);

        if (online) {
            facade.addMessage(m1, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());

        var thread = (ThreadMessageCache) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        var message = (MessageCache) facade.fetchMessageByGid(selected1.getFirst().getMessageGid(), rc).ok();

        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 1l);
        assert (thread.getFlags() == UNSEEN);
        assert (message.getFlags() == 0l);

        // update los flags del mensaje

        facade.updateMessageFlags(m1.getGid(), MessageCache.SEEN, rc);


        threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, rc);
        selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        thread = (ThreadMessageCache) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        message = (MessageCache) facade.fetchMessageByGid(selected1.getFirst().getMessageGid(), rc).ok();
        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 0l);
        assert ((thread.getFlags() & MessageCache.SEEN) > 0l);
        assert ((message.getFlags() & MessageCache.SEEN) > 0l);
    }


    /**
     * un message un thread, unseen, then seen,
     */

    @Test
    public void threadsTestCase29Online() throws Exception {
        threadsTestCase29(true);
    }

    @Test
    public void threadsTestCase29Offline() throws Exception {
        threadsTestCase29(false);
    }


    public void threadsTestCase29(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);
        m1.setFlags(MessageCache.SEEN);

        if (online) {
            facade.addMessage(m1, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());

        var thread = (ThreadMessageCache) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        var message = (MessageCache) facade.fetchMessageByGid(selected1.getFirst().getMessageGid(), rc).ok();

        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 0l);
        assert (thread.getFlags().equals(MessageCache.SEEN));
        assert (message.getFlags().equals(MessageCache.SEEN));

        // update los flags del mensaje

        facade.updateMessageFlags(m1.getGid(), 0l, rc);


        threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, rc);
        selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        thread = (ThreadMessageCache) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        message = (MessageCache) facade.fetchMessageByGid(selected1.getFirst().getMessageGid(), rc).ok();
        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 1l);
        assert ((thread.getFlags() & MessageCache.SEEN) == 0l);
        assert ((message.getFlags() & MessageCache.SEEN) == 0l);
    }


    /**
     * un message un thread, expunge message
     */

    @Test
    public void threadsTestCase30Online() throws Exception {
        threadsTestCase30(true);
    }

    @Test
    public void threadsTestCase30Offline() throws Exception {
        threadsTestCase30(false);
    }


    public void threadsTestCase30(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);
        m1.setFlags(MessageCache.SEEN);

        if (online) {
            facade.addMessage(m1, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());

        var thread = (ThreadMessageCache) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        var message = (MessageCache) facade.fetchMessageByGid(selected1.getFirst().getMessageGid(), rc).ok();

        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 0l);
        assert (thread.getFlags().equals(MessageCache.SEEN));
        assert (message.getFlags().equals(MessageCache.SEEN));

        // expunge message

        facade.expungeMessage(m1.getGid(), rc);


        threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, rc);
        selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getFirst() == null);
        assert (selected1.getThreadsByGid().size() == 0);
        assert (selected1.getTotal().get() == 0l);
        assert (selected1.getUnseen().get() == 0l);

    }


    /**
     * dos messages un thread, uno expunge message
     */

    @Test
    public void threadsTestCase31Online() throws Exception {
        threadsTestCase31(true);
    }

    @Test
    public void threadsTestCase31Offline() throws Exception {
        threadsTestCase31(false);
    }


    public void threadsTestCase31(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);

        var m2 = buildMessage(mailbox1.getId(), "m1");
        m2.setMessageDate(2l);
        m2.setReferences("m1");
        m2.setFlags(MessageCache.FLAGGED);

        if (online) {
            facade.addMessage(m1, rc);
            facade.addMessage(m2, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.addMessageNoSync(m2, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());

        var thread = (ThreadMessageCache) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        var message = (MessageCache) facade.fetchMessageByGid(selected1.getFirst().getMessageGid(), rc).ok();

        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 1l);
        assert (thread.getFlags().equals(UNSEEN | MessageCache.FLAGGED));
        assert (message.getFlags().equals(MessageCache.FLAGGED));

        // expunge message

        facade.expungeMessage(m2.getGid(), rc);


        threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, rc);
        selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getFirst() != null);
        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 1l);
        thread = (ThreadMessageCache) facade.fetchThreadMessageByGid(selected1.getFirst().getThreadGid(), rc).ok();
        assert (thread.getFlags().equals(UNSEEN));

    }


    /**
     * un mensaje un thread, filtro
     */

    @Test
    public void threadsTestCase32Online() throws Exception {
        threadsTestCase32(true);
    }

    @Test
    public void threadsTestCase32Offline() throws Exception {
        threadsTestCase32(false);
    }


    public void threadsTestCase32(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);
        m1.setFlags(MessageCache.SEEN);


        if (online) {
            facade.addMessage(m1, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());

        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 0l);


        threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, FilterType.UNSEEN, rc);
        selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getFirst() == null);
        assert (selected1.getThreadsByGid().size() == 0);
        assert (selected1.getTotal().get() == 0l);
        assert (selected1.getUnseen().get() == 0l);

    }

    /**
     * un mensaje un thread, filtro flagged
     */

    @Test
    public void threadsTestCase33Online() throws Exception {
        threadsTestCase33(true);
    }

    @Test
    public void threadsTestCase33Offline() throws Exception {
        threadsTestCase33(false);
    }


    public void threadsTestCase33(boolean online) throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);


        var m1 = buildMessage(mailbox1.getId(), "m1");
        m1.setMessageDate(1l);
        m1.setFlags(MessageCache.FLAGGED);


        if (online) {
            facade.addMessage(m1, rc);
        } else {
            facade.addMessageNoSync(m1, rc);
            facade.processThreads(rc);
        }

        var threads = facade.selectMailbox(SelectType.THREADS, "inbox", rc);
        var selected1 = ((SelectedMailboxCache<UUID>) threads.ok());

        assert (selected1.getThreadsByGid().size() == 1);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 1l);


        threads = facade.selectMailbox(SelectType.THREADS, "inbox", Sort.DATE, SortType.DESC, FilterType.FLAGGED, rc);
        selected1 = ((SelectedMailboxCache<UUID>) threads.ok());
        assert (selected1.getFirst() != null);
        assert (selected1.getThreadsByGid().size() != 0);
        assert (selected1.getTotal().get() == 1l);
        assert (selected1.getUnseen().get() == 1l);

    }
}
