package com.winllc.acme.server.util;

import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.Order;
import com.winllc.acme.server.model.data.CertData;
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
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CertUtil {


    public static X509Certificate base64ToCert(String certB64) throws CertificateException {
        byte[] encodedCert = Base64.getDecoder().decode(certB64);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(encodedCert);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(inputStream);
    }

    public static PKCS10CertificationRequest csrBase64ToPKC10Object(String csrBase64) throws AcmeServerException {
        try {
            Reader csrReader = new StringReader(csrBase64);
            try (PEMParser pemParser = new PEMParser(csrReader)) {
                Object pemObj = pemParser.readObject();
                if (pemObj instanceof PKCS10CertificationRequest) {
                    PKCS10CertificationRequest csr = (PKCS10CertificationRequest) pemObj;
                    return csr;
                }
            }
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

    public static String[] certAndChainsToPemArray(X509Certificate certificate, X509Certificate[] chain) throws CertificateEncodingException {
        String[] full = new String[chain.length + 1];
        full[0] = convertToPem(certificate);
        for(int i = 0; i < chain.length; i++){
            full[i + 1] = convertToPem(chain[i]);
        }
        return full;
    }

    protected static String convertToPem(X509Certificate cert) throws CertificateEncodingException {
        Base64.Encoder encoder = Base64.getEncoder();
        String cert_begin = "-----BEGIN CERTIFICATE-----\n";
        String end_cert = "-----END CERTIFICATE-----";

        byte[] derCert = cert.getEncoded();
        String pemCertPre = new String(encoder.encode(derCert));
        return cert_begin + pemCertPre + end_cert;
    }
}
