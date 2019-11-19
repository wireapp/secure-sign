package com.wire.bots.swisscom;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.swisscom.model.Config;
import com.wire.bots.swisscom.model.Digest;
import com.wire.bots.swisscom.model.Document;
import com.wire.bots.swisscom.model.Signer;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;


public class SignatureTest {
    @ClassRule
    public static DropwizardAppRule<Config> app = new DropwizardAppRule<>(Service.class,
            "swisscom.yaml",
            ConfigOverride.config("database.driverClass", "test"));
    private static final String INPUT_PDF = "input.pdf";

    @Test
    public void test() {
        try {
            JerseyClientConfiguration jerseyCfg = app.getConfiguration().getJerseyClientConfiguration();

            Client client = new JerseyClientBuilder(app.getEnvironment())
                    .using(jerseyCfg)
                    .withProvider(JacksonJsonProvider.class)
                    .build("test");

            SwisscomClient swisscomClient = new SwisscomClient(client);

            UUID userId = UUID.randomUUID();

            ClassLoader classLoader = EmbedTest.class.getClassLoader();
            InputStream fis = classLoader.getResourceAsStream(INPUT_PDF);
            Digest digest = Tools.createSignature(fis);

            byte[] pdf = digest.pdf;
            String hash = digest.hash;

            Signer signer = new Signer();
            signer.userId = userId;
            signer.name = "Dejan Kovacevic";
            signer.phone = "491726842882";
            signer.pseudonym = "dejan";

            Document document = new Document();
            document.name = INPUT_PDF;
            document.documentId = UUID.randomUUID();
            document.owner = userId;

            SwisscomClient.SignResponse pendingRes = swisscomClient.sign(signer, document, hash);

            String url = pendingRes.optionalOutputs.stepUpAuthorisationInfo.result.url;
            UUID responseId = pendingRes.optionalOutputs.responseId;

            SwisscomClient.SignResponse signResponse = swisscomClient.pending(responseId);

            if (signResponse.signature != null) {
                SwisscomClient.ExtendedSignatureObject signObj = signResponse.signature.other.signatureObjects.extendedSignatureObject;
                byte[] signature = Base64.getDecoder().decode(signObj.base64Signature.value);

                File resultingPdfFile = new File("result.pdf");

                Tools.attachCMS(new ByteArrayInputStream(pdf), resultingPdfFile, signature);

                resultingPdfFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("Digital signatures are difficult: %s", e);
        }
    }
}
