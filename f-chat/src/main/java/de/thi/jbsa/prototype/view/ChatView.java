package de.thi.jbsa.prototype.view;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import de.thi.jbsa.prototype.consumer.UiEventConsumer;
import de.thi.jbsa.prototype.model.cmd.PostMessageCmd;
import de.thi.jbsa.prototype.model.event.AbstractEvent;
import de.thi.jbsa.prototype.model.event.MentionEvent;
import de.thi.jbsa.prototype.model.event.MessagePostedEvent;
import de.thi.jbsa.prototype.model.event.MessageRepeatedEvent;
import de.thi.jbsa.prototype.model.model.Message;
import lombok.extern.slf4j.Slf4j;

@UIScope
@SpringComponent
@Route("home")
@Slf4j
@Push(transport = Transport.WEBSOCKET)
public class ChatView
        extends VerticalLayout {

  private enum EventHandler {
    MESSAGE_POSTED(MessagePostedEvent.class) {
      @Override
      void handle(ChatView chatView, AbstractEvent event) {
        chatView.addMessageImpl(chatView.createMsg((MessagePostedEvent) event));
      }
    },
    MESSAGE_REPEATED(MessageRepeatedEvent.class) {
      @Override
      void handle(ChatView chatView, AbstractEvent event) {
        chatView.setCounterForMessage((MessageRepeatedEvent) event);
      }
    },
    NOTIFICATION(MentionEvent.class) {
      @Override
      void handle(ChatView chatView, AbstractEvent event) {
        MentionEvent mentionEvent = (MentionEvent) event;
        if (mentionEvent.getMentionedUser().equals(chatView.sendUserIdField.getValue())) {
          Notification.show("You were mentioned in a message from " + mentionEvent.getUserId());
        }
      }
    };

    private final Class<? extends AbstractEvent> eventType;

    EventHandler(Class<? extends AbstractEvent> eventType) {
      this.eventType = eventType;
    }

    abstract void handle(ChatView chatView, AbstractEvent event);

    static EventHandler valueOf(AbstractEvent event) {

      return Stream.of(values())
                   .filter(h -> h.eventType.equals(event.getClass()))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException("Event not supported: " + event));
    }
  }

  private final List<Message> messagesForListBox = new ArrayList<>();

  private final ListBox<Message> msgListBox;

  private final TextField sendUserIdField;

  final RestTemplate restTemplate;

  private Registration eventRegistration;

  @Value("${studychat.url.getEvents}")
  private String getEventsUrl;

  @Value("${studychat.url.getMessages}")
  private String getMessagesUrl;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<UUID> lastUUID = Optional.empty();

  @Value("${studychat.url.sendMessage}")
  private String sendMessageUrl;

  public ChatView(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    HorizontalLayout componentLayout = new HorizontalLayout();

    VerticalLayout sendLayout = new VerticalLayout();
    VerticalLayout fetchLayout = new VerticalLayout();

    sendUserIdField = new TextField("User-ID");
    sendUserIdField.setValue("User-ID");

    TextField sendMessageField = new TextField("Message To Send");
    sendMessageField.setValue("My Message");

    sendUserIdField.addKeyPressListener(Key.ENTER, e -> sendMessage(sendMessageField.getValue(), sendUserIdField.getValue()));
    sendMessageField.addKeyPressListener(Key.ENTER, e -> sendMessage(sendMessageField.getValue(), sendUserIdField.getValue()));

    Button sendMessageButton = new Button("Send message");
    sendMessageButton.addClickListener(e -> sendMessage(sendMessageField.getValue(), sendUserIdField.getValue()));
    sendMessageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    msgListBox = new ListBox<>();
    MessageFormat msgListBoxTipFormat = new MessageFormat(
      "" +
        "Sent: \t\t{0,time,short}\n" +
        "From: \t\t{1}\n" +
        "Cmd-UUID: \t{2}\n" +
        "Event-UUID: \t{3}\n" +
        "Entity-ID: \t\t{4}\n" +
        "OccurCount: \t\t{5}\n");

    msgListBox.setRenderer(new ComponentRenderer<>(msg -> {
      Label label;
      if (msg.getOccurCount() > 1) {
        label = new Label(msg.getOccurCount() + "x - " + msg.getContent());
      } else {
        label = new Label(msg.getContent());
      }
      label.setEnabled(false);
      Object[] strings = { msg.getCreated(), msg.getSenderUserId(), msg.getCmdUuid(), msg.getEventUuid(), msg.getEntityId(), msg.getOccurCount() };
      String tip = msgListBoxTipFormat.format(strings);
      label.setTitle(tip);
      return label;
    }));
    add(new Text("Welcome to Studychat"));
    sendLayout.add(sendUserIdField);
    sendLayout.add(sendMessageField);
    sendLayout.add(sendMessageButton);

    fetchLayout.add(msgListBox);

    componentLayout.add(sendLayout);
    componentLayout.add(fetchLayout);
    add(componentLayout);
  }

  private void addMessageImpl(Message msg) {
    messagesForListBox.add(msg);
  }

  private void addNewEvent(AbstractEvent event) {
    addNewEvent(Collections.singletonList(event));
  }

  private void addNewEvent(List<AbstractEvent> eventList) {
    if (eventList.size() > 0) {
      lastUUID = Optional.of(eventList.get(eventList.size() - 1).getUuid());
    }

    eventList.forEach(event -> EventHandler.valueOf(event).handle(this, event));
    msgListBox.setItems(messagesForListBox);
  }

  private void addNewMessages(List<Message> allMessages) {
    messagesForListBox.addAll(allMessages);
    msgListBox.setItems(messagesForListBox);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    addNewMessages(getMessagesForInitialState());
    UI ui = attachEvent.getUI();
    eventRegistration = UiEventConsumer.registrer(abstractEvent -> ui.access(() -> addNewEvent(abstractEvent)));
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

  private List<Message> getMessagesForInitialState() {
    ResponseEntity<Message[]> responseEntity = restTemplate.getForEntity(getMessagesUrl, Message[].class);
    if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
      return new ArrayList<>(Arrays.asList(responseEntity.getBody()));
    }
    return new ArrayList<>();
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    if (eventRegistration != null) {
      eventRegistration.remove();
      eventRegistration = null;
    }
  }

  private void setCounterForMessage(MessageRepeatedEvent event) {
    Optional<Message> existingMessage = messagesForListBox.stream()
                                                          .filter(message -> message.getEventUuid().equals(event.getOriginalMessageUUID()))
                                                          .findFirst();
    if (existingMessage.isPresent()) {
      int messageIndex = messagesForListBox.indexOf(existingMessage.get());
      messagesForListBox.remove(messageIndex);
      existingMessage.get().setOccurCount(event.getOccurCount());
      messagesForListBox.add(messageIndex, existingMessage.get());
    }
    msgListBox.setItems(messagesForListBox);
  }

  private void sendMessage(String message, String userId) {
    PostMessageCmd cmd = new PostMessageCmd(userId, message);
    restTemplate.postForEntity(sendMessageUrl, cmd, PostMessageCmd.class);
  }

}
