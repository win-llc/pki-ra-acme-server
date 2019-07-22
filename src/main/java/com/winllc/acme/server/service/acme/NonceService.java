package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.util.AppUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class NonceService {

    @RequestMapping(value = "{directory}/new-nonce", method = RequestMethod.HEAD)
    public ResponseEntity<?> newNonceHead(HttpServletRequest request, @PathVariable String directory){
        AcmeURL acmeURL = new AcmeURL(request);
        DirectoryData directoryData = Application.directoryDataMap.get(acmeURL.getDirectoryIdentifier());
        return ResponseEntity.ok()
                .headers(generateHeaders(directoryData))
                .build();
    }

    @RequestMapping(value = "{directory}/new-nonce", method = RequestMethod.GET)
    public ResponseEntity<?> newNonceGet(HttpServletRequest request, @PathVariable String directory){
        AcmeURL acmeURL = new AcmeURL(request);
        DirectoryData directoryData = Application.directoryDataMap.get(acmeURL.getDirectoryIdentifier());
        return ResponseEntity.noContent()
                .headers(generateHeaders(directoryData))
                .build();
    }

    private HttpHeaders generateHeaders(DirectoryData directoryData){
        String nonce = AppUtil.generateNonce();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Replay-Nonce", nonce);
        headers.add("Cache-Control", "no-store");
        headers.add("Link", directoryData.buildLinkUrl());
        return headers;
    }

}
