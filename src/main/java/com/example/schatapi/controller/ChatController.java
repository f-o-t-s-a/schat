package com.example.schatapi.controller;

import com.example.schatapi.model.Message;
import com.example.schatapi.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private MessageRepository messageRepository;

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public Message sendMessage(Message message) {
        messageRepository.save(message);
        return message;
    }

    @GetMapping("/api/messages/{roomId}")
    @ResponseBody
    public List<Message> getMessages(@PathVariable String roomId) {
        return messageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }
}
