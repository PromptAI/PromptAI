package com.zervice.kbase.llm.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.context.annotation.RequestScope;

import java.io.InputStream;
import java.sql.Connection;

/**
 * llm service
 *
 * <p>
 *   <ul>query</ul>
 *  <ul>embedding</ul>
 *  <ul>query with sse</ul>
 * </p>
 *
 * <p>计费服务各自实现:</p>
 * <p>{@link com.zervice.kbase.llm.service.impl.OpenAILlmService} 需要记录使用</p>
 *
 * @author chenchen
 * @Date 2023/10/13
 */
@RequestScope
public interface LlmService {
    /**
     * chat with message
     *
     * <p>
     *     stream boolean or null Optional Defaults to false
     * If set, partial message deltas will be sent, like in ChatGPT.
     * Tokens will be sent as data-only server-sent events as they become available,
     * with the stream terminated by a data: [DONE] message. Example Python code.
     * </p>
     *
     * @param data {
     *     "model": "gpt-3.5-turbo",
     *     "messages": [
     *       {
     *         "role": "system",
     *         "content": "You are a helpful assistant."
     *       },
     *       {
     *         "role": "user",
     *         "content": "Hello!"
     *       }
     *     ]
     *   }
     *
     * @return InputStream
     */
    InputStream chatStream(JSONObject data) throws Exception;

    /**
     * chat with message
     *
     * @param data {
     *     "model": "gpt-3.5-turbo",
     *     "messages": [
     *       {
     *         "role": "system",
     *         "content": "You are a helpful assistant."
     *       },
     *       {
     *         "role": "user",
     *         "content": "Hello!"
     *       }
     *     ]
     *   }
     * @return {
     *   "id": "chatcmpl-123",
     *   "object": "chat.completion",
     *   "created": 1677652288,
     *   "model": "gpt-3.5-turbo-0613",
     *   "choices": [{
     *     "index": 0,
     *     "message": {
     *       "role": "assistant",
     *       "content": "\n\nHello there, how may I assist you today?",
     *     },
     *     "finish_reason": "stop"
     *   }],
     *   "usage": {
     *     "prompt_tokens": 9,
     *     "completion_tokens": 12,
     *     "total_tokens": 21
     *   }
     * }
     */
    JSONObject chat(JSONObject data) throws Exception;

    /**
     * embedding text
     *
     * @param data   {"input": ["text"]}
     * @return {
     *   "object": "list",
     *   "data": [
     *     {
     *       "object": "embedding",
     *       "embedding": [
     *         0.0023064255,
     *         -0.009327292,
     *         .... (1536 floats total for ada-002)
     *         -0.0028842222,
     *       ],
     *       "index": 0
     *     }
     *   ],
     *   "model": "text-embedding-ada-002",
     *   "usage": {
     *     "prompt_tokens": 8,
     *     "total_tokens": 8
     *   }
     * }
     */
    JSONObject embedding(JSONObject data) throws Exception;

    /**
     * 检查是否有足够的token
     */
    Boolean enoughToken(Connection conn) throws Exception;
}
