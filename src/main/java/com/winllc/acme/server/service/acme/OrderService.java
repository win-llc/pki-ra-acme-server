package com.winllc.acme.server.service.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.contants.IdentifierType;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.common.model.acme.*;
import com.winllc.acme.common.model.data.*;
import com.winllc.acme.common.model.requestresponse.CertificateRequest;
import com.winllc.acme.common.model.requestresponse.OrderRequest;
import com.winllc.acme.server.persistence.*;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.transaction.AcmeTransactionManagement;
import com.winllc.acme.server.transaction.CertIssuanceTransaction;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//Section 7.4
@RestController
public class OrderService extends BaseService {

    private static final Logger log = LogManager.getLogger(OrderService.class);

    @Autowired
    private OrderPersistence orderPersistence;
    @Autowired
    private OrderListPersistence orderListPersistence;
    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private SecurityValidatorUtil securityValidatorUtil;

    @Autowired
    private AcmeTransactionManagement acmeTransactionManagement;

    @Autowired
    @Qualifier("appTaskExecutor")
    private TaskExecutor taskExecutor;

    @RequestMapping(value = "{directory}/new-order", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> newOrder(HttpServletRequest request, @PathVariable String directory) {
        log.info("STEP ONE - NEW ORDER");
        Optional<DirectoryData> directoryDataOptional = directoryDataService.getByName(directory);
        DirectoryData directoryData = directoryDataOptional.orElseThrow(() -> new RuntimeException("Could not find DirectoryData"));

        try {
            PayloadAndAccount<OrderRequest> payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, OrderRequest.class);

            OrderRequest orderRequest = payloadAndAccount.getPayload();
            AccountData accountData = payloadAndAccount.getAccountData();

            //todo use this as primary handler
            CertIssuanceTransaction transaction = acmeTransactionManagement.startNewOrder(accountData, directoryData);
            OrderData orderData = transaction.startOrder(orderRequest);

            log.info("Location URL: "+orderData.buildUrl(Application.baseURL));

            return buildBaseResponseEntity(201, directoryData)
                    .header("Retry-After", "10")
                    .header("Location", orderData.buildUrl(Application.baseURL))
                    .body(orderData.getObject());
        } catch (Exception e) {
            log.error("Could not create new-order", e);
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail(e.getMessage());
            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }
    }

    @RequestMapping(value = "{directory}/order/{id}", consumes = "application/jose+json")
    public ResponseEntity<?> getOrder(@PathVariable String id, @PathVariable String directory) throws JsonProcessingException {
        log.info("STEP FOUR and SIX - ORDER STATUS");
        Optional<DirectoryData> directoryDataOptional = directoryDataService.getByName(directory);
        DirectoryData directoryData = directoryDataOptional.orElseThrow(() -> new RuntimeException("Could not find DirectoryData"));

        Optional<OrderData> orderDataOptional = orderPersistence.findById(id);
        if (orderDataOptional.isPresent()) {

            OrderData orderData = orderDataOptional.get();
            CertIssuanceTransaction transaction = acmeTransactionManagement.getTransaction(
                    orderData.getTransactionId(), CertIssuanceTransaction.class);

            orderData = transaction.getOrderData();

            log.info("Returning order: " + orderData.getId() + " :: Status: " + orderData.getObject().getStatus());

            Order order = orderData.getObject();
            String status = order.getStatus();

            //debugging
            ObjectMapper mapper = new ObjectMapper();
            log.info("Location URL: "+orderData.buildUrl(Application.baseURL));
            log.info("Order obj: "+mapper.writeValueAsString(order));

            if (status.equals(StatusType.PROCESSING.toString())) {

                return buildBaseResponseEntityWithRetryAfter(200, directoryData, 10)
                        .header("Location", orderData.buildUrl(Application.baseURL))
                        //.header("Retry-After", "10")
                        //.build();
                 .body(order);
            } else {
                return buildBaseResponseEntity(200, directoryData)
                        .header("Location", orderData.buildUrl(Application.baseURL))
                        .body(order);
            }
        } else {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail("Could not find order with ID: " + id);

            log.error(problemDetails);

            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }
    }

    @RequestMapping(value = "{directory}/order/{id}/finalize", consumes = "application/jose+json")
    public ResponseEntity<?> finalizeOrder(@PathVariable String id, HttpServletRequest httpServletRequest, @PathVariable String directory) {
        log.info("STEP FIVE - FINALIZE ORDER");
        Optional<DirectoryData> directoryDataOptional = directoryDataService.getByName(directory);
        DirectoryData directoryData = directoryDataOptional.orElseThrow(() -> new RuntimeException("Could not find DirectoryData"));

        PayloadAndAccount<CertificateRequest> certificateRequestPayloadAndAccount;
        try {
            certificateRequestPayloadAndAccount =
                    securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(httpServletRequest, CertificateRequest.class);
        } catch (AcmeServerException e) {
            log.error("Could not verify CertificateRequest", e);
            ProblemDetails problemDetails = new ProblemDetails(e.getProblemType());
            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }

        Optional<OrderData> optionalOrderData = orderPersistence.findById(id);
        OrderData orderData = optionalOrderData.get();

        CertIssuanceTransaction transaction = acmeTransactionManagement.getTransaction(
                orderData.getTransactionId(), CertIssuanceTransaction.class);
        orderData = transaction.getOrderData();
        //if order ready to be completed by passing authorization checks
        if (orderReadyForFinalize(orderData.getObject())) {
            log.info("readyForFinalize: " + orderData.getId());
            String csr = certificateRequestPayloadAndAccount.getPayload().getCsr();
            try {
                Optional<ProblemDetails> problemDetailsOptional = validateCsr(csr, orderData.getObject());
                if (!problemDetailsOptional.isPresent()) {

                    //orderData.getObject().setStatus(StatusType.PROCESSING.toString());
                    //orderData = orderPersistence.save(orderData);

                    //if checks pass, return
                    //finalizeOrder(orderData, csr);
                    transaction.finalizeOrder(csr);
                    orderData = transaction.getOrderData();
                    Order order = orderData.getObject();

                    ObjectMapper objectMapper = new ObjectMapper();

                    log.info("Finalized order: " + objectMapper.writeValueAsString(order));

                    return buildBaseResponseEntityWithRetryAfter(200, certificateRequestPayloadAndAccount.getDirectoryData(), 10)
                            .header("Location", orderData.buildUrl(Application.baseURL))
                            .body(order);
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
        } else if (orderData.getObject().isValid()) {
            return buildBaseResponseEntity(200, certificateRequestPayloadAndAccount.getDirectoryData())
                    .header("Location", orderData.buildUrl(Application.baseURL))
                    .body(orderData.getObject());
        } else if (orderData.getObject().getStatus().equals(StatusType.PROCESSING.toString())) {
            return buildBaseResponseEntityWithRetryAfter(200, certificateRequestPayloadAndAccount.getDirectoryData(), 10)
                    .header("Location", orderData.buildUrl(Application.baseURL))
                    .body(orderData.getObject());
        } else {
            log.error("Order not ready to finalize: " + orderData);
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

            Optional<DirectoryData> directoryDataOptional = directoryDataService.getByName(directory);
            DirectoryData directoryData = directoryDataOptional.orElseThrow(() -> new RuntimeException("Could not find DirectoryData"));

            OrderList orderList = orderListData.getObject();
            if (cursor != null) {
                orderList = orderListData.buildPaginatedOrderList(cursor);
                Optional<String> nextPageLink = orderListData.buildPaginatedLink(Application.baseURL, cursor);
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
