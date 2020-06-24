package de.thi.jbsa.prototype.service;

import javax.jms.Queue;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import de.thi.jbsa.prototype.model.cmd.Cmd;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 * @author Christopher Timm <christopher.timm@beskgroup.com> on 2020-02-18
 */
@Service
@Log
@RequiredArgsConstructor
public class CommandHandlerService {

  private final JmsTemplate jmsTemplate;

  private final Queue queue;

  public void handleCommand(Cmd cmd) {
    jmsTemplate.convertAndSend(queue, cmd);
    log.info("Sent cmd to queue " + cmd);
  }
}
