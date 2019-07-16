package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.RevocationReason;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.CertData;
import com.winllc.acme.server.model.requestresponse.RevokeCertRequest;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.util.AppUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
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

    //Section 7.4.2
    @RequestMapping(value = "cert/{id}", method = RequestMethod.POST,
            consumes = "application/jose+json")
    public ResponseEntity<?> certDownload(HttpServletRequest request, @PathVariable String id) {

        Optional<CertData> optionalCertData = new CertificatePersistence().getById(id);

        if(optionalCertData.isPresent()) {
            CertData certData = optionalCertData.get();

            try {
                PayloadAndAccount<String> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, String.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String returnCert = null;

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", request.getHeader("Accept"));

            switch (request.getHeader("Accept")){
                case "application/pem-certificate-chain":
                    returnCert = certData.buildReturnString();

                    break;
                case "application/pkix-cert":
                    returnCert = certData.getCertChain()[0];
                    break;
                case "application/pkcs7-mime":

                    break;
            }

            return buildBaseResponseEntity(200)
                    .headers(headers)
                    .body(returnCert);
        }else{
            //TODO return error
            return null;
        }
    }

    //Section 7.6
    @RequestMapping(value = "revoke-cert", method = RequestMethod.POST,
            consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> certRevoke(HttpServletRequest request, @PathVariable String id) {

        CertificateAuthority ca = Application.ca;

        try {
            //TODO verify signature from either account key or certificate
            PayloadAndAccount<RevokeCertRequest> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, RevokeCertRequest.class);
            RevokeCertRequest revokeCertRequest = payloadAndAccount.getPayload();

            //TODO before revoking, ensure account owns all of the identifiers associated with the certificate
            X509Certificate certificate = revokeCertRequest.buildX509Cert();

            if(validateRevocationRequest(revokeCertRequest)) {

                if (ca.revokeCertificate(certificate, revokeCertRequest.getReason())) {
                    return ResponseEntity.ok().build();
                } else {
                    ProblemDetails error = new ProblemDetails(ProblemType.ALREADY_REVOKED);

                    return buildBaseResponseEntity(400)
                            .body(error);
                }
            }else{
                ProblemDetails error = new ProblemDetails(ProblemType.BAD_REVOCATION_REASON);
                //TODO return
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    private boolean validateRevocationRequest(RevokeCertRequest request){
        boolean valid = true;

        if(request.getReason() != null){
            RevocationReason reason = RevocationReason.fromCode(request.getReason());
            if(reason == null) valid = false;
        }

        return valid;
    }

}
