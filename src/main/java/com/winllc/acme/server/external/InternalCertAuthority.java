package com.winllc.acme.server.external;

import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.IdentifierType;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.OrderData;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InternalCertAuthority implements CertificateAuthority {

    private String name;
    private String caKeystorePassword = "password";
    private String caKeystoreLocation = "C:\\Users\\jrmints\\IdeaProjects\\ACME Server\\src\\main\\resources\\internal-ca\\intermediate_ca.pfx";
    private String caKeystoreAlias = "alias";

    private List<X509Certificate> issuedCerts = new ArrayList<>();
    private List<X509Certificate> revokedCerts = new ArrayList<>();

    public InternalCertAuthority(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean revokeCertificate(X509Certificate certificate, int reason) {
        //if(listContainsCert(issuedCerts, certificate) && !listContainsCert(revokedCerts, certificate)){
            revokedCerts.add(certificate);
            return true;
        //}
        //TODO
        //return false;
    }

    private boolean listContainsCert(List<X509Certificate> certs, X509Certificate certToCheck){
        for(X509Certificate certificate : certs){
            if(certificate.getPublicKey().equals(certToCheck.getPublicKey())){
                return true;
            }
        }
        return false;
    }

    @Override
    public X509Certificate issueCertificate(OrderData orderData, PKCS10CertificationRequest certificationRequest) {
        try {
            KeyStore ks = loadKeystore(caKeystoreLocation, caKeystorePassword);
            return signCSR(orderData, certificationRequest, 30, ks, caKeystoreAlias, caKeystorePassword.toCharArray());
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Certificate[] getTrustChain() {
        try {
            KeyStore keyStore = loadKeystore(caKeystoreLocation, caKeystorePassword);
            Certificate[] chain = keyStore.getCertificateChain(caKeystoreAlias);
            return chain;
        }catch (Exception e){
            e.printStackTrace();
        }

        return new Certificate[0];
    }

    @Override
    public boolean isCertificateRevoked(X509Certificate certificate) {
        //TODO
        return false;
    }

    @Override
    public List<CAValidationRule> getValidationRules() {
        //todo
        CAValidationRule rule = new CAValidationRule();
        rule.setBaseDomainName("winllc.com");
        rule.setAllowIssuance(true);
        rule.setRequireHttpChallenge(true);
        rule.setIdentifierType(IdentifierType.DNS.toString());

        CAValidationRule rule2 = new CAValidationRule();
        rule2.setAllowHostnameIssuance(true);

        return Stream.of(rule, rule2).collect(Collectors.toList());
    }


    @Override
    public boolean canIssueToIdentifier(Identifier identifier) {
        //no rules, can issue
        if(getValidationRules().size() == 0) return true;

        for (CAValidationRule rule : getValidationRules()) {
            if(rule.canIssueToIdentifier(identifier)){
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier) {
        Set<ChallengeType> challengeTypes = new HashSet<>();
        if(canIssueToIdentifier(identifier)){
            for (CAValidationRule rule : getValidationRules()) {
                if(rule.canIssueToIdentifier(identifier)){
                    if(rule.isRequireHttpChallenge()) challengeTypes.add(ChallengeType.HTTP);
                    if(rule.isRequireDnsChallenge()) challengeTypes.add(ChallengeType.DNS);
                }
            }
            return new ArrayList<>(challengeTypes);
        }
        return null;
    }

    private X509Certificate signCSR(OrderData orderData, PKCS10CertificationRequest csr, int validity, KeyStore keystore, String alias, char[] password) throws Exception {
        try {
            Security.addProvider(new BouncyCastleProvider());

            PrivateKey cakey = (PrivateKey) keystore.getKey(alias, password);
            Certificate[] chain = keystore.getCertificateChain(alias);
            Certificate root = chain[1];
            X509Certificate intermediate = (X509Certificate) chain[0];

            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            X500Name issuer = new JcaX509CertificateHolder(intermediate).getSubject();
            BigInteger serial = new BigInteger(16, new SecureRandom());
            Date from = new Date();
            Date to = new Date(System.currentTimeMillis() + (validity * 86400000L));


            X509v3CertificateBuilder certgen = new X509v3CertificateBuilder(issuer, serial, from, to,
                    new X500Name("cn=" + orderData.getId()), csr.getSubjectPublicKeyInfo());
            certgen.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(false));
            //certgen.addExtension(X509Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));

            certgen.addExtension(X509Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(intermediate.getPublicKey()));
            List<Integer> keyUsages = new ArrayList<Integer>();

            keyUsages.add(KeyUsage.digitalSignature);

            if (keyUsages.size() == 0) {
                keyUsages.add(KeyUsage.digitalSignature);
            }

            Iterator<Integer> usageIterable = keyUsages.iterator();
            int usageBit = 0;
            while (usageIterable.hasNext()) {
                Integer usage = usageIterable.next();
                usageBit = usageBit | usage;
            }

            certgen.addExtension(X509Extension.keyUsage, false, new KeyUsage(usageBit));
            List<KeyPurposeId> keyPurposeIds = new ArrayList<>();
            keyPurposeIds.add(KeyPurposeId.id_kp_clientAuth);
            keyPurposeIds.add(KeyPurposeId.id_kp_serverAuth);

            if (keyPurposeIds.size() > 0) {
                org.bouncycastle.asn1.x509.ExtendedKeyUsage eku =
                        new org.bouncycastle.asn1.x509.ExtendedKeyUsage(keyPurposeIds.toArray(new KeyPurposeId[0]));
                certgen.addExtension(X509Extension.extendedKeyUsage, false, eku);
            }

            List<GeneralName> generalNameList = new ArrayList<>();
            for (Identifier identifier : orderData.getObject().getIdentifiers()) {
                GeneralName altName = new GeneralName(GeneralName.dNSName, identifier.getValue());
                generalNameList.add(altName);
            }

            GeneralNames subjectAltName = new GeneralNames(generalNameList.toArray(new GeneralName[0]));
            certgen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);

            AsymmetricKeyParameter foo = PrivateKeyFactory.createKey(cakey.getEncoded());
            ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(foo);

            X509CertificateHolder holder = certgen.build(signer);
            byte[] certencoded = holder.toASN1Structure().getEncoded();

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            signer = new JcaContentSignerBuilder("SHA256withRSA").build(cakey);
            generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(signer, intermediate));
            generator.addCertificate(new X509CertificateHolder(certencoded));
            generator.addCertificate(new X509CertificateHolder(intermediate.getEncoded()));
            generator.addCertificate(new X509CertificateHolder(root.getEncoded()));
            CMSTypedData content = new CMSProcessableByteArray(certencoded);
            CMSSignedData signeddata = generator.generate(content, true);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("-----BEGIN PKCS #7 SIGNED DATA-----\n".getBytes("ISO-8859-1"));
            out.write(Base64.encode(signeddata.getEncoded()));
            out.write("\n-----END PKCS #7 SIGNED DATA-----\n".getBytes("ISO-8859-1"));
            out.close();
            //return new String(out.toByteArray(), "ISO-8859-1");
            X509CertificateHolder x509CertificateHolder = new X509CertificateHolder(certencoded);
            X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider("BC")
                    .getCertificate(x509CertificateHolder);
            //internal store only
            issuedCerts.add(issuedCert);
            return issuedCert;
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    private KeyStore loadKeystore(String location, String password) throws Exception {
        System.out.println("Loading keystore: "+location);
        FileInputStream fis = new FileInputStream(location);

        KeyStore ks = KeyStore.getInstance("PKCS12");

        ks.load(fis, password.toCharArray());
        IOUtils.closeQuietly(fis);
        return ks;
    }
}
