package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.challenge.DnsChallenge;
import com.winllc.acme.server.challenge.HttpChallenge;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.Authorization;
import com.winllc.acme.server.model.Challenge;
import com.winllc.acme.server.model.Identifier;
import com.winllc.acme.server.model.ProblemDetails;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.process.AuthorizationProcessor;
import com.winllc.acme.server.util.AppUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

//Section 7.4.1
@RestController
public class AuthzService extends BaseService {

    private AuthorizationPersistence authorizationPersistence;

    @RequestMapping(value = "new-authz", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> newAuthz(HttpServletRequest request){
        try {
            PayloadAndAccount<Identifier> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, Identifier.class);

            Identifier identifier = payloadAndAccount.getPayload();

            if(serverWillingToIssueForIdentifier(identifier)){
                //TODO build authorization object
                Optional<AuthorizationData> authorizationOptional = new AuthorizationProcessor().buildAuthorizationForIdentifier(identifier, Application.directoryData);



            }else{
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNSUPPORTED_IDENTIFIER);
                //TODO fill out problem details
                return buildBaseResponseEntity(403)
                        .body(problemDetails);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Section 7.5
    @RequestMapping(value = "authz/{id}", method = RequestMethod.POST, consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> authz(HttpServletRequest request, @PathVariable String id){

        Optional<AuthorizationData> optionalAuthorizationData = authorizationPersistence.getById(id);

        if(optionalAuthorizationData.isPresent()) {
            AuthorizationData authorizationData = optionalAuthorizationData.get();

            try {
                JWSObject jwsObject = AppUtil.getJWSObjectFromHttpRequest(request);
                PayloadAndAccount<Authorization> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, Authorization.class);
                Authorization authorization = payloadAndAccount.getPayload();

                //Section 7.5.2
                if(authorization.getStatus().contentEquals(StatusType.DEACTIVATED.toString())){
                    authorizationData.getObject().setStatus(StatusType.DEACTIVATED.toString());
                    authorizationPersistence.save(authorizationData);

                    return buildBaseResponseEntity(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(authorizationData.getObject());
                }

                //TODO verify account key
                Authorization refreshedAuthorization = new AuthorizationProcessor().buildCurrentAuthorization(authorizationData);
                authorizationData.updateObject(refreshedAuthorization);

                authorizationPersistence.save(authorizationData);

                return buildBaseResponseEntity(200)
                        .header("Link", "TODO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(refreshedAuthorization);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return null;
    }

    //Section 7.5.1
    @RequestMapping(value = "chall/{id}", method = RequestMethod.POST, consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> challenge(HttpServletRequest request, @PathVariable String id){
        Optional<ChallengeData> optionalChallengeData = new ChallengePersistence().getById(id);

        if(optionalChallengeData.isPresent()){
            ChallengeData challengeData = optionalChallengeData.get();
            Challenge challenge = challengeData.getObject();

            ChallengeType challengeType = ChallengeType.valueOf(challenge.getType());
            switch (challengeType){
                case HTTP:
                    new HttpChallenge().verify(challengeData);
                    break;
                case DNS:
                    new DnsChallenge().verify(challengeData);
                    break;
            }

        }

        return null;
    }

    private boolean serverWillingToIssueForIdentifier(Identifier identifier){
        //TODO
        return false;
    }

}
