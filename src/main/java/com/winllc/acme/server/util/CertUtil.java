package com.winllc.acme.server.util;

import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;

import java.io.*;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CertUtil {


    public static X509Certificate base64ToCert(String certB64) throws CertificateException, IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] encodedCert = decoder.decodeBuffer(certB64);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(encodedCert);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(inputStream);
    }

    public static PKCS10CertificationRequest csrBase64ToPKC10Object(String csrBase64) throws AcmeServerException {
        try {
            byte[] base64Encoded = csrBase64.getBytes();
            org.apache.commons.codec.binary.Base64 b64 = new org.apache.commons.codec.binary.Base64();
            byte[] data = b64.decode(base64Encoded);
            return new PKCS10CertificationRequest(data);
/*
            Reader csrReader = new StringReader(csrBase64);
            try (PEMParser pemParser = new PEMParser(csrReader)) {
                Object pemObj = pemParser.readObject();
                if (pemObj instanceof PKCS10CertificationRequest) {
                    return (PKCS10CertificationRequest) pemObj;
                }
            }

 */
        } catch (IOException ex) {
            //LOG.error("getPKCS10CertRequest: unable to parse csr: " + ex.getMessage());
        }
        throw new AcmeServerException(ProblemType.BAD_CSR);
    }

    public static List<String> extractX509CSRDnsNames(String csr) throws AcmeServerException {

        PKCS10CertificationRequest certReq = csrBase64ToPKC10Object(csr);

        List<String> dnsNames = new ArrayList<>();
        Attribute[] attributes = certReq.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
        for (Attribute attribute : attributes) {
            for (ASN1Encodable value : attribute.getAttributeValues()) {
                Extensions extensions = Extensions.getInstance(value);
                GeneralNames gns = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
                for (GeneralName name : gns.getNames()) {
                    if (name.getTagNo() == GeneralName.dNSName) {
                        dnsNames.add(((DERIA5String) name.getName()).getString());
                    }
                }
            }
        }
        return dnsNames;
    }

    public static String[] certAndChainsToPemArray(X509Certificate certificate, Certificate[] chain) throws CertificateEncodingException {
        String[] full = new String[chain.length + 1];
        full[0] = convertToPem(certificate);
        for(int i = 0; i < chain.length; i++){
            full[i + 1] = convertToPem(chain[i]);
        }
        return full;
    }

    public static String convertToPem(Certificate cert) throws CertificateEncodingException {
        BASE64Encoder encoder = new BASE64Encoder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(X509Factory.BEGIN_CERT);
        stringBuilder.append("\n");
        stringBuilder.append(encoder.encode(cert.getEncoded()));
        stringBuilder.append("\n");
        stringBuilder.append(X509Factory.END_CERT);
        return stringBuilder.toString().replaceAll("(?m)^[ \t]*\r?\n", "");
        /*
        Base64.Encoder encoder = Base64.getEncoder();
        String cert_begin = "-----BEGIN CERTIFICATE-----\n";
        String end_cert = "-----END CERTIFICATE-----";

        byte[] derCert = cert.getEncoded();
        String pemCertPre = new String(encoder.encode(derCert));
        return cert_begin + pemCertPre + end_cert;
         */
    }
}
