package org.neuclear.ledger.hibernate;

import org.neuclear.ledger.InvalidTransactionException;
import org.neuclear.ledger.PostedHeldTransaction;
import org.neuclear.ledger.TransactionItem;
import org.neuclear.ledger.UnPostedHeldTransaction;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: pelleb
 * Date: Mar 23, 2004
 * Time: 10:29:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class HHeld {

    public HHeld() {
    }

    public HHeld(UnPostedHeldTransaction tran, Date transactionTime) {
        this.id = tran.getId();
        this.requestId = tran.getRequestId();
        this.transactionTime = transactionTime;
        this.expiryTime = tran.getExpiryTime();
        this.comment = tran.getComment();
        final List ol = tran.getItemList();
        this.items = new HashSet(ol.size());
        for (int i = 0; i < ol.size(); i++) {
            TransactionItem item = (TransactionItem) ol.get(i);
            items.add(new HHeldItem(this, item.getBook(), item.getAmount()));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Date getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Date transactionTime) {
        this.transactionTime = transactionTime;
    }

    public Date getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Date expiryTime) {
        this.expiryTime = expiryTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Set getItems() {
        return items;
    }

    public void setItems(Set items) {
        this.items = items;
    }

    public PostedHeldTransaction createPosted() throws InvalidTransactionException {
        UnPostedHeldTransaction unp = new UnPostedHeldTransaction(id, requestId, comment, expiryTime);
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            HHeldItem item = (HHeldItem) iter.next();
            unp.addItem(item.getBook(), item.getAmount());
        }
        return new PostedHeldTransaction(unp, transactionTime);
    }

    private String id;
    private String requestId;
    private Date transactionTime;
    private Date expiryTime;
    private String comment;
    private Set items;
}
