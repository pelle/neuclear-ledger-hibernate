package org.neuclear.ledger.hibernate;

import org.neuclear.ledger.LowlevelLedgerException;
import org.neuclear.ledger.browser.BookBrowser;

import java.util.Iterator;

/*
NeuClear Distributed Transaction Clearing Platform
(C) 2003 Pelle Braendgaard

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

$Id: HibernateBookBrowser.java,v 1.5 2004/04/21 23:25:57 pelle Exp $
$Log: HibernateBookBrowser.java,v $
Revision 1.5  2004/04/21 23:25:57  pelle
Integrated Browser with the asset controller
Updated look and feel
Added ServletLedgerFactory
Added ServletAssetControllerFactory
Created issue.jsp file
Fixed many smaller issues

Revision 1.4  2004/04/19 18:57:38  pelle
Updated Ledger to support more advanced book information.
You can now create a book or fetch a book by doing getBook(String id) on the ledger.
You can register a book or upddate an existing one using registerBook()
SimpleLedger now works and passes all tests.
HibernateLedger has been implemented, but there are a few things that dont work yet.

Revision 1.3  2004/03/31 23:11:30  pelle
Reworked the ID's of the transactions. The primary ID is now the request ID.
Receipt ID's are optional and added using a separate set method.
The various interactive passphrase agents now have shell methods for the new interactive approach.

Revision 1.2  2004/03/26 23:36:50  pelle
The simple browse(book) now works on hibernate, I have implemented the other two, which currently don not constrain the query correctly.

Revision 1.1  2004/03/25 22:05:20  pelle
First shell of the HibernateBookBrowser

*/

/**
 * User: pelleb
 * Date: Mar 25, 2004
 * Time: 10:00:41 PM
 */
public class HibernateBookBrowser extends BookBrowser {
    public HibernateBookBrowser(Iterator iter, String book) {
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

        int count = tran.getItems().size();
        while (iter.hasNext()) {
            count--;
            HTransactionItem party = (HTransactionItem) iter.next();
            if (!party.getBook().equals(item.getBook()) || count == 0) {
                counterparty = party.getBook();
            }
        }
        setRow(tran.getId(), counterparty, tran.getComment(), tran.getTransactionTime(), item.getAmount(), null, null, null);
        return true;
    }

    private final Iterator iter;
}
