package com.wire.bots.swisscom.handlers;

import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.*;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.SystemMessage;

import java.util.UUID;

public class MessageHandler extends MessageHandlerBase {

    private final SignatureMessageHandler signatureMessageHandler;
    private final RecordingMessageHandler recordingMessageHandler;

    public MessageHandler(SignatureMessageHandler signatureMessageHandler, RecordingMessageHandler recordingMessageHandler) {
        this.signatureMessageHandler = signatureMessageHandler;
        this.recordingMessageHandler = recordingMessageHandler;
    }

    @Override
    public boolean onNewBot(NewBot newBot, String auth) {
        return signatureMessageHandler.onNewBot(newBot, auth) && recordingMessageHandler.onNewBot(newBot, auth);
    }

    @Override
    public void onNewConversation(WireClient client, SystemMessage message) {
        signatureMessageHandler.onNewConversation(client, message);
        recordingMessageHandler.onNewConversation(client, message);
    }

    @Override
    public void onText(WireClient client, TextMessage message) {
        signatureMessageHandler.onText(client, message);
        recordingMessageHandler.onText(client, message);
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage message) {
        signatureMessageHandler.onMemberJoin(client, message);
        recordingMessageHandler.onMemberJoin(client, message);
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage message) {
        signatureMessageHandler.onMemberLeave(client, message);
        recordingMessageHandler.onMemberLeave(client, message);
    }

    @Override
    public void onConversationRename(WireClient client, SystemMessage message) {
        signatureMessageHandler.onConversationRename(client, message);
        recordingMessageHandler.onConversationRename(client, message);
    }

    @Override
    public void onBotRemoved(UUID botId, SystemMessage msg) {
        signatureMessageHandler.onBotRemoved(botId, msg);
        recordingMessageHandler.onBotRemoved(botId, msg);
    }

    @Override
    public void onEditText(WireClient client, EditedTextMessage msg) {
        signatureMessageHandler.onEditText(client, msg);
        recordingMessageHandler.onEditText(client, msg);
    }

    @Override
    public void onDelete(WireClient client, DeletedTextMessage msg) {
        signatureMessageHandler.onDelete(client, msg);
        recordingMessageHandler.onDelete(client, msg);
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        signatureMessageHandler.onImage(client, msg);
        recordingMessageHandler.onImage(client, msg);
    }

    @Override
    public void onVideo(WireClient client, VideoMessage msg) {
        signatureMessageHandler.onVideo(client, msg);
        recordingMessageHandler.onVideo(client, msg);
    }

    @Override
    public void onVideoPreview(WireClient client, ImageMessage msg) {
        signatureMessageHandler.onVideoPreview(client, msg);
        recordingMessageHandler.onVideoPreview(client, msg);
    }

    @Override
    public void onLinkPreview(WireClient client, LinkPreviewMessage msg) {
        signatureMessageHandler.onLinkPreview(client, msg);
        recordingMessageHandler.onLinkPreview(client, msg);
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage msg) {
        signatureMessageHandler.onAttachment(client, msg);
        recordingMessageHandler.onAttachment(client, msg);
    }

    @Override
    public void onReaction(WireClient client, ReactionMessage msg) {
        signatureMessageHandler.onReaction(client, msg);
        recordingMessageHandler.onReaction(client, msg);
    }

    @Override
    public void onPing(WireClient client, PingMessage msg) {
        signatureMessageHandler.onPing(client, msg);
        recordingMessageHandler.onPing(client, msg);
    }
}
