package org.neuclear.ledger.hibernate;

import org.neuclear.ledger.InvalidTransactionException;
import org.neuclear.ledger.PostedTransaction;
import org.neuclear.ledger.TransactionItem;
import org.neuclear.ledger.UnPostedTransaction;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: pelleb
 * Date: Mar 23, 2004
 * Time: 10:29:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class HTransaction {

    public HTransaction() {
    }

    public HTransaction(UnPostedTransaction unp, Date transactionTime) {
        this.id = unp.getId();
        this.requestId = unp.getRequestId();
        this.transactionTime = transactionTime;
        this.comment = unp.getComment();
        final List ol = unp.getItemList();
        this.items = new HashSet(ol.size());
        for (int i = 0; i < ol.size(); i++) {
            TransactionItem item = (TransactionItem) ol.get(i);
            items.add(new HTransactionItem(this, item.getBook(), item.getAmount()));
        }
    }

    public HTransaction(HHeld held, Date transactionTime) {
        this.id = held.getId();
        this.requestId = held.getRequestId();
        this.transactionTime = transactionTime;
        this.comment = held.getComment();
        this.items = new HashSet();
        Iterator iter = held.getItems().iterator();
        while (iter.hasNext()) {
            HHeldItem item = (HHeldItem) iter.next();
            items.add(new HTransactionItem(this, item.getBook(), item.getAmount()));
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

    public PostedTransaction createPosted() throws InvalidTransactionException {
        UnPostedTransaction unp = new UnPostedTransaction(requestId, id, comment);
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            HTransactionItem item = (HTransactionItem) iter.next();
            unp.addItem(item.getBook(), item.getAmount());
        }
        return new PostedTransaction(unp, transactionTime);
    }

    private String id;
    private String requestId;
    private Date transactionTime;
    private String comment;
    private Set items;
}
