package org.neuclear.ledger.hibernate;

import org.neuclear.ledger.*;

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
        this.id = unp.getRequestId();
        this.transactionTime = transactionTime;
        this.comment = unp.getComment();
        final List ol = unp.getItemList();
        this.items = new HashSet(ol.size());
        for (int i = 0; i < ol.size(); i++) {
            TransactionItem item = (TransactionItem) ol.get(i);
            final HTransactionItem hitem = new HTransactionItem(this, (HBook) item.getBook(), item.getAmount());
            items.add(hitem);
//            ((HBook)item.getBook()).getItems().add(hitem);
        }

    }

    public HTransaction(final PostedHeldTransaction held, final Date transactionTime, final double amount) throws ExceededHeldAmountException, UnBalancedTransactionException {
        this.id = held.getRequestId();
        this.transactionTime = transactionTime;
        this.comment = held.getComment();
        this.items = new HashSet();
        Iterator iter = held.getAdjustedItems(amount).iterator();
        while (iter.hasNext()) {
            TransactionItem item = (TransactionItem) iter.next();
            final HTransactionItem hitem = new HTransactionItem(this, (HBook) item.getBook(), item.getAmount());
            items.add(hitem);
//            ((HBook)item.getBook()).getItems().add(hitem);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
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
        UnPostedTransaction unp = new UnPostedTransaction(id, comment);
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            HTransactionItem item = (HTransactionItem) iter.next();
            unp.addItem(item.getBook(), item.getAmount());
        }
        PostedTransaction tran = new PostedTransaction(unp, transactionTime);
        tran.setReceiptId(receipt);
        return tran;
    }

    private String id;
    private String receipt;
    private Date transactionTime;
    private String comment;
    private Set items;
}
