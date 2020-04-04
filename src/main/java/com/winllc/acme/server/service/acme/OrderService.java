package com.winllc.acme.server.service.acme;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.challenge.DnsChallenge;
import com.winllc.acme.server.challenge.HttpChallenge;
import com.winllc.acme.server.contants.IdentifierType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.*;
import com.winllc.acme.server.model.data.*;
import com.winllc.acme.server.model.requestresponse.CertificateRequest;
import com.winllc.acme.server.model.requestresponse.OrderRequest;
import com.winllc.acme.server.persistence.*;
import com.winllc.acme.server.process.AuthorizationProcessor;
import com.winllc.acme.server.process.OrderProcessor;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Section 7.4
@RestController
public class OrderService extends BaseService {

    private static final Logger log = LogManager.getLogger(OrderService.class);

    @Autowired
    private OrderProcessor orderProcessor;
    @Autowired
    private OrderPersistence orderPersistence;
    @Autowired
    private ChallengePersistence challengePersistence;
    @Autowired
    private OrderListPersistence orderListPersistence;
    @Autowired
    private AccountPersistence accountPersistence;
    @Autowired
    private CertificatePersistence certificatePersistence;
    @Autowired
    private AuthorizationProcessor authorizationProcessor;
    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private SecurityValidatorUtil securityValidatorUtil;
    @Autowired
    private CertificateAuthorityService certificateAuthorityService;
    @Autowired
    private HttpChallenge httpChallenge;
    @Autowired
    private DnsChallenge dnsChallenge;
    @Autowired
    @Qualifier("appTaskExecutor")
    private TaskExecutor taskExecutor;

    @RequestMapping(value = "{directory}/new-order", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> newOrder(HttpServletRequest request, @PathVariable String directory) {
        log.info("start newOrder");
        DirectoryData directoryData = directoryDataService.findByName(directory);

        try {
            PayloadAndAccount<OrderRequest> payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, OrderRequest.class);

            OrderRequest orderRequest = payloadAndAccount.getPayload();
            AccountData accountData = payloadAndAccount.getAccountData();

            Optional<ProblemDetails> problemDetailsOptional = caCanFulfill(orderRequest, directoryData, accountData);
            if (orderRequest.isValid() && !problemDetailsOptional.isPresent()) {

                //CA can fulfill
                OrderData orderData = orderProcessor.buildNew(directoryData, accountData);
                Order order = orderData.getObject();

                order.setIdentifiers(orderRequest.getIdentifiers());
                order.setNotAfter(orderRequest.getNotAfter());
                order.setNotBefore(orderRequest.getNotBefore());

                generateAuthorizationsForOrder(orderData, payloadAndAccount);

                orderData = orderPersistence.save(orderData);

                log.info("Order created: " + orderData.getId());

                //todo move this
                authorizationProcessor.getCurrentAuthorizationsForOrder(orderData).forEach(a -> {
                    challengePersistence.findAllByAuthorizationIdEquals(a.getId()).forEach(challenge -> {
                        if(challenge.getObject().getStatus().equals(StatusType.PENDING.toString())) {
                            switch (challenge.getObject().getType()) {
                                case "http-01":
                                    httpChallenge.verify(challenge);
                                    break;
                                case "dns-01":
                                    dnsChallenge.verify(challenge);
                                    break;
                            }
                        }
                    });
                });

                //Get order list ID from account
                Optional<String> objectId = new AcmeURL(accountData.getObject().getOrders()).getObjectId();
                String orderListId = objectId.get();

                Optional<OrderListData> orderListDataOptional = orderListPersistence.findById(orderListId);
                if (orderListDataOptional.isPresent()) {
                    OrderListData orderListData = orderListDataOptional.get();
                    orderListData.addOrder(orderData);
                    orderListPersistence.save(orderListData);

                /*
                 If the server is willing to issue the requested certificate, it responds with a 201 (Created) response.
                 The body of this response is an order object reflecting the client’s request and any authorizations
                 the client must complete before the certificate will be issued.
                 */
                    log.debug("Order List created: " + orderListData);
                    return buildBaseResponseEntity(201, directoryData)
                            .header("Retry-After", "10")
                            .header("Location", orderData.buildUrl())
                            .body(order);
                } else {
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                    problemDetails.setDetail("Could not find Order List");

                    log.error(problemDetails);

                    return buildBaseResponseEntity(500, directoryData)
                            .body(problemDetails);
                }
            } else {
                /*
                The server MUST return an error if it cannot fulfill the request as specified,
                and it MUST NOT issue a certificate with contents other than those requested.
                If the server requires the request to be modified in a certain way,
                it should indicate the required changes using an appropriate error type and description.
                 */
                ProblemDetails problemDetails = problemDetailsOptional.get();
                log.error(problemDetails);
                return buildBaseResponseEntity(403, directoryData)
                        .body(problemDetails);
            }
        } catch (Exception e) {
            log.error("Could not create new-order", e);
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail(e.getMessage());
            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }
    }

    @RequestMapping(value = "{directory}/order/{id}", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> getOrder(@PathVariable String id, HttpServletRequest httpServletRequest, @PathVariable String directory) {
        log.info("getOrder: "+id);
        DirectoryData directoryData = directoryDataService.findByName(directory);

        Optional<OrderData> orderDataOptional = orderPersistence.findById(id);
        if (orderDataOptional.isPresent()) {
            OrderData orderData = orderDataOptional.get();
            orderData = orderProcessor.buildCurrentOrder(orderData);

            log.info("Returning order: "+orderData.getId() + " :: Status: "+orderData.getObject().getStatus());

            if(orderData.getObject().getStatus().equals(StatusType.PROCESSING.toString()) ||
                    orderData.getObject().getStatus().equals(StatusType.PENDING.toString())){
                return buildBaseResponseEntity(200, directoryData)
                        .header("Retry-After", "10").build();
                       // .body(orderData.getObject());
            }else{
                return buildBaseResponseEntity(200, directoryData)
                        .header("Location", orderData.buildUrl())
                        .body(orderData.getObject());
            }


        }else{
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail("Could not find order with ID: "+id);

            log.error(problemDetails);

            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }
    }

    @RequestMapping(value = "{directory}/order/{id}/finalize", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> finalizeOrder(@PathVariable String id, HttpServletRequest httpServletRequest, @PathVariable String directory) {
        log.info("finalizeOrder: "+id);
        DirectoryData directoryData = directoryDataService.findByName(directory);

        PayloadAndAccount<CertificateRequest> certificateRequestPayloadAndAccount = null;
        try {
            certificateRequestPayloadAndAccount =
                    securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(httpServletRequest, CertificateRequest.class);
        } catch (AcmeServerException e) {
            log.error(e);
            ProblemDetails problemDetails = new ProblemDetails(e.getProblemType());
            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }

        Optional<OrderData> optionalOrderData = orderPersistence.findById(id);
        OrderData orderData = orderProcessor.buildCurrentOrder(optionalOrderData.get());

        //if order ready to be completed by passing authorization checks
        if (orderReadyForFinalize(orderData.getObject())) {
            log.info("readyForFinalize: "+orderData.getId());
            String csr = certificateRequestPayloadAndAccount.getPayload().getCsr();
            try {
                Optional<ProblemDetails> problemDetailsOptional = validateCsr(csr, orderData.getObject());
                if (!problemDetailsOptional.isPresent()) {

                    orderData.getObject().setStatus(StatusType.PROCESSING.toString());
                    orderData = orderPersistence.save(orderData);

                    //if checks pass, return
                    finalizeOrder(orderData, csr);

                    log.info("Finalized order: " + orderData);

                    return buildBaseResponseEntity(200, certificateRequestPayloadAndAccount.getDirectoryData())
                            .header("Retry-After", "20")
                            .body(orderData.getObject());
                } else {
                    ProblemDetails problemDetails = problemDetailsOptional.get();

                    log.error(problemDetails);

                    return buildBaseResponseEntity(500, directoryData)
                            .body(problemDetails);
                }
            } catch (Exception e) {
                log.error("Could not finalize order", e);
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                return buildBaseResponseEntity(500, directoryData)
                        .body(problemDetails);
            }
        } else if(orderData.getObject().isValid()){
            return buildBaseResponseEntity(200, certificateRequestPayloadAndAccount.getDirectoryData())
                    .body(orderData.getObject());
        } else if(orderData.getObject().getStatus().equals(StatusType.PROCESSING.toString())){
            return buildBaseResponseEntity(200, certificateRequestPayloadAndAccount.getDirectoryData())
                    .header("Retry-After", "10")
                    .body(orderData.getObject());
        } else {
            log.error("Order not ready to finalize: "+orderData);
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.ORDER_NOT_READY);

            return buildBaseResponseEntity(403, certificateRequestPayloadAndAccount.getDirectoryData())
                    .body(problemDetails);
        }
    }

    //Section 7.1.2.1
    @RequestMapping(value = "{directory}/orders/{id}", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<?> orderList(@PathVariable String id, @PathVariable String directory, @RequestParam(required = false) Integer cursor, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();

        Optional<OrderListData> orderListDataOptional = orderListPersistence.findById(id);
        if (orderListDataOptional.isPresent()) {
            OrderListData orderListData = orderListDataOptional.get();

            DirectoryData directoryData = directoryDataService.findByName(directory);

            OrderList orderList = orderListData.getObject();
            if (cursor != null) {
                orderList = orderListData.buildPaginatedOrderList(cursor);
                Optional<String> nextPageLink = orderListData.buildPaginatedLink(cursor);
                //If a next page is available, add the link
                nextPageLink.ifPresent(s -> headers.add("Link", s));
            }

            return buildBaseResponseEntity(200, directoryData, headers)
                    .body(orderList);
        } else {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail("Could not find Order List");

            log.error(problemDetails);

            return ResponseEntity.status(500)
                    .body(problemDetails);
        }
    }

    //Return problem details if CA can't issue, return empty if can fulfill
    private Optional<ProblemDetails> caCanFulfill(OrderRequest orderRequest, DirectoryData directoryData, AccountData accountData) throws AcmeServerException {

        CertificateAuthority ca = certificateAuthorityService.getByName(directoryData.getMapsToCertificateAuthorityName());
        ProblemDetails problemDetails = new ProblemDetails(ProblemType.COMPOUND);
        //If allowed to issue to all identifiers, return true
        int allowedToIssueTo = 0;
        for (Identifier identifier : orderRequest.getIdentifiers()) {
            if (!ca.canIssueToIdentifier(identifier, accountData)) {
                ProblemDetails temp = new ProblemDetails(ProblemType.UNSUPPORTED_IDENTIFIER);
                temp.setDetail("CA can't issue for: " + identifier);
                problemDetails.addSubproblem(temp);
            }
        }

        return problemDetails.getSubproblems().length > 0 ? Optional.of(problemDetails) : Optional.empty();
    }


    //Section 8
    private void generateAuthorizationsForOrder(OrderData orderData, PayloadAndAccount payloadAndAccount) throws AcmeServerException {
        List<String> authorizationUrls = new ArrayList<>();
        Order order = orderData.getObject();

        for (Identifier identifier : order.getIdentifiers()) {
            Optional<AuthorizationData> authorizationOptional = authorizationProcessor.buildAuthorizationForIdentifier(identifier, payloadAndAccount, orderData);
            if (authorizationOptional.isPresent()) {
                AuthorizationData authorization = authorizationOptional.get();
                authorization.setOrderId(orderData.getId());
                authorizationUrls.add(authorization.buildUrl());
            }
        }

        //todo include pre-authorizations from account
        DirectoryData directoryData = payloadAndAccount.getDirectoryData();
        if(directoryData.isAllowPreAuthorization() && directoryData.getObject().getMeta().isExternalAccountRequired()){
            //todo
        }

        order.setAuthorizations(authorizationUrls.toArray(new String[0]));


    }

    private void finalizeOrder(final OrderData order, String csr) throws InternalServerException {
        /*
        “invalid”: The certificate will not be issued. Consider this order process abandoned.
        “pending”: The server does not believe that the client has fulfilled the requirements. Check the “authorizations” array for entries that are still pending.
        “ready”: The server agrees that the requirements have been fulfilled, and is awaiting finalization. Submit a finalization request.
        “processing”: The certificate is being issued. Send a POST-as-GET request after the time given in the Retry-After header field of the response, if any.
        “valid”: The server has issued the certificate and provisioned its URL to the “certificate” field of the order. Download the certificate.
         */

        Runnable issueCert = new Runnable() {
            @Override
            public void run() {
                DirectoryData directoryData = directoryDataService.findByName(order.getDirectory());
                CertificateAuthority ca = certificateAuthorityService.getByName(directoryData.getMapsToCertificateAuthorityName());

                OrderData newOrder = orderPersistence.findById(order.getId()).get();

                try {
                    Optional<AccountData> optionalAccount = accountPersistence.findById(order.getAccountId());
                    String eabKid = null;
                    if(optionalAccount.isPresent()){
                        AccountData accountData = optionalAccount.get();
                        if(StringUtils.isNotBlank(accountData.getEabKeyIdentifier())) eabKid = accountData.getEabKeyIdentifier();
                    }

                    X509Certificate certificate = ca.issueCertificate(newOrder, eabKid, CertUtil.csrBase64ToPKC10Object(csr));
                    String[] certWithChains = CertUtil.certAndChainsToPemArray(certificate, ca.getTrustChain());
                    CertData certData = new CertData(certWithChains, directoryData.getName(), newOrder.getAccountId());
                    certData = certificatePersistence.save(certData);

                    newOrder.getObject().setStatus(StatusType.VALID.toString());
                    newOrder.getObject().setCertificate(certData.buildUrl());

                    orderPersistence.save(newOrder);

                } catch (Exception e) {
                    log.error("Could not finalize order", e);
                    //throw new InternalServerException("Could not issue certificate", e);
                    newOrder.getObject().setStatus(StatusType.INVALID.toString());
                    orderPersistence.save(newOrder);
                }
            }
        };

        try {
            //Allow client to receive processing response first, then run
            Thread.sleep(5000);
            taskExecutor.execute(issueCert);
        } catch (InterruptedException e) {
            log.error("Could not sleep thread", e);
        }

    }

    private boolean orderReadyForFinalize(Order order) {
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
    private Optional<ProblemDetails> validateCsr(String csr, Order order) throws Exception {
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

        if (dnsNamesInCsr.equals(dnsNamesInOrder)) {
            return Optional.empty();
        } else {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.BAD_CSR);
            problemDetails.setDetail("CSR contains invalid identifiers");
            return Optional.of(problemDetails);
        }
    }

}
