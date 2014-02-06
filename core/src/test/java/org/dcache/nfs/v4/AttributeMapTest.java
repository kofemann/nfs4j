/*
 * Copyright (c) 2009 - 2014 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.nfs.v4;

import com.google.common.base.Optional;
import java.io.IOException;
import org.dcache.nfs.v4.xdr.attrlist4;
import org.dcache.nfs.v4.xdr.bitmap4;
import org.dcache.nfs.v4.xdr.fattr4;
import org.dcache.nfs.v4.xdr.nfs4_prot;
import org.dcache.nfs.v4.xdr.uint32_t;
import org.dcache.nfs.v4.xdr.uint64_t;
import org.dcache.xdr.OncRpcException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 */
public class AttributeMapTest {


    @Test
    public void testEmptyBitmap() throws OncRpcException, IOException {
	fattr4 attributes = new fattr4();
	attributes.attrmask = emptyBitmap();
	attributes.attr_vals =  new attrlist4( new byte[0]);

	AttributeMap attributeMap = new AttributeMap(attributes);
	Optional<uint64_t> size =  attributeMap.get(nfs4_prot.FATTR4_SIZE);
	assertNotNull("null value returnd", size);
	assertFalse("ghost value retirned", size.isPresent());
    }

    private static bitmap4 emptyBitmap() {
	return new bitmap4(new uint32_t[0]);
    }
}
