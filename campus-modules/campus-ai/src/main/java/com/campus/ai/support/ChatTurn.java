package com.campus.ai.support;

import lombok.Data;

import java.io.Serializable;

/**
 * 智能客服的一轮会话（存 Redis：ai:chat:{sessionId}，仅保留最近 6 轮）
 *
 * @author campus-mart
 */
@Data
public class ChatTurn implements Serializable {

    private static final long serialVersionUID = 1L;

    private String question;

    private String answer;

    private long time;

    public ChatTurn() {
    }

    public ChatTurn(String question, String answer, long time) {
        this.question = question;
        this.answer = answer;
        this.time = time;
    }
}
