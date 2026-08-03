package com.dbtraining.reconx.model;

import com.dbtraining.reconx.dto.TradeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TradeEventJsonConverter implements AttributeConverter<TradeEvent, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(TradeEvent attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize TradeEvent for DLQ storage", e);
        }
    }

    @Override
    public TradeEvent convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return MAPPER.readValue(dbData, TradeEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize TradeEvent from DLQ storage", e);
        }
    }
}
