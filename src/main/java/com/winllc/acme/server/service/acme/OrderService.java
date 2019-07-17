package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.IdentifierType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.CAValidationRule;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.acme.Order;
import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.*;
import com.winllc.acme.server.model.requestresponse.CertificateRequest;
import com.winllc.acme.server.model.requestresponse.OrderRequest;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.persistence.OrderListPersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.process.AuthorizationProcessor;
import com.winllc.acme.server.process.OrderProcessor;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.util.AppUtil;
import com.winllc.acme.server.util.CertUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//Section 7.4
public class OrderService extends BaseService {

    private OrderProcessor orderProcessor;
    private OrderPersistence orderPersistence;
    private OrderListPersistence orderListPersistence;
    private CertificatePersistence certificatePersistence;
    private AccountPersistence accountPersistence;

    @RequestMapping(value = "new-order", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> newOrder(HttpServletRequest request){

        try {
            PayloadAndAccount<OrderRequest> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, OrderRequest.class);
            AcmeURL acmeURL = new AcmeURL(request);
            DirectoryData directoryData = Application.directoryDataMap.get(acmeURL.getDirectoryIdentifier());

            OrderRequest orderRequest = payloadAndAccount.getPayload();
            AccountData accountData = payloadAndAccount.getAccountData();

            if(orderRequest.isValid() && caCanFulfill(orderRequest, directoryData)){

                //CA can fulfill
                OrderData orderData = orderProcessor.buildNew(directoryData);
                Order order = orderData.getObject();

                generateAuthorizationsForOrder(order, directoryData);

                order.setIdentifiers(orderRequest.getIdentifiers());
                order.setNotAfter(orderRequest.getNotAfter());
                order.setNotBefore(orderRequest.getNotBefore());

                orderData = orderPersistence.save(orderData);

                //Get order list ID from account
                Optional<String> objectId = new AcmeURL(accountData.getObject().getOrders()).getObjectId();
                String orderListId = objectId.get();

                OrderListData orderListData = orderListPersistence.getFromId(orderListId);
                orderListData.addOrder(orderData);
                orderListPersistence.save(orderListData);

                accountPersistence.save(accountData);

                /*
                 If the server is willing to issue the requested certificate, it responds with a 201 (Created) response.
                 The body of this response is an order object reflecting the client’s request and any authorizations
                 the client must complete before the certificate will be issued.
                 */
                return buildBaseResponseEntity(201, directoryData)
                        .body(order);
            }else{
                //TODO CA can't fulfill
                /*
                The server MUST return an error if it cannot fulfill the request as specified,
                and it MUST NOT issue a certificate with contents other than those requested.
                If the server requires the request to be modified in a certain way,
                it should indicate the required changes using an appropriate error type and description.
                 */
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @RequestMapping(value = "order/{id}/finalize", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> finalizeOrder(@PathVariable String id, HttpServletRequest httpServletRequest) {

        PayloadAndAccount<CertificateRequest> certificateRequestPayloadAndAccount = null;
        try {
            certificateRequestPayloadAndAccount =
                    AppUtil.verifyJWSAndReturnPayloadForExistingAccount(httpServletRequest, CertificateRequest.class);
        } catch (AcmeServerException e) {
            //TODO return exception
        }

        Optional<OrderData> optionalOrderData = new OrderPersistence().getById(id);
        OrderData orderData = orderProcessor.buildCurrentOrder(optionalOrderData.get());

        //if order ready to be completed by passing authorization checks
        if(orderReadyForFinalize(orderData.getObject())){
            //TODO get csr from payload
            String csr = certificateRequestPayloadAndAccount.getPayload().getCsr();
            try {
                if (validateCsr(csr, orderData.getObject())) {
                    //if checks pass, return

                    finalizeOrder(orderData, csr);

                    //TODO update the Order object
                    orderPersistence.save(orderData);

                    return buildBaseResponseEntity(200, certificateRequestPayloadAndAccount.getDirectoryData())
                            .body(orderData.getObject());
                }else{
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.BAD_CSR);
                    problemDetails.setDetail("why it failed");
                    //TODO

                }
            } catch (AcmeServerException e) {
                e.printStackTrace();
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.BAD_CSR);
                //TODO
            }
        }else{
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.ORDER_NOT_READY);

            return buildBaseResponseEntity(403, certificateRequestPayloadAndAccount.getDirectoryData())
                    .body(problemDetails);
        }

        return null;
    }

    //Section 7.1.2.1
    @RequestMapping(value = "orders/{id}", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<?> orderList(@PathVariable String id, HttpServletRequest request){
        //TODO add 'paging'

        OrderListData orderListData = new OrderListPersistence().getFromId(id);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Link", "TODO");
        headers.add("Link", "TODO");

        return ResponseEntity.status(200)
                .headers(headers)
                .body(orderListData.buildOrderList());
    }


    private boolean caCanFulfill(OrderRequest orderRequest, DirectoryData directoryData){

        CertificateAuthority ca = Application.availableCAs.get(directoryData.getMapsToCertificateAuthorityName());

        //If allowed to issue to all identifiers, return true
        int allowedToIssueTo = 0;
        for(Identifier identifier : orderRequest.getIdentifiers()) {
            for (CAValidationRule rule : ca.getValidationRules()) {
                if(rule.canIssueToIdentifier(identifier)){
                    allowedToIssueTo++;
                    break;
                }
            }
        }

        return allowedToIssueTo == orderRequest.getIdentifiers().length;
    }


    //Section 8
    private void generateAuthorizationsForOrder(Order order, DirectoryData directoryData){
        //TODO

        AuthorizationProcessor authorizationProcessor = new AuthorizationProcessor();

        List<String> authorizationUrls = new ArrayList<>();

        for(Identifier identifier : order.getIdentifiers()){
            //TODO don't use static directoryData
            Optional<AuthorizationData> authorizationOptional = authorizationProcessor.buildAuthorizationForIdentifier(identifier, directoryData);
            if(authorizationOptional.isPresent()){
                AuthorizationData authorization = authorizationOptional.get();
                authorizationUrls.add(authorization.buildUrl());
            }
        }

        order.setAuthorizations(authorizationUrls.toArray(new String[0]));
    }

    private void finalizeOrder(OrderData order, String csr){
        /*
        “invalid”: The certificate will not be issued. Consider this order process abandoned.
        “pending”: The server does not believe that the client has fulfilled the requirements. Check the “authorizations” array for entries that are still pending.
        “ready”: The server agrees that the requirements have been fulfilled, and is awaiting finalization. Submit a finalization request.
        “processing”: The certificate is being issued. Send a POST-as-GET request after the time given in the Retry-After header field of the response, if any.
        “valid”: The server has issued the certificate and provisioned its URL to the “certificate” field of the order. Download the certificate.
         */

        DirectoryData directoryData = Application.directoryDataMap.get(order.getDirectory());
        CertificateAuthority ca = new CertificateAuthorityService().getByDirectoryData(directoryData);

        try {
            X509Certificate certificate = ca.issueCertificate(CertUtil.csrBase64ToPKC10Object(csr));
            String[] certWithChains = CertUtil.certAndChainsToPemArray(certificate, ca.getTrustChain());
            CertData certData = new CertData(certWithChains, directoryData);
            certData = certificatePersistence.save(certData);

            order.getObject().setStatus(StatusType.VALID.toString());
            order.getObject().setCertificate(certData.buildUrl());

        }catch (Exception e){
            //TODO, but should not fail from CSR parsing at this point
        }
    }

    private boolean orderReadyForFinalize(Order order){
        //TODO other checks
        return order.getStatus().contentEquals(StatusType.READY.toString());
    }

    //CSR must contain same identifiers as order
    /*
    Can reject if:
    If the CSR and order identifiers differ
    If the account is not authorized for the identifiers indicated in the CSR
    If the CSR requests extensions that the CA is not willing to include
     */
    private boolean validateCsr(String csr, Order order) throws AcmeServerException {
        List<String> dnsNamesInCsr = CertUtil.extractX509CSRDnsNames(csr).stream()
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.toList());
        List<String> dnsNamesInOrder = Arrays.stream(order.getIdentifiers())
                .filter(i -> i.getType().contentEquals(IdentifierType.DNS.toString()))
                .map(Identifier::getValue)
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.toList());

        return dnsNamesInCsr.equals(dnsNamesInOrder);
    }

}
