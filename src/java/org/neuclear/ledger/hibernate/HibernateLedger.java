/*
 * Created on Jul 14, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.neuclear.ledger.hibernate;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.cfg.Configuration;
import org.neuclear.ledger.*;
import org.neuclear.ledger.browser.BookBrowser;
import org.neuclear.ledger.browser.LedgerBrowser;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;

/**
 * @author pelleb
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public final class HibernateLedger extends Ledger implements LedgerBrowser {

    public HibernateLedger(final String id) throws LowlevelLedgerException {
        this(id, false);
    }

    public HibernateLedger(final String id, final boolean create) throws LowlevelLedgerException {
        super(id);

        try {
            Configuration cfg = new Configuration()
                    .addClass(HTransaction.class)
                    .addClass(HTransactionItem.class)
                    .addClass(HHeld.class)
                    .addClass(HHeldItem.class);
            new net.sf.hibernate.tool.hbm2ddl.SchemaExport(cfg).create(create, create);
            factory = cfg.buildSessionFactory();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    /**
     * The basic interface for creating Transactions in the database.
     * The implementing class takes this transacion information and stores it with an automatically generated uniqueid.
     *
     * @param trans Transaction to perform
     * @return The reference to the transaction
     */
    public PostedTransaction performTransaction(UnPostedTransaction trans) throws UnBalancedTransactionException, LowlevelLedgerException, InvalidTransactionException {
        if (!trans.isBalanced())
            throw new UnBalancedTransactionException(this, trans, trans.getBalance());
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HTransaction posted = new HTransaction(trans, new Date());
            ses.save(posted);
//            ses.flush();
            t.commit();
            ses.close();
            return posted.createPosted();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    /**
     * Similar to a transaction but guarantees that there wont be any negative balances left after the transaction.
     *
     * @param trans Transaction to perform
     * @return The reference to the transaction
     */
    public PostedTransaction performVerifiedTransfer(UnPostedTransaction trans) throws UnBalancedTransactionException, LowlevelLedgerException, InvalidTransactionException {
        if (!trans.isBalanced())
            throw new UnBalancedTransactionException(this, trans, trans.getBalance());
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            Iterator iter = trans.getItems();
            // First lets check the balances
            while (iter.hasNext()) {
                TransactionItem item = (TransactionItem) iter.next();
                if (item.getAmount() < 0 && getAvailableBalance(item.getBook()) + item.getAmount() < 0)
                    throw new InsufficientFundsException(null, item.getBook(), item.getAmount());
            }

            HTransaction posted = new HTransaction(trans, new Date());
            ses.save(posted);
            t.commit();
            ses.close();
            return posted.createPosted();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    /**
     * The basic interface for creating Transactions in the database.
     * The implementing class takes this transacion information and stores it with an automatically generated uniqueid.
     * This transaction guarantees to not leave a negative balance in any account.
     *
     * @param trans Transaction to perform
     */
    public PostedHeldTransaction performHeldTransfer(UnPostedHeldTransaction trans) throws UnBalancedTransactionException, LowlevelLedgerException, InvalidTransactionException {
        if (!trans.isBalanced())
            throw new UnBalancedTransactionException(this, trans, trans.getBalance());
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            Iterator iter = trans.getItems();
            // First lets check the balances
            while (iter.hasNext()) {
                TransactionItem item = (TransactionItem) iter.next();
                if (item.getAmount() < 0 && getAvailableBalance(item.getBook()) + item.getAmount() < 0)
                    throw new InsufficientFundsException(null, item.getBook(), item.getAmount());
            }

            HHeld posted = new HHeld(trans, new Date());
            ses.save(posted);
            t.commit();
//            ses.flush();
            ses.close();
            return posted.createPosted();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    /**
     * Cancels a Held Transaction.
     *
     * @param hold
     * @throws org.neuclear.ledger.LowlevelLedgerException
     *
     * @throws org.neuclear.ledger.UnknownTransactionException
     *
     */
    public Date performCancelHold(PostedHeldTransaction hold) throws LowlevelLedgerException, UnknownTransactionException {
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld posted = (HHeld) ses.get(HHeld.class, hold.getRequestId());
            if (posted != null) {

                posted.setCancelled(true);
                ses.saveOrUpdate(posted);
                t.commit();
                ses.close();
                return new Date();
            } else {
                t.rollback();
                ses.close();
                throw new UnknownTransactionException(this, hold.getRequestId());
            }

        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }

    }

    /**
     * Completes a held transaction. Which means:
     * cancelling the hold and performing the transfer with the given updated amount and comment.
     *
     * @param hold    HeldTransaction to complete
     * @param amount  The updatd amount. It must be <= than the amount of the hold
     * @param comment
     * @return
     * @throws org.neuclear.ledger.InvalidTransactionException
     *
     * @throws org.neuclear.ledger.LowlevelLedgerException
     *
     * @throws org.neuclear.ledger.TransactionExpiredException
     *
     */
    public PostedTransaction performCompleteHold(PostedHeldTransaction hold, double amount, String comment) throws InvalidTransactionException, LowlevelLedgerException, TransactionExpiredException, UnknownTransactionException {
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld posted = (HHeld) ses.get(HHeld.class, hold.getRequestId());
            if (posted == null) {
                t.rollback();
                ses.close();
                throw new UnknownTransactionException(this, hold.getRequestId());
            }
            final Date time = new Date();
            if (posted.getExpiryTime().before(time) || posted.isCancelled() || posted.getCompletedId() != null) {
                ses.close();
                throw new TransactionExpiredException(this, hold);
            }
            HTransaction htran = new HTransaction(hold, time, amount);
            htran.setComment(comment);
            ses.save(htran);
            posted.setCompletedId(htran.getId());
            ses.update(htran);
            t.commit();
            ses.close();
            return htran.createPosted();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(this, e);
        }
    }

    /**
     * Searches for a Transaction based on its Transaction ID
     *
     * @param id A valid ID
     * @return The Transaction object
     */
    public Date getTransactionTime(String id) throws LowlevelLedgerException, UnknownTransactionException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("select transactionTime from HTransaction item where item.id = ?");
            q.setString(0, id);
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    ses.close();
                    return ((Timestamp) o);
                }
            }
            ses.close();
            throw new UnknownTransactionException(this, id);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }

    }

    /**
     * Calculate the true accounting balance at a given time. This does not take into account any held transactions, thus may not necessarily
     * show the Available balance.<p>
     * Example sql for implementors: <pre>
     * select c.credit - d.debit from
     *      (
     *          select sum(amount) as credit
     *          from ledger
     *          where transactiondate <= sysdate and end_date is null and credit= 'neu://BOB'
     *       ) c,
     *      (
     *          select sum(amount) as debit
     *          from ledger
     *          where transactiondate <= sysdate and end_date is null and debit= 'neu://BOB'
     *       ) d
     * <p/>
     * </pre>
     *
     * @return the balance as a double
     */

    public double getBalance(String book) throws LowlevelLedgerException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("select sum(item.amount) from HTransactionItem item where item.book = ? and item.transaction.receipt is not null");
            q.setString(0, book);
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    ses.close();
                    return ((Double) o).doubleValue();
                }
//                throw new LowlevelLedgerException(this,"Query returned more or less than one column");
            }
//            throw new LowlevelLedgerException(this,"Query didnt return a row");
            ses.close();
            return 0;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    private double getHeldBalance(String book) throws LowlevelLedgerException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("select sum(item.amount) from HHeldItem item where item.book = ? and item.amount<0 and item.held.expiryTime > ? and item.held.cancelled=false and item.held.completedId is null and item.held.receipt is not null");
            q.setString(0, book);
            q.setTimestamp(1, new Date());
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    ses.close();
                    return ((Double) o).doubleValue();
                }
            }
            ses.close();
            return 0;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    /**
     * Calculate the available balance at a given time. This DOES take into account any held transactions.
     * Example sql for implementors: <pre>
     * select c.credit - d.debit from
     *      (
     *          select sum(amount) as credit
     *          from ledger
     *          where transactiondate <= sysdate and (end_date is null or end_date>= sysdate) and credit= 'neu://BOB'
     *       ) c,
     *      (
     *          select sum(amount) as debit
     *          from ledger
     *          where transactiondate <= sysdate and end_date is null and debit= 'neu://BOB'
     *       ) d
     * <p/>
     * </pre>
     *
     * @return the balance as a double
     */

    public double getAvailableBalance(String book) throws LowlevelLedgerException {
        return getHeldBalance(book) + getBalance(book);
    }

    public boolean transactionExists(String id) throws LowlevelLedgerException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("select item.id from HTransaction item where item.id = ?");
            q.setString(0, id);
            Iterator iter = q.iterate();
            final boolean answer = (iter.hasNext());
            ses.close();
            return answer;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public boolean heldTransactionExists(String id) throws LowlevelLedgerException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("select item.id from HHeld item where item.id = ?");
            q.setString(0, id);
            Iterator iter = q.iterate();
            final boolean answer = (iter.hasNext());
            ses.close();
            return answer;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    /**
     * Searches for a Held Transaction based on its Transaction ID
     *
     * @param idstring A valid ID
     * @return The Transaction object
     */
    public PostedHeldTransaction findHeldTransaction(String idstring) throws LowlevelLedgerException, UnknownTransactionException {
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld tran = (HHeld) ses.get(HHeld.class, idstring);
            if (tran == null) {
                ses.close();
                throw new UnknownTransactionException(this, idstring);
            }
            PostedHeldTransaction ph = tran.createPosted();
            ses.close();
            return ph;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        } catch (InvalidTransactionException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public void setReceiptId(String id, String receipt) throws LowlevelLedgerException, UnknownTransactionException {
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HTransaction tran = (HTransaction) ses.get(HTransaction.class, id);
            if (tran == null) {
                ses.close();
                throw new UnknownTransactionException(this, id);
            }
            tran.setReceipt(receipt);
            ses.flush();
//            ses.update(tran);
            t.commit();
            ses.close();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }

    }

    public void setHeldReceiptId(String id, String receipt) throws LowlevelLedgerException, UnknownTransactionException {
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld tran = (HHeld) ses.get(HHeld.class, id);
            if (tran == null) {
                ses.close();
                throw new UnknownTransactionException(this, id);
            }
            tran.setReceipt(receipt);
            ses.flush();
            t.commit();
            ses.close();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }


    }

    public double getTestBalance() throws LowlevelLedgerException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("select sum(item.amount) from HTransactionItem item");
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    ses.close();
                    return ((Double) o).doubleValue();
                }
//                throw new LowlevelLedgerException(this,"Query returned more or less than one column");
            }
//            throw new LowlevelLedgerException(this,"Query didnt return a row");
            ses.close();
            return 1; //if we have to return something here there has been an error and we better flag it.
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public void close() throws LowlevelLedgerException {
        try {
            factory.close();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public BookBrowser browse(String book) throws LowlevelLedgerException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("from HTransactionItem item where item.book=?");
            q.setString(0, book);
            Iterator iter = q.iterate();
            return new HibernateBookBrowser(iter, book);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }

    }

    public BookBrowser browseFrom(String book, Date from) throws LowlevelLedgerException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("from HTransactionItem item where item.book=? and item.transaction.transactionTime>=?");
            q.setString(0, book);
            q.setTimestamp(1, from);
            System.out.println("from: " + from);
            Iterator iter = q.iterate();
            return new HibernateBookBrowser(iter, book);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }

    }

    public BookBrowser browseRange(String book, Date from, Date until) throws LowlevelLedgerException {
        try {
            Session ses = factory.openSession();
            Query q = ses.createQuery("from HTransactionItem item where item.book=? and item.transaction.transactionTime>=? and item.transaction.transactionTime<?");
            q.setString(0, book);
            q.setTimestamp(1, from);
            q.setTimestamp(2, until);
            System.out.println("from: " + from);
            System.out.println("until: " + until);
            System.out.println("range of " + (until.getTime() - from.getTime()));
            Iterator iter = q.iterate();
            return new HibernateBookBrowser(iter, book);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }

    }

    private final SessionFactory factory;

}
