package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.contants.RevocationReason;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.AcmeURL;
import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.CertData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.requestresponse.RevokeCertRequest;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;


@RestController
public class CertService extends BaseService {

    private Logger log = LogManager.getLogger(CertService.class);

    @Autowired
    private CertificatePersistence certificatePersistence;
    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private SecurityValidatorUtil securityValidatorUtil;
    @Autowired
    private CertificateAuthorityService certificateAuthorityService;
    @Autowired
    private ExternalAccountProviderService externalAccountProviderService;

    //Section 7.4.2
    @RequestMapping(value = "{directory}/cert/{id}", method = RequestMethod.POST)
    public ResponseEntity<?> certDownload(HttpServletRequest request, @PathVariable String id, @PathVariable String directory) {
        log.info("certDownload: "+id);
        try {
            DirectoryData directoryData = directoryDataService.findByName(directory);
            Optional<CertData> optionalCertData = certificatePersistence.findById(id);

            if (optionalCertData.isPresent()) {
                CertData certData = optionalCertData.get();

                PayloadAndAccount<String> payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, String.class);

                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", request.getHeader("Accept"));

                String returnCert = String.join(LINE_SEPARATOR, certData.getObject());

                if(StringUtils.isNotBlank(request.getHeader("Accept"))) {

                    switch (request.getHeader("Accept")) {
                        case "application/pem-certificate-chain":
                            //already set
                            break;
                        case "application/pkix-cert":
                            returnCert = certData.getCertChain()[0];
                            break;
                        case "application/pkcs7-mime":

                            //TODO
                            break;
                        default:
                            returnCert = certData.buildReturnString();
                    }
                }

                log.debug("Returning certificate: "+returnCert);

                return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                        .headers(headers)
                        .body(returnCert);
            } else {
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                problemDetails.setDetail("Could not find Cert Data");

                log.error(problemDetails);

                return buildBaseResponseEntity(500, directoryData)
                        .body(problemDetails);
            }
        }catch (Exception e){
            log.error("Error retrieving Cert Data", e);
            return ResponseEntity.status(500).build();
        }
    }

    //Section 7.6
    @RequestMapping(value = "{directory}/revoke-cert", method = RequestMethod.POST,
            consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> certRevoke(HttpServletRequest request, @PathVariable String directory) throws Exception {
        DirectoryData directoryData = directoryDataService.findByName(directory);
        CertificateAuthority ca = certificateAuthorityService.getByName(directoryData.getMapsToCertificateAuthorityName());
        try {
            AcmeJWSObject jwsObject = SecurityValidatorUtil.getJWSObjectFromHttpRequest(request);

            PayloadAndAccount<RevokeCertRequest> payloadAndAccount = securityValidatorUtil
                    .verifyJWSAndReturnPayloadForExistingAccount(jwsObject, request, RevokeCertRequest.class);

            boolean signedByAccountKey = signedByAccountKeyCheck(payloadAndAccount, jwsObject);
            boolean signedByCert = signedByCertCheck(payloadAndAccount, jwsObject);

            if(signedByCert) {
                log.debug("Revocation signed by Cert");
            }else if(signedByAccountKey){
                log.debug("Revocation signed by Account Key");
            }else{
                throw new AcmeServerException(ProblemType.UNAUTHORIZED, "Not authorized to revoke certificate");
            }

            RevokeCertRequest revokeCertRequest = payloadAndAccount.getPayload();
            X509Certificate certificate = revokeCertRequest.buildX509Cert();

            if(accountCanRevokeCertificate(certificate, payloadAndAccount.getAccountData())) {

                Optional<ProblemDetails> problemDetailsOptional = validateRevocationRequest(revokeCertRequest);
                if (!problemDetailsOptional.isPresent()) {

                    if (ca.revokeCertificate(certificate, revokeCertRequest.getReason())) {
                        return buildBaseResponseEntity(200, directoryData).build();
                    } else {
                        ProblemDetails error = new ProblemDetails(ProblemType.ALREADY_REVOKED);

                        return buildBaseResponseEntity(400, payloadAndAccount.getDirectoryData())
                                .body(error);
                    }
                } else {
                    return buildBaseResponseEntity(500, directoryData)
                            .body(problemDetailsOptional.get());
                }
            }else{
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNAUTHORIZED);
                problemDetails.setDetail("Account not authorized to revoke certificate");
                return buildBaseResponseEntity(401, directoryData)
                        .body(problemDetails);
            }

        } catch (Exception e) {
            ProblemDetails error = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            error.setDetail(e.getMessage());

            log.error(error, e);

            return buildBaseResponseEntity(500, directoryData)
                    .body(error);
        }
    }

    private boolean signedByCertCheck(PayloadAndAccount<RevokeCertRequest> payloadAndAccount, JWSObject jwsObject) throws CertificateException, IOException, JOSEException {
        RevokeCertRequest revokeCertRequest = payloadAndAccount.getPayload();
        X509Certificate certificate = revokeCertRequest.buildX509Cert();

        JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) certificate.getPublicKey());
        return jwsObject.verify(verifier);
    }

    private boolean signedByAccountKeyCheck(PayloadAndAccount<RevokeCertRequest> payloadAndAccount, JWSObject jwsObject) throws ParseException, JOSEException {

        AccountData accountData = payloadAndAccount.getAccountData();

        RSAKey rsaKey = RSAKey.parse(accountData.buildJwk().toJSONObject());

        JWSVerifier verifier = new RSASSAVerifier(rsaKey);
        return jwsObject.verify(verifier);
    }

    private Optional<ProblemDetails> validateRevocationRequest(RevokeCertRequest request){
        boolean valid = true;

        if(request.getReason() != null){
            RevocationReason reason = RevocationReason.fromCode(request.getReason());
            if(reason == null) valid = false;
        }

        return valid ?  Optional.empty() : Optional.of(new ProblemDetails(ProblemType.BAD_REVOCATION_REASON));
    }


    /*
    Before revoking a certificate, the server MUST verify that the key used to sign the request is authorized to revoke the certificate.
    The server MUST consider at least the following accounts authorized for a given certificate:

    the account that issued the certificate.
    an account that holds authorizations for all of the identifiers in the certificate.
    The server MUST also consider a revocation request valid if it is signed with the private key corresponding to the public key in the certificate.
     */
    private boolean accountCanRevokeCertificate(X509Certificate certificate, AccountData accountData) throws Exception {
        List<String> dnsListInCert = CertUtil.getDNSSubjectAlts(certificate);

        if(StringUtils.isNotBlank(accountData.getEabKeyIdentifier())){
            //If account tied to an external account, verify
            DirectoryData directoryData = directoryDataService.findByName(accountData.getDirectory());
            ExternalAccountProvider accountProvider = externalAccountProviderService.findByName(directoryData.getExternalAccountProviderName());

            List<String> canIssueToDomains = accountProvider.getCanIssueToDomainsForExternalAccount(accountData.getEabKeyIdentifier());

            boolean allValid = true;
            for(String certDns : dnsListInCert){
                boolean valid = false;
                for(String canIssueDomain : canIssueToDomains){
                    if(certDns.endsWith(canIssueDomain)){
                        valid = true;
                        break;
                    }
                }
                if(!valid){
                    allValid = false;
                    break;
                }
            }

            return allValid;
        }

        return true;
    }

}
