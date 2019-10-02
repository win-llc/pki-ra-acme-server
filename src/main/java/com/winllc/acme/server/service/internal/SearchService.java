package com.winllc.acme.server.service.internal;

import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.CertData;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.CertificatePersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/search")
public class SearchService {

    @Autowired
    private AccountPersistence accountPersistence;
    @Autowired
    private CertificatePersistence certificatePersistence;

    @GetMapping("/findCertsAssociatedWithExternalAccount/{eabKeyIdentifier}")
    public List<String> findCertsAssociatedWithExternalAccount(@PathVariable String eabKeyIdentifier){
        //todo

        List<AccountData> accountDataList = accountPersistence.findAllByEabKeyIdentifierEquals(eabKeyIdentifier);

        List<String> allCerts = new ArrayList<>();

        if(!CollectionUtils.isEmpty(accountDataList)){
            for(AccountData accountData : accountDataList){
                List<CertData> certs = certificatePersistence.findAllByAccountIdEquals(accountData.getId());
                List<String> temp = certs.stream()
                        .filter(c -> c.getObject() != null)
                        .filter(c -> c.getObject().length > 0)
                        .map(c -> c.getObject()[0])
                        .collect(Collectors.toList());
                allCerts.addAll(temp);
            }
        }

        return allCerts;
    }
}
