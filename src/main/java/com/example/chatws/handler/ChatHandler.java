package com.example.chatws.handler;

import com.example.chatws.DTO.ChatMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper;
    private final Set<WebSocketSession> sessions = new HashSet<>();
    private final Map<Long, Set<WebSocketSession>> chatRoomSessionMap = new HashMap<>();

    //클라이언트와 소켓 연결후 동작 메서드
    //위의 세션 목록(=sessions)에  웹소켓 세션객체를 넣어줌
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("{} 연결됨", session.getId());
        sessions.add(session);
    }

    //텍스트 메세지를 처리하는 메서드
    //소켓으로 들어온 문자 메시지에 페이로드를 가져옴
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("payload {}", payload);

        //DTO객체에 소켓 통신으로 받아온 값 메핑
        ChatMessageDTO chatMessageDTO = mapper.readValue(payload, ChatMessageDTO.class);
        log.info("session{}", chatMessageDTO.toString());

        //DTO에 매핑한 RoomID 정보
        Long chatRoomId = chatMessageDTO.getChatRoomId();

        //서버에 있는 채팅룸 세션 변수에 해당 id가 없으면 추가
        if (!chatRoomSessionMap.containsKey(chatRoomId)) {
            chatRoomSessionMap.put(chatRoomId, new HashSet<>());
        }
        //서버에 있는 채팅룸세션목록에서 해당 id의 채팅방을 가져옴
        Set<WebSocketSession> chatRoomSession = chatRoomSessionMap.get(chatRoomId);

        //DTO에서 해당 데이터가 ENTER이면 해당 세션을 추가해줌
        if (chatMessageDTO.getMessageTYPE().equals(ChatMessageDTO.MessageTYPE.ENTER)) {
            chatRoomSession.add(session);
        }
        //채팅방 세션의 크기가 3이하면 해당 세션을 제거하는 메서드 실행
        if (chatRoomSession.size() >= 3) {
            removeClosedSession(chatRoomSession);
        }
        //
        sendMessageToChatRoom(chatMessageDTO, chatRoomSession);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

    private void removeClosedSession(Set<WebSocketSession> chatRoomSession) {
        chatRoomSession.removeIf(s -> !sessions.contains(s));
    }

    public void sendMessageToChatRoom(ChatMessageDTO chatMessageDTO, Set<WebSocketSession> chatRoomSession) {
        chatRoomSession.parallelStream().forEach(s-> sendMessage(s, chatMessageDTO));
    }

    public <T> void sendMessage(WebSocketSession session, T message) {
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

}
