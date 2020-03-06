package com.wire.bots.swisscom.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.*;
import com.wire.bots.sdk.server.model.SystemMessage;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.swisscom.DAO.EventsDAO;
import org.skife.jdbi.v2.DBI;

import java.util.UUID;

public class RecordingMessageHandler extends MessageHandlerBase {
    private final ObjectMapper mapper = new ObjectMapper();

    private final EventsDAO eventsDAO;

    public RecordingMessageHandler(DBI jdbi) {
        this.eventsDAO = jdbi.onDemand(EventsDAO.class);
    }

    @Override
    public void onNewConversation(WireClient client, SystemMessage msg) {
        UUID convId = msg.convId;
        UUID botId = client.getId();
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, botId, messageId, type, msg);
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage msg) {
        UUID botId = client.getId();

        Logger.debug("onMemberJoin: %s users: %s", botId, msg.users);

        UUID convId = msg.convId;
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, botId, messageId, type, msg);
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage msg) {
        UUID convId = msg.convId;
        UUID botId = client.getId();
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, botId, messageId, type, msg);
    }

    @Override
    public void onConversationRename(WireClient client, SystemMessage msg) {
        UUID convId = msg.convId;
        UUID botId = client.getId();
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, botId, messageId, type, msg);
    }

    @Override
    public void onBotRemoved(UUID botId, SystemMessage msg) {
        UUID convId = msg.convId;
        UUID messageId = msg.id;
        String type = "conversation.member-leave.bot-removed";

        persist(convId, null, botId, messageId, type, msg);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID userId = msg.getUserId();
        UUID botId = client.getId();
        UUID messageId = msg.getMessageId();
        UUID convId = client.getConversationId();
        String type = "conversation.otr-message-add.new-text";

        persist(convId, userId, botId, messageId, type, msg);
    }

    @Override
    public void onEditText(WireClient client, EditedTextMessage msg) {
        UUID botId = client.getId();
        UUID convId = client.getConversationId();
        UUID userId = msg.getUserId();
        UUID messageId = msg.getMessageId();
        UUID replacingMessageId = msg.getReplacingMessageId();
        String type = "conversation.otr-message-add.edit-text";

        try {
            String payload = mapper.writeValueAsString(msg);
            int update = eventsDAO.update(replacingMessageId, type, payload);
            Logger.info("%s: conv: %s, %s -> %s, msg: %s, replacingMsgId: %s, update: %d",
                    type,
                    convId,
                    userId,
                    botId,
                    messageId,
                    replacingMessageId,
                    update);
        } catch (Exception e) {
            Logger.error("onEditText: %s msg: %s, replacingMsgId: %s, %s", botId, messageId, replacingMessageId, e);
        }
    }

    @Override
    public void onDelete(WireClient client, DeletedTextMessage msg) {
        UUID botId = client.getId();
        UUID messageId = msg.getMessageId();
        UUID convId = client.getConversationId();
        UUID userId = msg.getUserId();
        String type = "conversation.otr-message-add.delete-text";

        persist(convId, userId, botId, messageId, type, msg);
        eventsDAO.delete(msg.getDeletedMessageId());
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        UUID convId = client.getConversationId();
        UUID messageId = msg.getMessageId();
        UUID botId = client.getId();
        UUID userId = msg.getUserId();
        String type = "conversation.otr-message-add.new-image";

        try {
            persist(convId, userId, botId, messageId, type, msg);
        } catch (Exception e) {
            Logger.error("onImage: %s %s %s", botId, messageId, e);
        }
    }

    @Override
    public void onVideo(WireClient client, VideoMessage msg) {
        UUID convId = client.getConversationId();
        UUID messageId = msg.getMessageId();
        UUID botId = client.getId();
        UUID userId = msg.getUserId();
        String type = "conversation.otr-message-add.new-video";

        try {
            persist(convId, userId, botId, messageId, type, msg);
        } catch (Exception e) {
            Logger.error("onVideo: %s %s %s", botId, messageId, e);
        }
    }

    @Override
    public void onVideoPreview(WireClient client, ImageMessage msg) {
        UUID convId = client.getConversationId();
        UUID messageId = UUID.randomUUID();
        UUID botId = client.getId();
        UUID userId = msg.getUserId();
        String type = "conversation.otr-message-add.new-preview";

        try {
            persist(convId, userId, botId, messageId, type, msg);
        } catch (Exception e) {
            Logger.error("onVideoPreview: %s %s %s", botId, messageId, e);
        }
    }

    @Override
    public void onLinkPreview(WireClient client, LinkPreviewMessage msg) {
        UUID convId = client.getConversationId();
        UUID messageId = msg.getMessageId();
        UUID botId = client.getId();
        UUID userId = msg.getUserId();
        String type = "conversation.otr-message-add.new-link";

        try {
            persist(convId, userId, botId, messageId, type, msg);
        } catch (Exception e) {
            Logger.error("onLinkPreview: %s %s %s", botId, messageId, e);
        }
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage msg) {
        UUID convId = client.getConversationId();
        UUID botId = client.getId();
        UUID messageId = msg.getMessageId();
        UUID userId = msg.getUserId();
        String type = "conversation.otr-message-add.new-attachment";

        try {
            persist(convId, userId, botId, messageId, type, msg);
        } catch (Exception e) {
            Logger.error("onAttachment: %s %s %s", botId, messageId, e);
        }
    }

    @Override
    public void onReaction(WireClient client, ReactionMessage msg) {
        UUID convId = client.getConversationId();
        UUID messageId = msg.getMessageId();
        UUID botId = client.getId();
        UUID userId = msg.getUserId();
        String type = "conversation.otr-message-add.new-reaction";

        persist(convId, userId, botId, messageId, type, msg);
    }

    @Override
    public void onPing(WireClient client, PingMessage msg) {
        UUID botId = client.getId();
        UUID convId = client.getConversationId();
        UUID messageId = msg.getMessageId();
        UUID userId = msg.getUserId();
        String type = "conversation.otr-message-add.new-ping";

        persist(convId, userId, botId, messageId, type, msg);
    }

    private void persist(UUID convId, UUID senderId, UUID userId, UUID msgId, String type, Object msg)
            throws RuntimeException {
        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(msgId, convId, type, payload);

            Logger.info("%s: conv: %s, %s -> %s, msg: %s, insert: %d",
                    type,
                    convId,
                    senderId,
                    userId,
                    msgId,
                    insert);
        } catch (Exception e) {
            String error = String.format("%s: conv: %s, user: %s, msg: %s, e: %s",
                    type,
                    convId,
                    userId,
                    msgId,
                    e);
            Logger.error(error);
            throw new RuntimeException(error);
        }
    }
}
