package com.wire.bots.swisscom;


import com.wire.bots.sdk.tools.Util;
import com.wire.bots.swisscom.model.Digest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.util.Hex;
import org.junit.Test;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

public class EmbedTest {
    private static final String CMS_SIG = "cms.sig";

    private static final String OUTPUT_PDF = "output.pdf";
    private static final String INPUT_PDF = "input.pdf";
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

        new File(OUTPUT_PDF).delete();
    }

    @Test
    public void test2() throws IOException {
        InputStream resourceAsStream = classLoader.getResourceAsStream(INPUT_PDF);
        FileOutputStream fos = new FileOutputStream(OUTPUT_PDF);
        CreateSignature createSignature = new CreateSignature();
        createSignature.signPdf(resourceAsStream, fos);
        new File(OUTPUT_PDF).delete();
    }

    @Test
    public void test3() throws IOException, NoSuchAlgorithmException {
        InputStream cmsStream = classLoader.getResourceAsStream(CMS_SIG);
        InputStream fis = classLoader.getResourceAsStream(INPUT_PDF);
        Digest digest = Tools.createSignature(fis);
        File out = Tools.writeToFile(digest.pdf, OUTPUT_PDF);
        Tools.attachCMS(new ByteArrayInputStream(digest.pdf), out, Util.toByteArray(cmsStream));

        out.delete();
    }
}
