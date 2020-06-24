package de.thi.jbsa.prototype.aop;

import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Objects;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;
import de.thi.jbsa.prototype.model.cmd.PostMessageCmd;
import de.thi.jbsa.prototype.model.event.AbstractEvent;
import de.thi.jbsa.prototype.model.event.MessagePostedEvent;
import de.thi.jbsa.prototype.service.MessageProcessorService;

/**
 * Created by Balazs Endredi <balazs.endredi@beskgroup.com> on 06.05.2020
 */
@SpringBootTest
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class DomainAspectsITest {

  @Qualifier("eventQueue")
  @Autowired
  private Queue eventQueue;

  @Autowired
  private JmsTemplate jmsTemplate;

  @Autowired
  private MessageProcessorService messageProcessorService;

  @Test
  void testCensoredMessagePosted()
    throws Exception {
    // given
    PostMessageCmd cmd = new PostMessageCmd("user-id", "I'm a JBSA student!");

    // when
    messageProcessorService.postMessage(cmd);

    // then
    MessageConsumer consumer = getMessageConsumerForQueue();
    // here we have a timeout, because we have to wait for the service to finish processing and the event to arrive on the queue
    // The timeout should be as low as possible to avoid waiting too long for a failure,
    // but high enough to minimize the risk of false positives (search for "flaky tests").
    ActiveMQObjectMessage receivedMessage = (ActiveMQObjectMessage) consumer.receive(1000);
    assertNotNull(receivedMessage);
    AbstractEvent eventFromQueue = (AbstractEvent) receivedMessage.getObject();

    assertThat(eventFromQueue, Matchers.isA(MessagePostedEvent.class));
    Assert.assertEquals("I'm a <not allowed term> student!", ((MessagePostedEvent) eventFromQueue).getContent());
  }

  private MessageConsumer getMessageConsumerForQueue()
    throws JMSException {
    Connection testConnection = Objects.requireNonNull(jmsTemplate.getConnectionFactory()).createConnection();
    testConnection.start();
    Session session = testConnection.createSession();
    return session.createConsumer(eventQueue);
  }
}
