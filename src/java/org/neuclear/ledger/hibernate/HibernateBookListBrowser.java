package org.neuclear.ledger.hibernate;

/*
 *  The NeuClear Project and it's libraries are
 *  (c) 2002-2004 Antilles Software Ventures SA
 *  For more information see: http://neuclear.org
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import org.neuclear.ledger.LowlevelLedgerException;
import org.neuclear.ledger.browser.BookBrowser;

import java.util.Iterator;

/**
 * User: pelleb
 * Date: Mar 25, 2004
 * Time: 10:00:41 PM
 */
public class HibernateBookListBrowser extends BookBrowser {
    public HibernateBookListBrowser(Iterator iter, String book) {
        super(book);
        this.iter = iter;
    }

    public boolean next() throws LowlevelLedgerException {
        if (!iter.hasNext())
            return false;
        HTransactionItem item = (HTransactionItem) iter.next();
        final HTransaction tran = item.getTransaction();
        HBook counterparty = null;
        Iterator iter = tran.getItems().iterator();
        HBook last = null;

        int count = tran.getItems().size();
        while (iter.hasNext()) {
            count--;
            HTransactionItem party = (HTransactionItem) iter.next();
            if (!party.getBook().equals(item.getBook())) {
                counterparty = party.getBook();
            }
            last = party.getBook();
        }
        if (counterparty == null)//We did a transfer to ourselves
            counterparty = last;
        setRow(tran.getId(), counterparty, tran.getComment(), tran.getTransactionTime(), item.getAmount(), null, null, null);
        return true;
    }

    private final Iterator iter;
}
