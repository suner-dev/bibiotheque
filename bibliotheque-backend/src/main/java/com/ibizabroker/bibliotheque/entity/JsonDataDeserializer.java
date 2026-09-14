package com.ibizabroker.bibliotheque.entity;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.stereotype.Component;

@Component
public class JsonDataDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String dateString = parser.getText();
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        try {
            return simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            throw new IOException("Invalid date format: " + dateString, e);
        }
    }
}