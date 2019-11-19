package com.wire.bots.swisscom.DAO;

import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.swisscom.model.Event;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EventsResultSetMapper implements ResultSetMapper<Event> {
    @Override
    public Event map(int i, ResultSet rs, StatementContext statementContext) {
        Event event = new Event();
        try {
            event.conversationId = getUuid(rs, "conversationId");
            event.time = rs.getString("time");
            event.type = rs.getString("type");
            event.payload = rs.getString("payload");
            event.messageId = getUuid(rs, "messageId");
            return event;
        } catch (Exception e) {
            Logger.error("EventsResultSetMapper: i: %d, e: %s", i, e);
            return event;
        }
    }

    private UUID getUuid(ResultSet rs, String name) throws SQLException {
        UUID contact = null;
        Object rsObject = rs.getObject(name);
        if (rsObject != null)
            contact = (UUID) rsObject;
        return contact;
    }
}
