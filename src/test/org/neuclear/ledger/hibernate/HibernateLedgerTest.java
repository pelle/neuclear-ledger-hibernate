package org.neuclear.ledger.hibernate;

import org.neuclear.ledger.LedgerController;
import org.neuclear.ledger.LowlevelLedgerException;
import org.neuclear.ledger.UnknownLedgerException;
import org.neuclear.ledger.tests.AbstractLedgerTest;

/**
 * Created by IntelliJ IDEA.
 * User: pelleb
 * Date: Jul 16, 2003
 * Time: 12:58:30 PM
 * To change this template use Options | File Templates.
 */
public final class HibernateLedgerTest extends AbstractLedgerTest {
    public HibernateLedgerTest(final String s) {
        super(s);
    }

    public final LedgerController createLedger() throws UnknownLedgerException, LowlevelLedgerException {
        return new HibernateLedgerController("test", true);
    }


}
