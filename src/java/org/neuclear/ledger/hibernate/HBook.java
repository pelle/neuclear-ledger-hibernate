package org.neuclear.ledger.hibernate;

import org.neuclear.ledger.Book;

import java.util.Date;
import java.util.Set;

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

/**
 * User: pelleb
 * Date: Apr 19, 2004
 * Time: 11:49:47 AM
 */
public class HBook extends Book {
    public HBook() {

    }

    public HBook(String id, String nickname, String type, String source, Date registered, Date updated, String registrationid) {
        super(id, nickname, type, source, registered, updated, registrationid);
    }

    public HBook(String id, Date registered) {
        super(id, registered);
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public void setRegistrationId(String registrationid) {
        this.registrationid = registrationid;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRegistered(Date registered) {
        this.registered = registered;
    }

    public Set getItems() {
        return items;
    }

    public void setItems(Set items) {
        this.items = items;
    }

    private Set items;


}
