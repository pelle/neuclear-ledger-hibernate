package org.neuclear.ledger.hibernate;

import org.neuclear.ledger.Ledger;
import org.neuclear.ledger.LowlevelLedgerException;
import org.neuclear.ledger.tests.AbstractLedgerBrowserTest;

/*
$Id: HibernateBrowserTest.java,v 1.1 2004/03/26 18:38:28 pelle Exp $
$Log: HibernateBrowserTest.java,v $
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

    public Ledger getLedger() throws LowlevelLedgerException {
        return new HibernateLedger("test");
    }
}
