package org.neuclear.ledger.hibernate;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;

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

$Id: ThreadLocalSession.java,v 1.2 2004/05/13 23:44:04 pelle Exp $
$Log: ThreadLocalSession.java,v $
Revision 1.2  2004/05/13 23:44:04  pelle
Updated the HibernateLedgerController. Got rid of a part of the getBook query that wasn't compatible with hsqldb
Hopefully improved the ThreadLocalSession. on getSession() it now tests if the connection is still open. If not it creates a new one.

Revision 1.1  2004/04/28 00:23:24  pelle
Fixed the strange verification error
Added bunch of new unit tests to support this.
Updated Signer's dependencies and version number to be a 0.9 release.
Implemented ThreadLocalSession session management for Hibernate ledger.
Various other minor changes.

*/

/**
 * User: pelleb
 * Date: Apr 27, 2004
 * Time: 9:44:54 PM
 */
public class ThreadLocalSession extends ThreadLocal {
    public ThreadLocalSession(SessionFactory factory) {
        this.factory = factory;
    }

    public final Session getSession() throws HibernateException {
        final Session session = (Session) get();
        if (session.isOpen() && session.isConnected())
            return session;
        session.close();
        this.set(initialValue());
        return getSession();
    }

    /**
     * Returns the current thread's initial value for this thread-local
     * variable.  This method will be called once per accessing thread for each
     * thread-local, the first time each thread accesses the variable with the
     * {@link #get()} or {@link #set(Object)} method.  If the programmer
     * desires thread-local variables to be initialized to some value other
     * than <tt>null</tt>, <tt>ThreadLocal</tt> must be subclassed, and this
     * method overridden.  Typically, an anonymous inner class will be used.
     * Typical implementations of <tt>initialValue</tt> will call an
     * appropriate constructor and return the newly constructed object.
     *
     * @return the initial value for this thread-local
     */
    protected Object initialValue() {
        try {
            return factory.openSession();    //To change body of overridden methods use File | Settings | File Templates.
        } catch (HibernateException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws HibernateException {

        factory.close();
    }

    private final SessionFactory factory;
}
