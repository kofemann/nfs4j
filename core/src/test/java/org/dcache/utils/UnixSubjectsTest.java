package org.dcache.utils;

import javax.security.auth.Subject;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.dcache.utils.UnixSubjects.*;

/**
 *
 */
public class UnixSubjectsTest {

    @Test
    public void testNobodyIsNobody() {
        assertTrue(isNobody(NOBODY));
    }

    @Test
    public void testRootIsRoot() {
        assertTrue(isRoot(ROOT));
    }

    @Test
    public void testManualyCreatedRoot() {
        assertTrue(isRoot(of(0, 0)));
    }

    @Test
    public void testEmptySubjectIsNobody() {
        assertTrue(isNobody(new Subject()));
    }

    @Test
    public void testRandomUserNotRoot() {
        assertFalse(isRoot(of(1, 1)));
    }

    @Test
    public void testRandomUserNotNobody() {
        assertFalse(isNobody(of(1, 1)));
    }

}
