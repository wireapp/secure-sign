package com.wire.bots.swisscom;

import com.wire.bots.sdk.tools.Util;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class CreateSignature implements SignatureInterface {
    private final SignatureOptions signatureOptions;
    private final PDVisibleSignDesigner visibleSignDesigner;
    private final PDVisibleSigProperties visibleSignatureProperties;
    private final int p;

    public CreateSignature(InputStream image, int page) throws IOException {
        this.p = page;
        signatureOptions = new SignatureOptions();
        signatureOptions.setPreferredSignatureSize(21 * 1000);
        visibleSignatureProperties = new PDVisibleSigProperties();

        visibleSignDesigner = new PDVisibleSignDesigner(image);
        visibleSignDesigner.coordinates(0, -70).zoom(-93f).adjustForRotation();
    }

    boolean signPdf(InputStream fis, OutputStream fos) throws IOException {
        try (PDDocument doc = PDDocument.load(fis)) {
            PDSignature signature = new PDSignature();

            // this builds the signature structures in a separate document
            visibleSignatureProperties.buildSignature();
            signature.setName(visibleSignatureProperties.getSignerName());
            signature.setLocation(visibleSignatureProperties.getSignerLocation());
            signature.setReason(visibleSignatureProperties.getSignatureReason());

            signatureOptions.setVisualSignature(visibleSignatureProperties.getVisibleSignature());
            signatureOptions.setPage(visibleSignatureProperties.getPage() - 1);

            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setSignDate(Calendar.getInstance());
            doc.addSignature(signature, this, signatureOptions);
            doc.saveIncremental(fos);
            return true;
        }
    }

    @Override
    public byte[] sign(InputStream is) throws IOException {
        return Util.getResource("cms.sig");
    }

    public void setVisibleSignatureProperties(String name, String location, String reason, int preferredSize, int page,
                                              boolean visualSignEnabled) {
        visibleSignatureProperties.signerName(name).signerLocation(location).signatureReason(reason)
                .preferredSize(preferredSize).page(page).visualSignEnabled(visualSignEnabled)
                .setPdVisibleSignature(visibleSignDesigner);
    }
}

