package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.util.NonceUtil;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class NonceService {

    @Autowired
    private DirectoryDataService directoryDataService;

    @RequestMapping(value = "{directory}/new-nonce", method = RequestMethod.HEAD)
    public ResponseEntity<?> newNonceHead(HttpServletRequest request, @PathVariable String directory){
        DirectoryData directoryData = directoryDataService.getByName(directory);
        return ResponseEntity.ok()
                .headers(generateHeaders(directoryData))
                .build();
    }

    @RequestMapping(value = "{directory}/new-nonce", method = RequestMethod.GET)
    public ResponseEntity<?> newNonceGet(HttpServletRequest request, @PathVariable String directory){
        DirectoryData directoryData = directoryDataService.getByName(directory);
        return ResponseEntity.noContent()
                .headers(generateHeaders(directoryData))
                .build();
    }

    private HttpHeaders generateHeaders(DirectoryData directoryData){
        String nonce = NonceUtil.generateNonce();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Replay-Nonce", nonce);
        headers.add("Cache-Control", "no-store");
        headers.add("Link", directoryData.buildLinkUrl());
        return headers;
    }

}
