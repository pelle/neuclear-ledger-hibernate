package org.neuclear.ledger.hibernate;

/**
 * Created by IntelliJ IDEA.
 * User: pelleb
 * Date: Mar 23, 2004
 * Time: 10:33:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class HHeldItem {
    public HHeldItem(HHeld held, String book, double amount) {
        this.book = book;
        this.amount = amount;
        this.held = held;
//        this.heldId=held.getId();
    }

    public HHeldItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

//    public String getHeldId() {
//        return heldId;
//    }
//
//    public void setHeldId(String heldId) {
//        this.heldId = heldId;
//    }

    public HHeld getHeld() {
        return held;
    }

    public void setHeld(HHeld held) {
        this.held = held;
    }

    private String id;
//    private String heldId;
    private HHeld held;
    private String book;
    private double amount;
}
