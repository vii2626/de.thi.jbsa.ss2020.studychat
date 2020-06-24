package de.thi.jbsa.prototype.model.model;

import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Balázs Endrédi <balazs.endredi@beskgroup.com> on 2020-04-04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

  private UUID cmdUuid;

  private String content;

  private Date created;

  /**
   * Event entity id in the event-source db. Used for identifying of a state snapshot
   */
  private Long entityId;

  private UUID eventUuid;

  private String senderUserId;

  private Integer occurCount = 1;
}
