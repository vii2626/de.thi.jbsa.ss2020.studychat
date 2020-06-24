package de.thi.jbsa.prototype.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import javax.jms.Queue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import de.thi.jbsa.prototype.model.cmd.PostMessageCmd;

@ExtendWith(MockitoExtension.class)
class CommandHandlerServiceTest {

  @InjectMocks
  private CommandHandlerService commandHandlerService;

  @Mock
  private JmsTemplate jmsTemplate;

  @Mock
  private Queue queue;

  @Test
  void testHandleCommand() {
    // given
    PostMessageCmd postMessageCmd = new PostMessageCmd("timmy", "I prefer unit tests for their speed");
    // when
    commandHandlerService.handleCommand(postMessageCmd);
    //when
    verify(jmsTemplate).convertAndSend(eq(queue), eq(postMessageCmd));
  }
}
