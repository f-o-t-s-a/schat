package com.example.schatapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.schatapi.model.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoomIdOrderByTimestampAsc(String roomId);

}
