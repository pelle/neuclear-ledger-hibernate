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

import org.neuclear.ledger.Book;
import org.neuclear.ledger.LowlevelLedgerException;
import org.neuclear.ledger.browser.PortfolioBrowser;

import java.util.Iterator;

/**
 * User: pelleb
 * Date: Mar 25, 2004
 * Time: 10:00:41 PM
 */
public class HibernatePortfolioBrowser extends PortfolioBrowser {
    public HibernatePortfolioBrowser(Iterator iter, Book book) {
        super(book);
        this.iter = iter;
    }

    public boolean next() throws LowlevelLedgerException {
        if (!iter.hasNext())
            return false;
        Object[] item = (Object[]) iter.next();
        if (item == null || item.length != 3 || item[0] == null || item[1] == null || item[2] == null)
            return false;
        setRow((String) item[0], ((Integer) item[1]).intValue(), ((Double) item[2]).doubleValue());
        return true;
    }

    private final Iterator iter;
}
