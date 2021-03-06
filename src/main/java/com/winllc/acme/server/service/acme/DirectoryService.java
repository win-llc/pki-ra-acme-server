package com.winllc.acme.server.service.acme;

import com.winllc.acme.common.model.acme.Directory;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

//Section 7.1.1
@RestController
public class DirectoryService {

    private static final Logger log = LogManager.getLogger(DirectoryService.class);

    @Autowired
    private DirectoryDataService directoryDataService;

    @RequestMapping(method = RequestMethod.GET, value = "{directoryName}/directory")
    public ResponseEntity<?> directory(HttpServletRequest request, @PathVariable String directoryName)
            throws Exception {
        DirectoryData directoryData = directoryDataService.findByName(directoryName);
        Directory directory = directoryData.getObject();

        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(directory);
    }
}
