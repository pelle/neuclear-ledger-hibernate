package org.neuclear.ledger.hibernate;

/**
 * Created by IntelliJ IDEA.
 * User: pelleb
 * Date: Mar 23, 2004
 * Time: 10:33:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class HTransactionItem {
    public HTransactionItem(HTransaction tran, HBook book, double amount) {
        this.book = book;
        this.amount = amount;
        this.transaction = tran;
//        this.transactionId=tran.getRequestId();
    }

    public HTransactionItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public HBook getBook() {
        return book;
    }

    public void setBook(HBook book) {
        this.book = book;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

/*
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
*/

    public HTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(HTransaction transaction) {
        this.transaction = transaction;
    }

    private String id;
//    private String transactionId;
    private HTransaction transaction;
    private HBook book;
    private double amount;
}
