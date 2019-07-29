package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.RevocationReason;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.CertData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.requestresponse.RevokeCertRequest;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.util.AppUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
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
import java.security.cert.X509Certificate;
import java.util.Optional;


@RestController
public class CertService extends BaseService {

    private Logger log = LogManager.getLogger(CertService.class);

    @Autowired
    private CertificatePersistence certificatePersistence;

    //Section 7.4.2
    @RequestMapping(value = "{directory}/cert/{id}", method = RequestMethod.POST,
            consumes = "application/jose+json")
    public ResponseEntity<?> certDownload(HttpServletRequest request, @PathVariable String id, @PathVariable String directory) throws AcmeServerException {
        try {
            AcmeURL acmeURL = new AcmeURL(request);
            DirectoryData directoryData = Application.directoryDataMap.get(acmeURL.getDirectoryIdentifier());

            Optional<CertData> optionalCertData = certificatePersistence.getById(id);

            if (optionalCertData.isPresent()) {
                CertData certData = optionalCertData.get();

                PayloadAndAccount<String> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, String.class);

                String returnCert = null;

                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", request.getHeader("Accept"));

                switch (request.getHeader("Accept")) {
                    case "application/pem-certificate-chain":
                        returnCert = certData.buildReturnString();
                        break;
                    case "application/pkix-cert":
                        returnCert = certData.getCertChain()[0];
                        break;
                    case "application/pkcs7-mime":
                        //TODO
                        break;
                }

                return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                        .headers(headers)
                        .body(returnCert);
            } else {
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                problemDetails.setDetail("Could not find Cert Data");
                return buildBaseResponseEntity(500, directoryData)
                        .body(problemDetails);
            }
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    //Section 7.6
    @RequestMapping(value = "{directory}/revoke-cert", method = RequestMethod.POST,
            consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> certRevoke(HttpServletRequest request, @PathVariable String id) {
        AcmeURL acmeURL = new AcmeURL(request);
        DirectoryData directoryData = Application.directoryDataMap.get(acmeURL.getDirectoryIdentifier());
        CertificateAuthority ca = Application.availableCAs.get(directoryData.getMapsToCertificateAuthorityName());
        try {
            //TODO verify signature from either account key or certificate
            JWSObject jwsObject = AppUtil.getJWSObjectFromHttpRequest(request);
            PayloadAndAccount<RevokeCertRequest> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, RevokeCertRequest.class);
            RevokeCertRequest revokeCertRequest = payloadAndAccount.getPayload();

            boolean signedByCert = true;
            if(signedByCert) {
                JWSVerifier verifier = new RSASSAVerifier((RSAKey) jwsObject.getHeader().getJWK().toPublicJWK());
            }else{

            }

            //TODO before revoking, ensure account owns all of the identifiers associated with the certificate
            X509Certificate certificate = revokeCertRequest.buildX509Cert();

            Optional<ProblemDetails> problemDetailsOptional = validateRevocationRequest(revokeCertRequest);
            if(!problemDetailsOptional.isPresent()) {

                if (ca.revokeCertificate(certificate, revokeCertRequest.getReason())) {
                    return buildBaseResponseEntity(200, directoryData).build();
                } else {
                    ProblemDetails error = new ProblemDetails(ProblemType.ALREADY_REVOKED);

                    return buildBaseResponseEntity(400, payloadAndAccount.getDirectoryData())
                            .body(error);
                }
            }else{
                return buildBaseResponseEntity(500, directoryData)
                        .body(problemDetailsOptional.get());
            }

        } catch (Exception e) {
            log.error(e);

            ProblemDetails error = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            error.setDetail(e.getMessage());
            return buildBaseResponseEntity(500, directoryData)
                    .body(error);
        }
    }

    private Optional<ProblemDetails> validateRevocationRequest(RevokeCertRequest request){
        boolean valid = true;

        if(request.getReason() != null){
            RevocationReason reason = RevocationReason.fromCode(request.getReason());
            if(reason == null) valid = false;
        }

        return valid ?  Optional.empty() : Optional.of(new ProblemDetails(ProblemType.BAD_REVOCATION_REASON));
    }

}
