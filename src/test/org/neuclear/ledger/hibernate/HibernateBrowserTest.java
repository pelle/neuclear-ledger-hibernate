package org.neuclear.ledger.hibernate;

import org.neuclear.ledger.LedgerController;
import org.neuclear.ledger.LowlevelLedgerException;
import org.neuclear.ledger.tests.AbstractLedgerBrowserTest;

/*
$Id: HibernateBrowserTest.java,v 1.3 2004/04/27 15:25:16 pelle Exp $
$Log: HibernateBrowserTest.java,v $
Revision 1.3  2004/04/27 15:25:16  pelle
Due to a new API change in 0.5 I have changed the name of Ledger and it's implementers to LedgerController.

Revision 1.2  2004/04/20 00:16:19  pelle
Hibernate now works with the book tables

Revision 1.1  2004/03/26 18:38:28  pelle
More work on browsers. Added an AbstractLedgerBrowserTest for unit testing LedgerBrowsers.

*/

/**
 * User: pelleb
 * Date: Mar 26, 2004
 * Time: 1:00:05 PM
 */
public class HibernateBrowserTest extends AbstractLedgerBrowserTest {
    public HibernateBrowserTest(String name) {
        super(name);
    }

    public LedgerController getLedger() throws LowlevelLedgerException {
        return new HibernateLedgerController("test", true);
    }
}
