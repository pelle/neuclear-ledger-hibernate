/*
 * Created on Jul 14, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.neuclear.ledger.hibernate;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.cfg.Configuration;
import org.neuclear.ledger.*;
import org.neuclear.ledger.browser.BookBrowser;
import org.neuclear.ledger.browser.LedgerBrowser;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author pelleb
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public final class HibernateLedger extends Ledger implements LedgerBrowser {

    public HibernateLedger(final String id) throws LowlevelLedgerException, UnknownLedgerException {
        super(id);

        try {
            Configuration cfg = new Configuration()
                    .addClass(HTransaction.class)
                    .addClass(HTransactionItem.class)
                    .addClass(HHeld.class)
                    .addClass(HHeldItem.class);
//            new net.sf.hibernate.tool.hbm2ddl.SchemaExport(cfg).create(true, true);
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
            throw new UnBalancedTransactionException(this, trans);
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HTransaction posted = new HTransaction(trans, new Date());
            ses.saveOrUpdate(posted);
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
            throw new UnBalancedTransactionException(this, trans);
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HTransaction posted = new HTransaction(trans, new Date());
            ses.saveOrUpdate(posted);
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
            throw new UnBalancedTransactionException(this, trans);
        try {
            Session ses = factory.openSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld posted = new HHeld(trans, new Date());
            ses.saveOrUpdate(posted);
            t.commit();
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
    public void performCancelHold(PostedHeldTransaction hold) throws LowlevelLedgerException, UnknownTransactionException {

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
        return null;
    }

    /**
     * Searches for a Transaction based on its Transaction ID
     *
     * @param id A valid ID
     * @return The Transaction object
     */
    public Date getTransactionTime(String id) throws LowlevelLedgerException, UnknownTransactionException, InvalidTransactionException, UnknownBookException {
        return null;
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
        return 0;
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
        return 0;
    }

    /**
     * Searches for a Held Transaction based on its Transaction ID
     *
     * @param idstring A valid ID
     * @return The Transaction object
     */
    public PostedHeldTransaction findHeldTransaction(String idstring) throws LowlevelLedgerException, UnknownTransactionException {
        return null;
    }

    public void close() throws LowlevelLedgerException {
        try {
            factory.close();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public BookBrowser browse(String book) throws LowlevelLedgerException {
        return null;
    }

    public BookBrowser browseFrom(String book, Timestamp from) throws LowlevelLedgerException {
        return null;
    }

    public BookBrowser browseRange(String book, Timestamp from, Timestamp until) throws LowlevelLedgerException {
        return null;
    }

    private final SessionFactory factory;

}
