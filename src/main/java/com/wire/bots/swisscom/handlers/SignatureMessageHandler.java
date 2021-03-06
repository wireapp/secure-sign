package com.wire.bots.swisscom.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.ReactionMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.SystemMessage;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.swisscom.DAO.DocumentDAO;
import com.wire.bots.swisscom.DAO.EventsDAO;
import com.wire.bots.swisscom.DAO.SignRequestDAO;
import com.wire.bots.swisscom.DAO.SignerDAO;
import com.wire.bots.swisscom.SwisscomClient;
import com.wire.bots.swisscom.Tools;
import com.wire.bots.swisscom.model.Digest;
import com.wire.bots.swisscom.model.Document;
import com.wire.bots.swisscom.model.Signer;
import org.skife.jdbi.v2.DBI;

import java.io.ByteArrayInputStream;
import java.util.UUID;

public class SignatureMessageHandler extends MessageHandlerBase {
    private static final String PHONE_NUMBER_PROMPT =
            "In order to be able to sign documents you need to provide your phone number.\n" +
                    "Just type your phone# like: +491726842...";
    private static final String PHONE_NUMBER_CONFIRMATION =
            "Thank you! You will receive an SMS with OTP code to %s when signing a document";
    private static final String OTP_LINK_LABEL =
            "In order to complete the signing process please go to: [link](%s)";
    private static final String WELCOME_NOTE =
            "Digital signature enabled for PDF files. In order to sign a document you need to **Like** it";

    private final ObjectMapper mapper = new ObjectMapper();
    private final SwisscomClient swisscomClient;
    private final SignerDAO signerDAO;
    private final EventsDAO eventsDAO;
    private final DocumentDAO documentDAO;
    private final SignRequestDAO signRequestDAO;

    public SignatureMessageHandler(DBI jdbi, SwisscomClient swisscomClient) {
        this.swisscomClient = swisscomClient;
        signerDAO = jdbi.onDemand(SignerDAO.class);
        eventsDAO = jdbi.onDemand(EventsDAO.class);
        documentDAO = jdbi.onDemand(DocumentDAO.class);
        signRequestDAO = jdbi.onDemand(SignRequestDAO.class);
    }

    @Override
    public boolean onNewBot(NewBot newBot, String auth) {
        User origin = newBot.origin;
        if (1 == signerDAO.insert(origin.id, origin.name, origin.handle))
            Logger.info("onNewBot. New subscriber, bot: %s, user: %s", newBot.id, origin.id);

        return true;
    }

    @Override
    public void onNewConversation(WireClient client, SystemMessage message) {
        try {
            client.sendText(WELCOME_NOTE);
            for (Member member : message.conversation.members) {
                if (member.service != null)
                    continue;

                UUID userId = member.id;
                Signer signer = signerDAO.getSigner(userId);
                if (signer != null) {
                    if (signer.phone == null) {
                        client.sendDirectText(PHONE_NUMBER_PROMPT, userId);
                    }
                } else {
                    User user = client.getUser(userId);
                    signerDAO.insert(user.id, user.name, user.handle);
                    client.sendDirectText(PHONE_NUMBER_PROMPT, userId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("onNewConversation: %s", e);
        }
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage message) {
        try {
            for (UUID userId : message.users) {
                Signer signer = signerDAO.getSigner(userId);
                if (signer != null) {
                    if (signer.phone == null) {
                        client.sendDirectText(PHONE_NUMBER_PROMPT, userId);
                    }
                } else {
                    User user = client.getUser(userId);
                    signerDAO.insert(user.id, user.name, user.handle);
                    client.sendDirectText(PHONE_NUMBER_PROMPT, userId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("onMemberJoin: %s", e);
        }
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            String command = msg.getText();
            if (isPhoneNumber(command)) {
                String phone = normalizePhoneNumber(command);
                int update = signerDAO.update(msg.getUserId(), phone);
                if (update != 0) {
                    String text = String.format(PHONE_NUMBER_CONFIRMATION, command);
                    client.sendDirectText(text, msg.getUserId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("onText: user: %s, e: %s", msg.getUserId(), e);
        }
    }

    private String normalizePhoneNumber(String command) {
        return command.replace("+", "").replace(" ", "");
    }

    private boolean isPhoneNumber(String command) {
        return command.startsWith("+") && command.length() >= 12;
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage msg) {
        try {
            if (msg.getName().toLowerCase().endsWith(".pdf")) {
                String payload = mapper.writeValueAsString(msg);
                eventsDAO.insert(msg.getMessageId(), msg.getConversationId(), msg.getMimeType(), payload);
                byte[] document = client.downloadAsset(msg.getAssetKey(), msg.getAssetToken(), msg.getSha256(), msg.getOtrKey());
                documentDAO.insert(msg.getMessageId(), document, msg.getUserId(), msg.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReaction(WireClient client, ReactionMessage msg) {
        if (msg.getEmoji().isEmpty()) {
            return;
        }

        UUID botId = client.getId();
        UUID documentId = msg.getReactionMessageId();
        UUID userId = msg.getUserId();

        try {
            Document document = documentDAO.getDocument(documentId);
            if (document == null) {
                Logger.info("Unknown document: %s", documentId);
                return;
            }

            Signer signer = signerDAO.getSigner(userId);
            if (signer.phone == null) {
                Logger.info("Prompting user %s for phone number", userId);
                client.sendDirectText(PHONE_NUMBER_PROMPT, userId);
                return;
            }

            Logger.info("Signing eventId: %s, user: %s", document.documentId, userId);

            byte[] original = documentDAO.getOriginal(documentId);

            Digest digest = Tools.createSignature(new ByteArrayInputStream(original));
            documentDAO.update(documentId, digest.pdf);

            SwisscomClient.SignResponse signResponse = swisscomClient.sign(signer, document, digest.hash);
            if (signResponse.optionalOutputs == null || signResponse.optionalOutputs.stepUpAuthorisationInfo == null) {
                client.sendDirectText(signResponse.result.minor, userId);
                return;
            }

            SwisscomClient.StepUpAuthorisationInfo stepUpAuthorisationInfo = signResponse.optionalOutputs.stepUpAuthorisationInfo;
            UUID requestId = signResponse.requestId;
            UUID responseId = signResponse.optionalOutputs.responseId;

            signRequestDAO.insert(requestId, responseId, botId, userId, documentId);

            String url = stepUpAuthorisationInfo.result.url;
            String format = String.format(OTP_LINK_LABEL, url);

            client.sendDirectText(format, userId);
        } catch (Exception e) {
            Logger.error(e.getMessage());
            try {
                final String txt = String.format("Failed to send signing req to AIS: %s", e);
                client.sendDirectText(txt, userId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
