package com.wire.bots.swisscom;


import com.wire.bots.sdk.tools.Util;
import com.wire.bots.swisscom.model.Digest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.util.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Base64;
import java.util.Calendar;

public class EmbedTest {
    private static final String CMS_SIG = "cms.sig";
    private static final String OUTPUT_PDF = "output.pdf";
    private static final String INPUT_PDF = "input.pdf";
    private static final String INPUT_WITH_PLACEHOLDER_PDF = "input_with_placeholder.pdf";
    private static final String SIGNATURE_PNG = "signature.png";
    private ClassLoader classLoader = EmbedTest.class.getClassLoader();

    @Test
    public void test() throws IOException {
        ClassLoader classLoader = EmbedTest.class.getClassLoader();
        SignatureOptions signatureOptions = new SignatureOptions();
        signatureOptions.setPreferredSignatureSize(21 * 1000);

        int offset;
        byte[] cms = Util.getResource(CMS_SIG);
        byte[] hex = Hex.getBytes(cms);

        try (PDDocument document = PDDocument.load(classLoader.getResourceAsStream(INPUT_PDF))) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Dejan Kovacevic");
            signature.setLocation("Prague, Czech Republic");
            signature.setReason("Purchase order");
            signature.setContactInfo("dejan@wire.com");
            signature.setSignDate(Calendar.getInstance());

            document.addSignature(signature, signatureOptions);

            try (FileOutputStream stream = new FileOutputStream(OUTPUT_PDF)) {
                ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(stream);
                externalSigning.setSignature(new byte[0]);

                int[] byteRange = signature.getByteRange();
                int s = byteRange[0];
                int e = byteRange[1] + 1;
                int begin = s + e;
                int len = byteRange[2] - begin;

                // remember the offset (add 1 because of "<")
                offset = begin;
            }
        }

        try (RandomAccessFile raf = new RandomAccessFile(OUTPUT_PDF, "rw")) {
            raf.seek(offset);
            raf.write(hex);
        }
    }

    @Test
    public void test2() {
        try {
            InputStream pdf = classLoader.getResourceAsStream(INPUT_PDF);
            InputStream image = classLoader.getResourceAsStream(SIGNATURE_PNG);

            int page = 1;

            CreateSignature createSignature = new CreateSignature(image, page);

            int preferredSize = 0;
            createSignature.setVisibleSignatureProperties("name", "location", "Security", preferredSize, page, true);

            FileOutputStream fos = new FileOutputStream(OUTPUT_PDF);
            createSignature.signPdf(pdf, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //new File(OUTPUT_PDF).delete();
        }
    }

    @Test
    public void test3() throws IOException, NoSuchAlgorithmException {
        InputStream cmsStream = classLoader.getResourceAsStream(CMS_SIG);
        InputStream fis = classLoader.getResourceAsStream(INPUT_PDF);
        Digest digest = Tools.createSignature(fis);
        File out = Tools.writeToFile(digest.pdf, OUTPUT_PDF);
        Tools.attachCMS(new ByteArrayInputStream(digest.pdf), out, Util.toByteArray(cmsStream));

        //out.delete();
    }

    @Test
    public void testCalcDigest() throws IOException, NoSuchAlgorithmException {
        InputStream fis = classLoader.getResourceAsStream("a4ede2b6-396c-4f71-894c-af4a25a91c52.pdf");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Util.toByteArray(fis));
        final String base64 = Base64.getEncoder().encodeToString(digest);
        System.out.printf("%s\n", base64);
    }

    @Test
    public void verifyBase64Signature() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        InputStream cms = classLoader.getResourceAsStream("a4ede2b6-396c-4f71-894c-af4a25a91c52.cms.txt");
        final byte[] signature = Base64.getDecoder().decode(Tools.toByteArray(cms));

        InputStream pdf = classLoader.getResourceAsStream("a4ede2b6-396c-4f71-894c-af4a25a91c52.pdf");
        final byte[] doc = Util.toByteArray(pdf);

        final boolean verify = Tools.verify(doc, signature);

        assert verify;
    }

    @Test
    public void verifySignature() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        InputStream cms = classLoader.getResourceAsStream("a4ede2b6-396c-4f71-894c-af4a25a91c52.cms");
        final byte[] signature = Tools.toByteArray(cms);

        InputStream pdf = classLoader.getResourceAsStream("a4ede2b6-396c-4f71-894c-af4a25a91c52.pdf");
        final byte[] doc = Util.toByteArray(pdf);

        final boolean verify = Tools.verify(doc, signature);

        assert verify;
    }
}
