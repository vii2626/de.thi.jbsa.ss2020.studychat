package de.thi.jbsa.prototype.view;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import de.thi.jbsa.prototype.model.event.NotificationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import de.thi.jbsa.prototype.model.cmd.MessageList;
import de.thi.jbsa.prototype.model.cmd.PostMessageCmd;
import de.thi.jbsa.prototype.model.event.AbstractEvent;
import de.thi.jbsa.prototype.model.event.EventList;
import de.thi.jbsa.prototype.model.event.MentionEvent;
import de.thi.jbsa.prototype.model.event.MessagePostedEvent;
import de.thi.jbsa.prototype.model.model.Message;
import lombok.extern.slf4j.Slf4j;

@UIScope
@SpringComponent
@Route("home")
@Slf4j
public class ChatView
        extends VerticalLayout {

  private enum EventHandler {
    MESSAGE_POSTED(MessagePostedEvent.class) {
      @Override
      void handle(ChatView chatView, AbstractEvent event) {
        chatView.addMessageImpl(chatView.createMsg((MessagePostedEvent) event));
      }
    },
    NOTIFICATION(NotificationEvent.class) {
      @Override
      void handle(ChatView chatView, AbstractEvent event) {
        throw new UnsupportedOperationException("Not implemented");
      }
    };

    EventHandler(Class<? extends AbstractEvent> eventType) {
      this.eventType = eventType;
    }

    static EventHandler valueOf(AbstractEvent event) {

      return Stream.of(values())
              .filter(h -> h.eventType.equals(event.getClass()))
              .findAny()
              .orElseThrow(() -> new IllegalArgumentException("Event not supported: " + event));
    }
    private final Class<? extends AbstractEvent> eventType;
    abstract void handle(ChatView chatView, AbstractEvent event);


  }

  private void addMessageImpl(Message msg) {
    messagesForListBox.add(msg);
  }

  final RestTemplate restTemplate;

  @Value("${studychat.url.getEvents}")
  private String getEventsUrl;

  @Value("${studychat.url.getMessage}")
  private String getMessageUrl;

  @Value("${studychat.url.getMessages}")
  private String getMessagesUrl;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<UUID> lastUUID = Optional.empty();

  private List<Message> messagesForListBox = new ArrayList<>();

  @Value("${studychat.url.sendMessage}")
  private String sendMessageUrl;

  public ChatView(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    HorizontalLayout componentLayout = new HorizontalLayout();

    VerticalLayout sendLayout = new VerticalLayout();
    VerticalLayout fetchLayout = new VerticalLayout();

    TextField sendUserIdField = new TextField("User-ID");
    sendUserIdField.setValue("User-ID");

    TextField sendMessageField = new TextField("Message To Send");
    sendMessageField.setValue("My Message");

    sendUserIdField.addKeyPressListener(Key.ENTER, e -> sendMessage(sendMessageField.getValue(), sendUserIdField.getValue()));
    sendMessageField.addKeyPressListener(Key.ENTER, e -> sendMessage(sendMessageField.getValue(), sendUserIdField.getValue()));

    Button sendMessageButton = new Button("Send message");
    sendMessageButton.addClickListener(e -> sendMessage(sendMessageField.getValue(), sendUserIdField.getValue()));
    sendMessageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    ListBox<Message> msgListBox = new ListBox<>();
    MessageFormat msgListBoxTipFormat = new MessageFormat(
            "" +
                    "Sent: \t\t{0,time,short}\n" +
                    "From: \t\t{1}\n" +
                    "Cmd-UUID: \t{2}\n" +
                    "Event-UUID: \t{3}\n" +
                    "Entity-ID: \t\t{4}\n");

    msgListBox.setRenderer(new ComponentRenderer<>(msg -> {
      Label label = new Label(msg.getContent());
      label.setEnabled(false);
      Object[] strings = { msg.getCreated(), msg.getSenderUserId(), msg.getCmdUuid(), msg.getEventUuid(), msg.getEntityId() };
      String tip = msgListBoxTipFormat.format(strings);
      label.setTitle(tip);
      return label;
    }));
    //
    Button fetchEventsButton = new Button("Fetch last Events");
    fetchEventsButton.addClickListener(e -> {
      List<AbstractEvent> eventList = getEvents(sendUserIdField.getValue());
      if (eventList.size() > 0) {
        lastUUID = Optional.of(eventList.get(eventList.size() - 1).getUuid());
      }
      List<String> notifications = new ArrayList<>();
      eventList.stream()
               .filter(abstractEvent -> abstractEvent instanceof MentionEvent)
               .map(abstractEvent -> (MentionEvent) abstractEvent)
               .filter(mentionEvent -> mentionEvent.getMentionedUsers().contains(sendUserIdField.getValue()))
               .forEach(mentionEvent -> notifications.add("You were mentioned in a message from " + mentionEvent.getUserId()));

      eventList.stream()
              .forEach(event -> EventHandler.valueOf(event).handle(this, event));
      msgListBox.setItems(messagesForListBox);
      Notification.show(eventList.size() + " items found");
      notifications.forEach(Notification::show);
    });
    fetchEventsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    add(new Text("Welcome to Studychat"));
    sendLayout.add(sendUserIdField);
    sendLayout.add(sendMessageField);
    sendLayout.add(sendMessageButton);

    fetchLayout.add(msgListBox);
    fetchLayout.add(fetchEventsButton);

    componentLayout.add(sendLayout);
    componentLayout.add(fetchLayout);
    add(componentLayout);
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

  private List<Message> getAllMessages() {
    ResponseEntity<MessageList> responseEntity = restTemplate.getForEntity(getMessagesUrl, MessageList.class);
    if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
      return responseEntity.getBody().getMessages();
    }
    return new ArrayList<>();
  }

  private List<AbstractEvent> getEvents(String userId) {

    StringBuilder requestURL = new StringBuilder(getEventsUrl);
    lastUUID.ifPresent(uuid -> requestURL.append("&lastUUID=").append(uuid));
    ResponseEntity<EventList> responseEntity = restTemplate.getForEntity(requestURL.toString(), EventList.class, userId);
    if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
      return responseEntity.getBody().getEvents();
    }
    return new ArrayList<>();
  }

  private void sendMessage(String message, String userId) {
    PostMessageCmd cmd = new PostMessageCmd(userId, message);
    restTemplate.postForEntity(sendMessageUrl, cmd, PostMessageCmd.class);
  }
}
