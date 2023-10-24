package com.example.chatws.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    public enum MessageTYPE {
        ENTER, TALK
    }

    private MessageTYPE messageTYPE;
    private Long chatRoomId;
    private Long senderId;
    private String message;
}
