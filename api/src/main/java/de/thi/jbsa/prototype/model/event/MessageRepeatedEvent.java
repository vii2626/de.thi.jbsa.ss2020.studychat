package de.thi.jbsa.prototype.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageRepeatedEvent extends AbstractEvent {

  private final UUID uuid = UUID.randomUUID();

  private UUID cmdUuid;

  private String content;

  private Long entityId;

  private String userId;


}
