package de.thi.jbsa.prototype.model.event;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * @author Christopher Timm <christopher.timm@beskgroup.com> on 12/5/20
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class MessageRepeatedEvent
  extends AbstractEvent {

  private final UUID uuid = UUID.randomUUID();

  @Builder.Default
  private int occurCount = 2;

  private UUID originalMessageUUID;

  private UUID currentMessageEventUUID;

}
