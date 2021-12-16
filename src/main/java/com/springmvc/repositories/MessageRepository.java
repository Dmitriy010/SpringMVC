package com.springmvc.repositories;

import com.springmvc.models.Message;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends CrudRepository<Message,Long> {
    List<Message> findByTag(String tag);

}
