package de.thi.jbsa.prototype.service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.jms.Topic;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import de.thi.jbsa.prototype.domain.MessageDoc;
import de.thi.jbsa.prototype.model.event.MentionEvent;
import de.thi.jbsa.prototype.model.event.MessagePostedEvent;
import de.thi.jbsa.prototype.model.event.MessageRepeatedEvent;
import de.thi.jbsa.prototype.model.model.Message;
import de.thi.jbsa.prototype.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageService {

  private final JmsTemplate jmsTemplate;

  private final MessageRepository messageRepository;

  private final Topic topic;

  public MessageService(MessageRepository messageRepository, JmsTemplate jmsTemplate, Topic topic) {
    this.messageRepository = messageRepository;
    this.jmsTemplate = jmsTemplate;
    this.topic = topic;
  }

  private Message createMsg(MessagePostedEvent event) {
    Message msg = new Message();
    msg.setCmdUuid(event.getCmdUuid());
    msg.setContent(event.getContent());
    msg.setCreated(new Date());
    msg.setEntityId(event.getEntityId());
    msg.setEventUuid(event.getUuid());
    msg.setSenderUserId(event.getUserId());
    return msg;
  }

  public List<Message> getlast10Messages() {
    return messageRepository.findFirst10ByOrderByMessage_CreatedDesc()
                            .stream()
                            .map(MessageDoc::getMessage)
                            .sorted(Comparator.comparingLong(Message::getEntityId))
                            .collect(Collectors.toList());
  }

  public void handleMentionEvent(MentionEvent event) {
    // This is a temporary event. We don't need that in the read-db.
    jmsTemplate.convertAndSend(topic, event);
  }

  public void handleMessagePostedEvent(MessagePostedEvent event) {
    MessageDoc doc = new MessageDoc(createMsg(event));
    messageRepository.save(doc);
    jmsTemplate.convertAndSend(topic, event);
  }

  public void handleMessageRepeatedEvent(MessageRepeatedEvent event) {
    Optional<MessageDoc> existingMessage = messageRepository.findFirstByMessage_EventUuidOrderByMessage_CreatedDesc(event.getOriginalMessageUUID());
    if (!existingMessage.isPresent()){
      log.error("Didn't find message to increase the counter");
      return;
    }
    log.debug("Message with UUID {} occurred for the {} times", existingMessage.get().getMessage().getEventUuid(), event.getOccurCount());
    existingMessage.get().getMessage().setOccurCount(event.getOccurCount());

    messageRepository.save(existingMessage.get());
    jmsTemplate.convertAndSend(topic, event);
  }
}
