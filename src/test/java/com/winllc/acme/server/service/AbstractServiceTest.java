package com.winllc.acme.server.service;

import com.winllc.acme.server.MockCertificateAuthority;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.common.model.acme.Account;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public abstract class AbstractServiceTest {

    protected String testAccountJwk = "{\"kty\":\"RSA\",\"e\":\"AQAB\",\"n\":\"w_RXzWeUTNCCqRFR_km9LHpxmYMgGLCj78G3PpH-1GGAKRPUihULrGQv5ti74AfOofSldGN9ALX-SKrrQXMCh227eIxF8FKRQGdEUjj8uiuAVI6wvrWMhLqKs_xuHx8qsyI893juC8LSdeuo_oFnxqLGB2YfJ6h7Ivb6XAll-7OXF7HWD9x6otAh9K4Pt1VZAxDnBxVOaa6sedAxFmP0a9cGD0QJbx-93xZBRi093m7VslIPZKbmI2x-kXISNxetGKWeR1XkZLIcz0thdkSkO40Pb5IS5A7xS8e14JoCmI6Mu3FnxokNnyAwCdqV8y7b-hV6EQ29P7VByTDj9o60fw\"}";

    @MockBean
    private DirectoryDataService directoryDataService;
    @MockBean
    private AccountPersistence accountPersistence;
    @MockBean
    private CertificateAuthorityService certificateAuthorityService;


    @Before
    public void setUp() throws Exception {
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);

        Account account = new Account();
        account.setStatus(StatusType.VALID.toString());
        AccountData accountData = MockUtils.buildMockAccountData();

        CertificateAuthority certificateAuthority = new MockCertificateAuthority();

        when(directoryDataService.getByName(any())).thenReturn(Optional.of(directoryData));
        when(directoryDataService.findByName(any())).thenReturn(directoryData);

        //todo move to local tests
        when(accountPersistence.save(any())).thenReturn(accountData);
        when(accountPersistence.findFirstByJwkEquals(MockUtils.rsaJWK.toPublicJWK().toString())).thenReturn(Optional.of(accountData));
        when(accountPersistence.findFirstByJwkEquals(MockUtils.alternateRsaJwk.toPublicJWK().toString())).thenReturn(Optional.empty());
        when(accountPersistence.findById(accountData.getId())).thenReturn(Optional.of(accountData));
        when(accountPersistence.findAllByEabKeyIdentifierEquals(any())).thenReturn(Collections.singletonList(accountData));

        when(certificateAuthorityService.findByName(any())).thenReturn(certificateAuthority);
        when(certificateAuthorityService.getByName(any())).thenReturn(certificateAuthority);
    }

    @After
    public void tearDown() throws Exception {
    }

}
