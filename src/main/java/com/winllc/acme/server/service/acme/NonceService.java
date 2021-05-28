package com.winllc.acme.server.service.acme;

import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.util.NonceUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class NonceService {

    private final DirectoryDataService directoryDataService;
    private final NonceUtil nonceUtil;

    public NonceService(DirectoryDataService directoryDataService, NonceUtil nonceUtil) {
        this.directoryDataService = directoryDataService;
        this.nonceUtil = nonceUtil;
    }

    @RequestMapping(value = "{directory}/new-nonce", method = RequestMethod.HEAD)
    public ResponseEntity<?> newNonceHead(HttpServletRequest request, @PathVariable String directory)
            throws Exception {
        DirectoryData directoryData = directoryDataService.findByName(directory);
        return ResponseEntity.ok()
                .headers(generateHeaders(directoryData))
                .build();
    }

    @RequestMapping(value = "{directory}/new-nonce", method = RequestMethod.GET)
    public ResponseEntity<?> newNonceGet(HttpServletRequest request, @PathVariable String directory)
            throws Exception {
        DirectoryData directoryData = directoryDataService.findByName(directory);
        return ResponseEntity.noContent()
                .headers(generateHeaders(directoryData))
                .build();
    }

    private HttpHeaders generateHeaders(DirectoryData directoryData){
        String nonce = nonceUtil.generateNonce();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Replay-Nonce", nonce);
        headers.add("Cache-Control", "no-store");
        headers.add("Link", directoryData.buildLinkUrl(Application.baseURL));
        return headers;
    }

}
