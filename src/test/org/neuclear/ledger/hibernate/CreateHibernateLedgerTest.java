package org.neuclear.ledger.hibernate;

import junit.framework.TestCase;
import org.neuclear.ledger.*;

/**
 * Created by IntelliJ IDEA.
 * User: pelleb
 * Date: Mar 23, 2004
 * Time: 5:16:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class CreateHibernateLedgerTest extends TestCase {
    public CreateHibernateLedgerTest(String name) {
        super(name);
    }

    public void testCreateLedger() throws UnknownLedgerException, LowlevelLedgerException, UnBalancedTransactionException, InvalidTransactionException, UnknownBookException {
        Ledger ledger = new HibernateLedger("test", true);
        assertNotNull(ledger);

        double pre = ledger.getBalance("bob");

        PostedTransaction tran = ledger.transfer("bob", "alice", 85, "hello");
        assertNotNull(tran);

        assertEquals(pre - 85, ledger.getBalance("bob"), 0);
//        ledger.close();
    }
}
