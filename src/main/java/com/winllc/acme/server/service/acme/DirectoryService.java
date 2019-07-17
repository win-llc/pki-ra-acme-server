package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.DirectoryData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

//Section 7.1.1
@RestController
public class DirectoryService {

    @RequestMapping(method = RequestMethod.GET, value = "acme")
    public ResponseEntity<?> directory(HttpServletRequest request){
        AcmeURL acmeURL = new AcmeURL(request);
        DirectoryData directoryData = Application.directoryDataMap.get(acmeURL.getDirectoryIdentifier());
        Directory directory = directoryData.getObject();

        //Section 7.4.1 paragraph 4
        if(!directoryData.isAllowPreAuthorization()){
            directory.setNewAuthz(null);
        }

        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(directory);
    }
}
