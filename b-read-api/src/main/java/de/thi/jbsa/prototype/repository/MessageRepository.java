package de.thi.jbsa.prototype.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.mongodb.repository.MongoRepository;
import de.thi.jbsa.prototype.domain.MessageDoc;

/**
 * @author Christopher Timm <christopher.timm@beskgroup.com> on 2020-04-01
 */
public interface MessageRepository
  extends MongoRepository<MessageDoc, String> {

  List<MessageDoc> findFirst10ByOrderByMessage_CreatedDesc();
  Optional<MessageDoc> findFirstByMessage_EventUuidOrderByMessage_CreatedDesc(UUID messageUUID);

}
