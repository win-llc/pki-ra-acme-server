package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.Account;
import com.winllc.acme.server.model.OrderList;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderListData;
import com.winllc.acme.server.model.requestresponse.KeyChangeRequest;
import com.winllc.acme.server.persistence.OrderListPersistence;

public class AccountProcessor implements AcmeDataProcessor<AccountData> {

    public AccountData buildNew(){
        Account account = new Account();
        account.setStatus(StatusType.VALID.toString());

        //Set order list location, Section 7.1.2.1
        OrderList orderList = new OrderList();

        OrderListData orderListData = new OrderListData(orderList);
        new OrderListPersistence().save(orderListData);

        account.setOrders(orderListData.buildUrl());

        AccountData accountData = new AccountData(account);

        return accountData;
    }

    //Section 7.3.6
    public Account deactivateAccount(Account account) throws Exception {
        //TODO
        //if all goes well
        if(account.getStatus().contentEquals("valid")) {
            account.setStatus("deactivated");

            //TODO cancel pending operations

            return account;
        }else{
            throw new Exception();
        }
    }

    public Account serverRevoke(Account account) throws Exception {
        //TODO
        //if all goes well
        if(account.getStatus().contentEquals("valid")) {
            account.setStatus("revoked");

            return account;
        }else{
            throw new Exception();
        }
    }

    public Account keyChange(KeyChangeRequest keyChangeRequest){
        //TODO
        //canChangeAccount

        return null;
    }

    private boolean canChangeAccount(Account account){
        return !account.getStatus().equalsIgnoreCase("deactivated");
    }
}
