package de.thi.jbsa.prototype.model.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Christopher Timm <christopher.timm@beskgroup.com> on 2020-04-08
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "jsontype")
@JsonSubTypes({
  @JsonSubTypes.Type(value = MessagePostedEvent.class, name ="messagePostedEvent"),
  @JsonSubTypes.Type(value = MentionEvent.class, name ="mentionEvent"),
  @JsonSubTypes.Type(value = MessageRepeatedEvent.class, name ="repeatedEvent")
})
@Data
@NoArgsConstructor
public abstract class AbstractEvent implements Event {

  private Long entityId;

}
