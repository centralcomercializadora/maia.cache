package central.mail.cache;


import bee.configuracion.Configuracion;
import bee.registry.Registry;
import bee.result.Result;
import bee.serviceregistry.Modules;
import bee.session.ExecutionContext;
import central.mail.cache.model.*;
import central.mail.store.impl.business.StoreBusiness;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CacheTest {
    private static ICacheFacade facade;
    private static ExecutionContext rc = new ExecutionContext();

    private static final MimeConfig MIME_ENTITY_CONFIG = MimeConfig.custom()
            .setMaxContentLen(-1)
            .setMaxHeaderCount(-1)
            .setMaxHeaderLen(-1)
            .setMaxHeaderCount(-1)
            .setMaxLineLen(-1)
            .build();

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
        var message = new MessageCache<UUID, UUID>();
        var mailbox = this.makeMailbox("inbox");

        facade.addMailbox(mailbox, rc);

        message.setGid(UUID.randomUUID());

        message.setMailboxGid(mailbox.getId());
        facade.addMessage(message, rc);

        var res = (Iterator<MailboxMessageCache<UUID, UUID>>) facade.fetchMailboxMessagesInMailboxByName("inbox", rc).ok();

        assert (res.hasNext());

        assert (message.getGid().equals(res.next().getMessageGid()));

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

        var message1 = new MessageCache<UUID, UUID>();
        message1.setGid(UUID.randomUUID());
        message1.setMailboxGid(mailbox1.getId());

        var message2 = new MessageCache<UUID, UUID>();
        message2.setGid(UUID.randomUUID());
        message2.setMailboxGid(mailbox2.getId());

        facade.addMessage(message1, rc);

        facade.addMessage(message2, rc);


        var res = (Iterator<MailboxMessageCache<UUID, UUID>>) facade.fetchMailboxMessagesInMailboxByName("inbox", rc).ok();

        assert (res.hasNext());

        assert (message1.getGid().equals(res.next().getMessageGid()));

        assert (!res.hasNext());


        res = (Iterator<MailboxMessageCache<UUID, UUID>>) facade.fetchMailboxMessagesInMailboxByName("inbox2", rc).ok();

        assert (res.hasNext());

        assert (message2.getGid().equals(res.next().getMessageGid()));

        assert (!res.hasNext());


        assert (facade.fetchMailboxMessagesInMailboxByName("inbox3", rc).isError());

        res = (Iterator<MailboxMessageCache<UUID, UUID>>) facade.fetchMailboxMessagesInMailboxByName("inbox4", rc).ok();

        assert (!res.hasNext());


    }

    @Test
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
            var message = new MessageCache<UUID, UUID>();
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

    @Test
    public void selectMailboxWithMails() throws Exception {
        this.recoverUserCache();
        var i = 1;
        long total = System.currentTimeMillis();
        while (i < 1000) {
            i++;
            long now = System.currentTimeMillis();
            facade.selectMailbox("inbox", rc);
            System.out.println("en: " + (System.currentTimeMillis() - now) + "ms");
        }

        System.out.println("total en: " + (System.currentTimeMillis() - total) + "ms");

    }

    @Test
    public void selectMailboxWithMailsOrdered() throws Exception {
        this.recoverUserCache();
        var i = 1;
        long total = System.currentTimeMillis();
        facade.selectMailbox("inbox", rc);
    }

    @Test
    public void releaseUserCache() throws Exception {
        this.loadMessages();
        facade.releaseUserCache(rc);
    }

    @Test
    public void recoverUserCache() throws Exception {
        facade.recoverUserCache(rc);
    }

    /**
     * Tres mensajes sin relacion
     *
     * @throws Exception
     */

    @Test
    public void threadsTestCase1() throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = new MessageCache<UUID, UUID>();
        m1.setMailboxGid(mailbox1.getId());
        m1.setGid(UUID.randomUUID());
        m1.setMessageId("m1");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = new MessageCache<UUID, UUID>();
        m2.setMailboxGid(mailbox1.getId());
        m2.setGid(UUID.randomUUID());
        m2.setMessageId("m2");
        m2.setMessageDate(System.currentTimeMillis());

        var m3 = new MessageCache<UUID, UUID>();
        m3.setMailboxGid(mailbox1.getId());
        m3.setGid(UUID.randomUUID());
        m3.setMessageId("m3");
        m3.setMessageDate(System.currentTimeMillis());


        facade.addMessage(m1, rc);
        facade.addMessage(m2, rc);
        facade.addMessage(m3, rc);
        facade.processThreads(rc);

        var threads = facade.selectMailbox("inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 3);

    }


    /**
     * Dos mensajes, un solo hilo, el mensaje 2 tiene en inreplyto al mensaje 1
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase2() throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = new MessageCache<UUID, UUID>();
        m1.setMailboxGid(mailbox1.getId());
        m1.setGid(UUID.randomUUID());
        m1.setMessageId("m1");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = new MessageCache<UUID, UUID>();
        m2.setMailboxGid(mailbox1.getId());
        m2.setGid(UUID.randomUUID());
        m2.setMessageId("m2");
        m2.setInReplyTo("m1");
        m2.setMessageDate(System.currentTimeMillis());

        facade.addMessage(m1, rc);
        facade.addMessage(m2, rc);
        facade.processThreads(rc);

        var threads = facade.selectMailbox("inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }

    /**
     * Tres mensajes, dos hilos, el mensaje 2 tiene en inreplyto al mensaje 1, mensaje 3 indpendiente
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase3() throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = new MessageCache<UUID, UUID>();
        m1.setMailboxGid(mailbox1.getId());
        m1.setGid(UUID.randomUUID());
        m1.setMessageId("m1");
        m1.setMessageDate(1l);

        var m2 = new MessageCache<UUID, UUID>();
        m2.setMailboxGid(mailbox1.getId());
        m2.setGid(UUID.randomUUID());
        m2.setMessageId("m2");
        m2.setInReplyTo("m1");
        m2.setMessageDate(2l);

        var m3 = new MessageCache<UUID, UUID>();
        m3.setMailboxGid(mailbox1.getId());
        m3.setGid(UUID.randomUUID());
        m3.setMessageId("m3");
        m3.setMessageDate(3l);


        facade.addMessage(m1, rc);
        facade.addMessage(m2, rc);
        facade.addMessage(m3, rc);
        facade.processThreads(rc);

        var threads = facade.selectMailbox("inbox", rc);
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
     * Dos mensajes, un solo hilo, el mensaje 1 tiene en inreplyto al mensaje 2
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase4() throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = new MessageCache<UUID, UUID>();
        m1.setMailboxGid(mailbox1.getId());
        m1.setGid(UUID.randomUUID());
        m1.setMessageId("m1");
        m1.setInReplyTo("m2");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = new MessageCache<UUID, UUID>();
        m2.setMailboxGid(mailbox1.getId());
        m2.setGid(UUID.randomUUID());
        m2.setMessageId("m2");
        m2.setMessageDate(System.currentTimeMillis());

        facade.addMessage(m1, rc);
        facade.addMessage(m2, rc);
        facade.processThreads(rc);

        var threads = facade.selectMailbox("inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size()== 1);

    }

    /**
     * Tres mensajes un solo hilo, el mensaje 1 tiene referencia a 2, el mensaje 2 tiene referencia a 3
     *
     * @throws Exception
     */

    @Test
    public void threadsTestCase5() throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = new MessageCache<UUID, UUID>();
        m1.setMailboxGid(mailbox1.getId());
        m1.setGid(UUID.randomUUID());
        m1.setMessageId("m1");
        m1.setInReplyTo("m2");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = new MessageCache<UUID, UUID>();
        m2.setMailboxGid(mailbox1.getId());
        m2.setGid(UUID.randomUUID());
        m2.setMessageId("m2");
        m2.setInReplyTo("m3");
        m2.setMessageDate(System.currentTimeMillis());

        var m3 = new MessageCache<UUID, UUID>();
        m3.setMailboxGid(mailbox1.getId());
        m3.setGid(UUID.randomUUID());
        m3.setMessageId("m3");
        m3.setMessageDate(System.currentTimeMillis());


        facade.addMessage(m1, rc);
        facade.addMessage(m2, rc);
        facade.addMessage(m3, rc);
        facade.processThreads(rc);

        var threads = facade.selectMailbox("inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }

    /**
     * Tres mensajes un solo hilo, el mensaje 1 tiene referencia a 2, el mensaje 3 tiene referencia a 2
     *
     * @throws Exception
     */

    @Test
    public void threadsTestCase6() throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = new MessageCache<UUID, UUID>();
        m1.setMailboxGid(mailbox1.getId());
        m1.setGid(UUID.randomUUID());
        m1.setMessageId("m1");
        m1.setInReplyTo("m2");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = new MessageCache<UUID, UUID>();
        m2.setMailboxGid(mailbox1.getId());
        m2.setGid(UUID.randomUUID());
        m2.setMessageId("m2");
        m2.setMessageDate(System.currentTimeMillis());

        var m3 = new MessageCache<UUID, UUID>();
        m3.setMailboxGid(mailbox1.getId());
        m3.setGid(UUID.randomUUID());
        m3.setMessageId("m3");
        m3.setInReplyTo("m2");
        m3.setMessageDate(System.currentTimeMillis());


        facade.addMessage(m1, rc);
        facade.addMessage(m2, rc);
        facade.addMessage(m3, rc);
        facade.processThreads(rc);

        var threads = facade.selectMailbox("inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }
    /**
     * Tres mensajes dos hilos, el mensaje 1 esta solo, el mensaje 2 esta solo , el mensaje 3 tiene referencia a 2
     *
     * @throws Exception
     */
    @Test
    public void threadsTestCase7() throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = new MessageCache<UUID, UUID>();
        m1.setMailboxGid(mailbox1.getId());
        m1.setGid(UUID.randomUUID());
        m1.setMessageId("m1");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = new MessageCache<UUID, UUID>();
        m2.setMailboxGid(mailbox1.getId());
        m2.setGid(UUID.randomUUID());
        m2.setMessageId("m2");
        m2.setMessageDate(System.currentTimeMillis());

        var m3 = new MessageCache<UUID, UUID>();
        m3.setMailboxGid(mailbox1.getId());
        m3.setGid(UUID.randomUUID());
        m3.setMessageId("m3");
        m3.setInReplyTo("m2");
        m3.setMessageDate(System.currentTimeMillis());


        facade.addMessage(m1, rc);
        facade.addMessage(m2, rc);
        facade.addMessage(m3, rc);
        facade.processThreads(rc);

        var threads = facade.selectMailbox("inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 2);

    }



    @Test
    public void threadsTestCase8() throws Exception {
        var mailbox1 = this.makeMailbox("inbox");
        facade.addMailbox(mailbox1, rc);

        var m1 = new MessageCache<UUID, UUID>();
        m1.setMailboxGid(mailbox1.getId());
        m1.setGid(UUID.randomUUID());
        m1.setMessageId("m1");
        m1.setInReplyTo("noexiste");
        m1.setReferences("noexiste");
        m1.setMessageDate(System.currentTimeMillis());

        var m2 = new MessageCache<UUID, UUID>();
        m2.setMailboxGid(mailbox1.getId());
        m2.setGid(UUID.randomUUID());
        m2.setMessageId("m2");
        m2.setInReplyTo("noexiste");
        m2.setReferences("noexiste");
        m2.setMessageDate(System.currentTimeMillis());

        var m3 = new MessageCache<UUID, UUID>();
        m3.setMailboxGid(mailbox1.getId());
        m3.setGid(UUID.randomUUID());
        m3.setMessageId("m3");
        m3.setInReplyTo("m2");
        m3.setReferences("m1 m2 noexiste");
        m3.setMessageDate(System.currentTimeMillis());


        facade.addMessage(m1, rc);
        facade.addMessage(m2, rc);
        facade.addMessage(m3, rc);
        facade.processThreads(rc);

        var threads = facade.selectMailbox("inbox", rc);
        assert (((SelectedMailboxCache<UUID>) threads.ok()).getThreadsByGid().size() == 1);

    }
}
