package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.acme.OrderList;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.model.data.OrderListData;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.OrderListPersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.service.acme.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/*
                  valid
                    |
                    |
        +-----------+-----------+
 Client |                Server |
deactiv.|                revoke |
        V                       V
   deactivated               revoked
 */
@Component
public class AccountProcessor implements AcmeDataProcessor<AccountData> {
    private static final Logger log = LogManager.getLogger(AccountProcessor.class);

    @Autowired
    private AccountPersistence accountPersistence;
    @Autowired
    private OrderPersistence orderPersistence;
    @Autowired
    private OrderListPersistence orderListPersistence;

    public AccountData buildNew(DirectoryData directoryData){
        Account account = new Account();
        account.setStatus(StatusType.VALID.toString());

        //Set order list location, Section 7.1.2.1
        OrderList orderList = new OrderList();

        OrderListData orderListData = new OrderListData(orderList, directoryData.getName());
        orderListData = orderListPersistence.save(orderListData);

        account.setOrders(orderListData.buildUrl());

        AccountData accountData = new AccountData(account, directoryData.getName());

        return accountData;
    }

    //Section 7.3.6
    public AccountData deactivateAccount(AccountData accountData) throws InternalServerException {
        //if all goes well
        Account account = accountData.getObject();
        if(account.getStatus().contentEquals(StatusType.VALID.toString())) {
            accountData.getObject().setStatus(StatusType.DEACTIVATED.toString());
            accountData = accountPersistence.save(accountData);

            markInProgressAccountObjectsInvalid(accountData);

            log.info("Account deactivated: "+accountData.getId());

            return accountData;
        }else{
            throw new InternalServerException("Account was not in state to be set deactivated");
        }
    }

    public AccountData accountRevoke(AccountData accountData) throws InternalServerException {
        //if all goes well
        Account account = accountData.getObject();
        if(account.getStatus().contentEquals(StatusType.VALID.toString())) {
            account.setStatus(StatusType.REVOKED.toString());
            accountData = accountPersistence.save(accountData);

            markInProgressAccountObjectsInvalid(accountData);

            log.info("Account revoked: "+accountData.getId());

            return accountData;
        }else{
            throw new InternalServerException("Account was not in state to be set revoked");
        }
    }

    //The server SHOULD cancel any pending operations authorized by the account’s key, such as certificate orders
    private void markInProgressAccountObjectsInvalid(AccountData accountData){
        List<OrderData> orderDataList = orderPersistence.findAllByAccountIdEquals(accountData.getId());
        orderDataList.forEach(o -> {
            if(!o.getObject().getStatus().contentEquals(StatusType.VALID.toString())) {
                o.getObject().setStatus(StatusType.INVALID.toString());
                orderPersistence.save(o);
            }
        });
    }
}
