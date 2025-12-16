package com.online_games_service.common.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ReadingConverter
public class RedisReadingConverter implements Converter<byte[], Map<String, Object>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> convert(byte[] source) {
        try {
            return objectMapper.readValue(source, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize from Redis", e);
        }
    }
}
