package com.wire.bots.swisscom;

import com.wire.bots.swisscom.model.Digest;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.util.Hex;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;

public class Tools {
    private static final String SIGNATURE_PNG = "signature.png";
    private static final ClassLoader classLoader = Tools.class.getClassLoader();

    private static final int PREFERRED_SIGNATURE_SIZE = 21 * 1000;

    public static void setMDPPermission(PDDocument doc, PDSignature signature, int accessPermissions) {
        COSDictionary sigDict = signature.getCOSObject();

        // DocMDP specific stuff
        COSDictionary transformParameters = new COSDictionary();
        transformParameters.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
        transformParameters.setInt(COSName.P, accessPermissions);
        transformParameters.setName(COSName.V, "1.2");
        transformParameters.setNeedToBeUpdated(true);

        COSDictionary referenceDict = new COSDictionary();
        referenceDict.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
        referenceDict.setItem("TransformMethod", COSName.getPDFName("DocMDP"));
        referenceDict.setItem("DigestMethod", COSName.getPDFName("SHA2"));
        referenceDict.setItem("TransformParams", transformParameters);
        referenceDict.setNeedToBeUpdated(true);

        COSArray referenceArray = new COSArray();
        referenceArray.add(referenceDict);
        sigDict.setItem("Reference", referenceArray);
        referenceArray.setNeedToBeUpdated(true);

        // Catalog
        COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
        COSDictionary permsDict = new COSDictionary();
        catalogDict.setItem(COSName.PERMS, permsDict);
        permsDict.setItem(COSName.DOCMDP, signature);
        catalogDict.setNeedToBeUpdated(true);
        permsDict.setNeedToBeUpdated(true);
    }

    //setMDPPermission(document, signature, 2);

    public static Digest createSignature(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        Digest digest = new Digest();

        String location = "Geneva, Switzerland";

        InputStream image = classLoader.getResourceAsStream(SIGNATURE_PNG);
        PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(image);
        visibleSignDesigner.coordinates(0, -100).zoom(-93f).adjustForRotation();

        PDVisibleSigProperties visibleSignatureProperties = new PDVisibleSigProperties();
        visibleSignatureProperties
                .preferredSize(0)
                .visualSignEnabled(true)
                .setPdVisibleSignature(visibleSignDesigner);

        visibleSignatureProperties.buildSignature();

        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setLocation(location);
        signature.setSignDate(Calendar.getInstance());

        try (PDDocument document = PDDocument.load(inputStream)) {
            int pageCount = document.getPages().getCount();
            try (SignatureOptions signatureOptions = new SignatureOptions()) {
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {

                    signatureOptions.setPreferredSignatureSize(PREFERRED_SIGNATURE_SIZE);
                    signatureOptions.setVisualSignature(visibleSignatureProperties.getVisibleSignature());
                    signatureOptions.setPage(pageCount - 1);

                    document.addSignature(signature, signatureOptions);

                    ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(stream);
                    externalSigning.setSignature(new byte[0]);
                    int[] byteRange = signature.getByteRange();
                    digest.pdf = stream.toByteArray();

                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        os.write(digest.pdf, byteRange[0], byteRange[1]);
                        os.write(digest.pdf, byteRange[2], byteRange[3]);
                        digest.hash = calculateSHA2(os.toByteArray());
                    }
                }
            }
        }
        return digest;
    }

    public static void attachCMS(InputStream inputStream, File outputPdf, byte[] cms) throws IOException {
        int offset;
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDSignature signature = document.getLastSignatureDictionary();
            int[] byteRange = signature.getByteRange();
            offset = byteRange[1] + 1;
        }

        try (RandomAccessFile raf = new RandomAccessFile(outputPdf, "rw")) {
            raf.seek(offset);
            raf.write(Hex.getBytes(cms));
        }
    }

    public static String calculateSHA2(byte[] pdf) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(pdf);
        return Base64.getEncoder().encodeToString(digest);
    }

    public static File writeToFile(byte[] bytes, String pathname) throws IOException {
        File file = new File(pathname);
        try (FileOutputStream bw = new FileOutputStream(file)) {
            bw.write(bytes);
        }
        return file;
    }

    public static byte[] readFile(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return toByteArray(fis);
        }
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int n;
            byte[] buffer = new byte[1024 * 4];
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        }
    }
}
