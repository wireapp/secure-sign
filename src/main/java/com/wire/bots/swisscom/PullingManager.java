package com.wire.bots.swisscom;

import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.swisscom.DAO.DocumentDAO;
import com.wire.bots.swisscom.DAO.SignRequestDAO;
import com.wire.bots.swisscom.DAO.SignatureDAO;
import com.wire.bots.swisscom.DAO.SignerDAO;
import com.wire.bots.swisscom.model.SignRequest;
import com.wire.bots.swisscom.model.Signer;
import org.skife.jdbi.v2.DBI;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

class PullingManager {
    private final DBI jdbi;
    private final SwisscomClient swisscomClient;

    PullingManager(DBI jdbi, SwisscomClient swisscomClient) {
        this.jdbi = jdbi;
        this.swisscomClient = swisscomClient;
    }

    void pull() {
        SignRequestDAO signRequestDAO = jdbi.onDemand(SignRequestDAO.class);
        SignerDAO signerDAO = jdbi.onDemand(SignerDAO.class);
        DocumentDAO documentDAO = jdbi.onDemand(DocumentDAO.class);
        SignatureDAO signatureDAO = jdbi.onDemand(SignatureDAO.class);

        List<SignRequest> signRequests = signRequestDAO.get();
        for (SignRequest request : signRequests) {
            try {
                // pull the cms from Swisscom
                SwisscomClient.SignResponse signResponse = swisscomClient.pending(request.responseId);
                if (signResponse == null || signResponse.signature == null)
                    continue;

                UUID requestId = request.requestId;
                Signer signer = signerDAO.getSigner(request.userId);

                // save cms into db
                SwisscomClient.ExtendedSignatureObject signObj = signResponse.signature.other.signatureObjects.extendedSignatureObject;
                byte[] cms = Base64.getDecoder().decode(signObj.base64Signature.value);
                signatureDAO.insert(request.responseId, requestId, request.documentId, request.userId, cms);

                WireClient client = Service.instance.getRepo().getClient(request.botId);

                // post CMS
                File cmsFile = Tools.writeToFile(cms, String.format("signatures/%s.cms", request.documentId));
                client.sendFile(cmsFile, "application/cms");

                String documentName = signObj.documentId;

                Logger.info("Received signature: req: %s, doc: %s, cms: %d",
                        requestId,
                        documentName,
                        cms.length);

                // get the original doc from db
                byte[] pdf = documentDAO.getSigned(request.documentId);

                try {
                    Tools.verify(pdf, cms);
                } catch (Exception e) {
                    Logger.warning("CMS for %s ist corrupt: %s", request.documentId, e);
                }

                // attach cms to pdf
                String outPdfFilename = String.format("signatures/signed_%s", documentName);
                File outPdfFile = Tools.writeToFile(pdf, outPdfFilename);
                Tools.attachCMS(new ByteArrayInputStream(pdf), outPdfFile, cms);

                // post signed doc into conversation
                client.sendText(String.format("**%s** signed document: `%s`", signer.name, documentName));
                UUID msgId = client.sendFile(outPdfFile, "application/pdf");

                signRequestDAO.delete(requestId);

                // save signed doc into db as new doc ready to be signed by other participants
                byte[] signedDoc = Tools.readFile(outPdfFile);
                documentDAO.insert(msgId, signedDoc, client.getId(), documentName);

                // update original document with the signed one
                documentDAO.update(request.documentId, signedDoc);
            } catch (Exception e) {
                e.printStackTrace();
                Logger.info(e.getMessage());
            }
        }
    }
}
