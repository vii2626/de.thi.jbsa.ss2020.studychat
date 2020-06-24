package de.thi.jbsa.prototype.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Objects;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;
import de.thi.jbsa.prototype.model.cmd.PostMessageCmd;

@SpringBootTest
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class CommandHandlerServiceITest {

  @Autowired
  private CommandHandlerService commandHandlerService;

  @Autowired
  private JmsTemplate jmsTemplate;

  @Autowired
  private Queue queue;

  private MessageConsumer getMessageConsumerForQueue()
    throws JMSException {
    Connection testConnection = Objects.requireNonNull(jmsTemplate.getConnectionFactory()).createConnection();
    testConnection.start();
    Session session = testConnection.createSession();
    return session.createConsumer(queue);
  }

  @Test
  void handleCommand()
    throws Exception {
    // given
    MessageConsumer consumer = getMessageConsumerForQueue();
    // when
    PostMessageCmd postedMessage = new PostMessageCmd("timmy", "but I also use integration tests for being closer to production");
    commandHandlerService.handleCommand(postedMessage);
    // then
    final ActiveMQObjectMessage message = (ActiveMQObjectMessage) consumer.receive(1000);
    assertNotNull(message);
    PostMessageCmd commendFromQueue = (PostMessageCmd) message.getObject();
    assertEquals(postedMessage, commendFromQueue);
  }
}
