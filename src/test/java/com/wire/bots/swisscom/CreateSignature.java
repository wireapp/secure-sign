package com.wire.bots.swisscom;

import com.wire.bots.sdk.tools.Util;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class CreateSignature implements SignatureInterface {

    boolean signPdf(InputStream fis, OutputStream fos) throws IOException {
        SignatureOptions signatureOptions = new SignatureOptions();
        signatureOptions.setPreferredSignatureSize(41 * 1000);

        PDDocument doc = PDDocument.load(fis);
        PDSignature signature = new PDSignature();

        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName("NAME");
        signature.setLocation("LOCATION");
        signature.setReason("REASON");
        signature.setSignDate(Calendar.getInstance());
        doc.addSignature(signature, this, signatureOptions);
        doc.saveIncremental(fos);
        return true;
    }

    @Override
    public byte[] sign(InputStream is) throws IOException {
        return Util.getResource("cms.sig");
    }
}

