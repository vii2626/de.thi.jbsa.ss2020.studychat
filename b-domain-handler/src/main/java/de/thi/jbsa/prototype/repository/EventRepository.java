package de.thi.jbsa.prototype.repository;

import de.thi.jbsa.prototype.model.EventName;
//import de.thi.jbsa.prototype.model.event.AbstractEvent;
import org.springframework.data.repository.CrudRepository;
import de.thi.jbsa.prototype.model.EventEntity;

//import java.util.UUID;

/**
 * @author Christopher Timm <christopher.timm@beskgroup.com> on 27.02.18
 */
public interface EventRepository
        extends CrudRepository<EventEntity, Long> {

//  EventEntity findByUuid(UUID uuid);

//  EventEntity findFirstByEventNameOrderByIdDesc(EventName eventname);

  EventEntity findFirstByEventNameAndValueContainingOrderByIdDesc(EventName eventname, String containedInValue);
}
