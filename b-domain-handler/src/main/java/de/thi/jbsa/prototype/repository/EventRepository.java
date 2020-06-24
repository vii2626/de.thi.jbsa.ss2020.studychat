package de.thi.jbsa.prototype.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import de.thi.jbsa.prototype.model.EventEntity;
import de.thi.jbsa.prototype.model.EventName;

/**
 * @author Christopher Timm <christopher.timm@beskgroup.com> on 27.02.18
 */
public interface EventRepository
  extends CrudRepository<EventEntity, Long> {
  Optional<EventEntity> findFirstByEventNameAndValueContainingOrderByIdDesc(EventName eventname, String containedInValue);
}
