package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.Application;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

//Section 7.1.1
@RestController
public class DirectoryService {

    @RequestMapping(method = RequestMethod.GET, value = "acme")
    public ResponseEntity<?> directory(){

        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(Application.directory);
    }
}
