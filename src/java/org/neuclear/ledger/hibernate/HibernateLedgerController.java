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
import net.sf.hibernate.cfg.Configuration;
import org.neuclear.ledger.*;
import org.neuclear.ledger.browser.BookBrowser;
import org.neuclear.ledger.browser.BookListBrowser;
import org.neuclear.ledger.browser.LedgerBrowser;
import org.neuclear.ledger.browser.PortfolioBrowser;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;

/**
 * @author pelleb
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public final class HibernateLedgerController extends LedgerController implements LedgerBrowser {

    public HibernateLedgerController(final String id) throws LowlevelLedgerException {
        this(id, false);
    }

    public boolean existsLedger(String id) {
        return false;
    }

    public HibernateLedgerController(final String id, final boolean create) throws LowlevelLedgerException {
        super(id);

        try {
            Configuration cfg = new Configuration()
                    .addClass(HBook.class)
                    .addClass(HTransaction.class)
                    .addClass(HTransactionItem.class)
                    .addClass(HHeld.class)
                    .addClass(HHeldItem.class);
            new net.sf.hibernate.tool.hbm2ddl.SchemaExport(cfg).create(create, create);
            locSes = new ThreadLocalSession(cfg.buildSessionFactory());
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
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HTransaction posted = new HTransaction(trans, new Date());
            ses.save(posted);
            ses.flush();
            t.commit();
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
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            Iterator iter = trans.getItems();
            // First lets check the balances
            while (iter.hasNext()) {
                TransactionItem item = (TransactionItem) iter.next();
                if (item.getAmount() < 0 && getAvailableBalance(trans.getLedger(), item.getBook().getId()) + item.getAmount() < 0)
                    throw new InsufficientFundsException(null, item.getBook().getId(), item.getAmount());
            }

            HTransaction posted = new HTransaction(trans, new Date());
            ses.save(posted);
            t.commit();
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
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            Iterator iter = trans.getItems();
            // First lets check the balances
            while (iter.hasNext()) {
                TransactionItem item = (TransactionItem) iter.next();
                if (item.getAmount() < 0 && getAvailableBalance(trans.getLedger(), item.getBook().getId()) + item.getAmount() < 0)
                    throw new InsufficientFundsException(null, item.getBook().getId(), item.getAmount());
            }

            HHeld posted = new HHeld(trans, new Date());
            ses.save(posted);
            t.commit();
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
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld posted = (HHeld) ses.get(HHeld.class, hold.getRequestId());
            if (posted != null) {

                posted.setCancelled(true);
                ses.saveOrUpdate(posted);
                t.commit();
                return new Date();
            } else {
                t.rollback();
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
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld posted = (HHeld) ses.get(HHeld.class, hold.getRequestId());
            if (posted == null) {
                t.rollback();
                throw new UnknownTransactionException(this, hold.getRequestId());
            }
            final Date time = new Date();
            if (posted.getExpiryTime().before(time) || posted.isCancelled() || posted.getCompletedId() != null) {
                throw new TransactionExpiredException(this, hold);
            }
            HTransaction htran = new HTransaction(hold, time, amount);
            htran.setComment(comment);
            ses.save(htran);
            posted.setCompletedId(htran.getId());
            ses.update(htran);
            t.commit();
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
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select transactionTime from HTransaction item where item.id = ?");
            q.setString(0, id);
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    return ((Timestamp) o);
                }
            }
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

    public double getBalance(String ledger, String book) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select sum(item.amount) from HTransactionItem item where item.book.id = ? and item.transaction.ledger=? and item.transaction.receipt is not null");
            q.setString(0, book);
            q.setString(1, ledger);
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    return ((Double) o).doubleValue();
                }
            }
            return 0;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    private double getHeldBalance(String ledger, String book) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select sum(item.amount) from HHeldItem item where item.book.id = ? and item.held.ledger=?  and item.amount<0 and item.held.expiryTime > ? and item.held.cancelled=false and item.held.completedId is null and item.held.receipt is not null");
            q.setString(0, book);
            q.setString(1, ledger);
            q.setTimestamp(2, new Date());
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    return ((Double) o).doubleValue();
                }
            }
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

    public double getAvailableBalance(String ledger, String book) throws LowlevelLedgerException {
        return getHeldBalance(ledger, book) + getBalance(ledger, book);
    }

    public long getBookCount(String ledger) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select count(books) from HBook books");
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    return ((Integer) o).longValue();
                }
            }
            return 0;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public long getTransactionCount(String ledger) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select count(transactions) from HTransaction transactions where transactions.ledger=?");
            q.setString(0, ledger);
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    return ((Integer) o).longValue();
                }
            }
            return 0;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }


    public boolean transactionExists(String id) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select item.id from HTransaction item where item.id = ?");
            q.setString(0, id);
            Iterator iter = q.iterate();
            final boolean answer = (iter.hasNext());
            return answer;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public boolean heldTransactionExists(String id) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select item.id from HHeld item where item.id = ?");
            q.setString(0, id);
            Iterator iter = q.iterate();
            final boolean answer = (iter.hasNext());
            return answer;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    /**
     * Register a Book in the system
     *
     * @param id
     * @param nickname
     * @param type
     * @param source
     * @param registrationid
     * @return
     * @throws org.neuclear.ledger.LowlevelLedgerException
     *
     */
    public Book registerBook(String id, String nickname, String type, String source, String registrationid) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HBook book = (HBook) ses.get(HBook.class, id);
            final Date time = new Date();
            if (book == null) {
                book = new HBook(id, nickname, type, source, time, time, registrationid);
                ses.save(book);
            } else {
                book.setNickname(nickname);
                book.setRegistrationId(registrationid);
                book.setUpdated(time);
                book.setType(type);
                book.setSource(source);
            }
            t.commit();
            return book;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public Book getBook(String id) throws LowlevelLedgerException, UnknownBookException {
        try {
            id = id.toLowerCase();
            Session ses = locSes.getSession();
            Query q = ses.createQuery("from HBook book " +
                    "where(length(?)=32 and book.id=?) or " +
                    "(length(?)<>32 and book.nickname=?)");
//                   + " or (substring(book.id,0,length(?))=?)");
            q.setString(0, id);
            q.setString(1, id);
            q.setString(2, id);
            q.setString(3, id);
//            q.setString(4, id);
//            q.setString(5, id);
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                HBook book = (HBook) iter.next();
                if (iter.hasNext()) //oops we've got more than one
                    throw new UnknownBookException(this, id);
                return book;
            } else/* if (id.length() == 32)*/ {
                net.sf.hibernate.Transaction t = ses.beginTransaction();
                HBook book = new HBook(id, new Date());
                ses.save(book);
                t.commit();
                return book;
            }
//            throw new UnknownBookException(this, id);
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
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld tran = (HHeld) ses.get(HHeld.class, idstring);
            if (tran == null) {
                throw new UnknownTransactionException(this, idstring);
            }
            PostedHeldTransaction ph = tran.createPosted();
            return ph;
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        } catch (InvalidTransactionException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public void setReceiptId(String id, String receipt) throws LowlevelLedgerException, UnknownTransactionException {
        try {
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HTransaction tran = (HTransaction) ses.load(HTransaction.class, id);
            if (tran == null) {
                throw new UnknownTransactionException(this, id);
            }
            tran.setReceipt(receipt);
            ses.flush();
            t.commit();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }

    }

    public void setHeldReceiptId(String id, String receipt) throws LowlevelLedgerException, UnknownTransactionException {
        try {
            Session ses = locSes.getSession();
            net.sf.hibernate.Transaction t = ses.beginTransaction();
            HHeld tran = (HHeld) ses.get(HHeld.class, id);
            if (tran == null) {
                throw new UnknownTransactionException(this, id);
            }
            tran.setReceipt(receipt);
            ses.flush();
            t.commit();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }


    }

    public double getTestBalance(String ledger) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select sum(item.amount) from HTransactionItem item where item.transaction.ledger=?");
            q.setString(0, ledger);
            Iterator iter = q.iterate();
            if (iter.hasNext()) {
                final Object o = iter.next();
                if (o != null) {
                    return ((Double) o).doubleValue();
                }
                return 0; // if o is NULL there are no transactions and it balances
            }
            return 1; //if we have to return something here there has been an error and we better flag it.
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public void close() throws LowlevelLedgerException {
        try {
            locSes.close();
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public BookBrowser browse(String book) throws LowlevelLedgerException {
        return browse(getId(), book);
    }

    public BookBrowser browse(String ledger, String book) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("from HTransactionItem item where item.book.id=? and item.transaction.ledger=?");
            q.setString(0, book);
            q.setString(1, ledger);
            Iterator iter = q.iterate();
            return new HibernateBookBrowser(iter, book);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public BookBrowser browseFrom(String ledger, String book, Date from) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("from HTransactionItem item where item.book.id=? and item.transaction.transactionTime>=? and item.transaction.ledger=?");
            q.setString(0, book);
            q.setTimestamp(1, from);
            q.setString(2, ledger);
            System.out.println("from: " + from);
            Iterator iter = q.iterate();
            return new HibernateBookBrowser(iter, book);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public BookBrowser browseRange(String ledger, String book, Date from, Date until) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("from HTransactionItem item where item.book.id=? and " +
                    "item.transaction.transactionTime>=? and item.transaction.transactionTime<? and " +
                    "item.transaction.ledger=?");
            q.setString(0, book);
            q.setTimestamp(1, from);
            q.setTimestamp(2, until);
            q.setString(3, ledger);
            System.out.println("from: " + from);
            System.out.println("until: " + until);
            System.out.println("range of " + (until.getTime() - from.getTime()));
            Iterator iter = q.iterate();
            return new HibernateBookBrowser(iter, book);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }

    }

    public BookBrowser browseFrom(String book, Date from) throws LowlevelLedgerException {
        return browseFrom(getId(), book, from);
    }

    public BookBrowser browseRange(String book, Date from, Date until) throws LowlevelLedgerException {
        return browseRange(getId(), book, from, until);
    }

    public BookListBrowser browseBooks(String ledger) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select item.book,count(item.id),sum(item.amount) from HTransactionItem item where item.transaction.ledger=? group by item.book");
            q.setString(0, ledger);
            Iterator iter = q.iterate();
            return new HibernateBookListBrowser(iter, ledger);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    public PortfolioBrowser browsePortfolio(Book book) throws LowlevelLedgerException {
        try {
            Session ses = locSes.getSession();
            Query q = ses.createQuery("select item.transaction.ledger,count(item.id),sum(item.amount) from HTransactionItem item where item.book.id=? group by item.transaction.ledger");
            q.setString(0, book.getId());
            Iterator iter = q.iterate();
            return new HibernatePortfolioBrowser(iter, book);
        } catch (HibernateException e) {
            throw new LowlevelLedgerException(e);
        }
    }

    private final ThreadLocalSession locSes;

}
