package de.thi.jbsa.prototype.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jms.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.thi.jbsa.prototype.aop.Censored;
import de.thi.jbsa.prototype.model.EventEntity;
import de.thi.jbsa.prototype.model.EventName;
import de.thi.jbsa.prototype.model.cmd.PostMessageCmd;
import de.thi.jbsa.prototype.model.event.AbstractEvent;
import de.thi.jbsa.prototype.model.event.MentionEvent;
import de.thi.jbsa.prototype.model.event.MessagePostedEvent;
import de.thi.jbsa.prototype.model.event.MessageRepeatedEvent;
import de.thi.jbsa.prototype.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Christopher Timm <christopher.timm@beskgroup.com> on 2020-04-03
 */
@Service
@Slf4j
public class MessageProcessorService {

  private final Queue eventQueue;

  private final EventRepository eventRepository;

  private final JmsTemplate jmsTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public MessageProcessorService(@Qualifier("eventQueue") Queue eventQueue, JmsTemplate jmsTemplate, EventRepository eventRepository) {
    this.eventQueue = eventQueue;
    this.jmsTemplate = jmsTemplate;
    this.eventRepository = eventRepository;
  }

  private List<MentionEvent> checkForUserMentions(MessagePostedEvent event) {
    List<MentionEvent> mentionEventList = new ArrayList<>();
    Matcher matcher = Pattern.compile("\\s@([\\w_-]+)").matcher(event.getContent());
    while (matcher.find()) {
      String mentionedUser = matcher.group().substring(2);
      MentionEvent mentionEvent = new MentionEvent();
      mentionEvent.setUserId(event.getUserId());
      mentionEvent.setMentionedUser(mentionedUser);
      mentionEvent.setCausationUuid(event.getUuid());
      mentionEventList.add(mentionEvent);
    }
    return mentionEventList;
  }

  private void checkForDuplicateMessagesAndSaveAndSend(MessagePostedEvent event) {
    Optional<EventEntity> previousMessage = eventRepository.findFirstByEventNameAndValueContainingOrderByIdDesc(EventName.MESSAGE_POSTED,
      "userId\":\"" + event.getUserId());
    if (previousMessage.isPresent()) {
      final MessagePostedEvent messagePostedEventFromDb = (MessagePostedEvent) fromJson(previousMessage.get().getValue());
      if (event.getContent().equals(messagePostedEventFromDb.getContent())) {

        Optional<EventEntity> previousRepeatedEvent = eventRepository.findFirstByEventNameAndValueContainingOrderByIdDesc(EventName.MESSAGE_REPEATED,
          "currentMessageEventUUID\":\"" + messagePostedEventFromDb.getUuid());
        MessagePostedEvent newSavedMessagePostedEvent = (MessagePostedEvent) fromJson(saveEvent(event).getValue());
        MessageRepeatedEvent.MessageRepeatedEventBuilder newMessageRepeatedEventBuilder =
          MessageRepeatedEvent.builder()
                              .currentMessageEventUUID(
                                newSavedMessagePostedEvent.getUuid());
        if (previousRepeatedEvent.isPresent()) {
          MessageRepeatedEvent previousMessageRepeatedEventFromDb = (MessageRepeatedEvent) fromJson(previousRepeatedEvent.get().getValue());
          newMessageRepeatedEventBuilder = newMessageRepeatedEventBuilder
            .originalMessageUUID(previousMessageRepeatedEventFromDb.getOriginalMessageUUID())
            .occurCount(previousMessageRepeatedEventFromDb.getOccurCount() + 1);
        } else {
          newMessageRepeatedEventBuilder = newMessageRepeatedEventBuilder
            .originalMessageUUID(messagePostedEventFromDb.getUuid());
        }
        saveAndSendEvent(newMessageRepeatedEventBuilder.build());
        return;
      }
    }
    log.info("Message appeared for the first time. Just sending it through");
    saveAndSendEvent(event);
  }

  private AbstractEvent fromJson(String value) {
    try {
      return objectMapper.readValue(value, AbstractEvent.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("BusinessEvent cannot be deserialized: " + value, e);
    }
  }

  @Censored
  public void postMessage(PostMessageCmd cmd) {

    log.info("creating event for ... " + cmd);
    MessagePostedEvent event = new MessagePostedEvent();
    event.setCmdUuid(cmd.getUuid());
    event.setContent(cmd.getContent());
    event.setUserId(cmd.getUserId());
    // This is the place for more business logic
    List<MentionEvent> mentionEvents = checkForUserMentions(event);
    mentionEvents.forEach(mentionEvent -> {
      log.debug("Found mention of user {}", mentionEvent.getMentionedUser());
      saveAndSendEvent(mentionEvent);
    });

    checkForDuplicateMessagesAndSaveAndSend(event);
  }

  private void saveAndSendEvent(AbstractEvent event) {
    log.info("Saving event " + event);
    EventEntity entity = saveEvent(event);
    event.setEntityId(entity.getId());
    sendEvent(event);
  }

  private EventEntity saveEvent(AbstractEvent event) {
    EventEntity entity = new EventEntity();
    String json = toJson(event);
    entity.setValue(json);
    if (event instanceof MessagePostedEvent) {
      entity.setEventName(EventName.MESSAGE_POSTED);
    } else if (event instanceof MentionEvent) {
      entity.setEventName(EventName.MENTION);
    } else if (event instanceof MessageRepeatedEvent) {
      entity.setEventName(EventName.MESSAGE_REPEATED);
    }
    log.debug("Writing event... : " + json);
    EventEntity savedEventEntity = eventRepository.save(entity);
    log.info("Written event to db " + event);
    return savedEventEntity;
  }

  private String toJson(de.thi.jbsa.prototype.model.event.Event event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("BusinessEvent cannot be serialized: " + event, e);
    }
  }

  private void sendEvent(AbstractEvent event) {
    jmsTemplate.convertAndSend(eventQueue, event);
    log.info("Sent event to queue " + event);
  }
}
