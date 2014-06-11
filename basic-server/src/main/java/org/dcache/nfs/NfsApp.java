/*
 * Copyright (c) 2009 - 2013 Deutsches Elektronen-Synchroton,
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
package org.dcache.nfs;

import java.io.IOException;
import org.dcache.xdr.OncRpcSvc;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class NfsApp {

    private NfsApp() {
        // this class it used only to bootstrap the Spring IoC
    }

    public static void main(String[] args) throws IOException {

        try {
            ApplicationContext context = new ClassPathXmlApplicationContext("oncrpcsvc.xml");
            OncRpcSvc service = (OncRpcSvc) context.getBean("oncrpcsvc");
            service.start();

            System.in.read();
        } catch (BeansException e) {
            System.err.println("NfsApp: " + e.getMessage());
            System.exit(1);
        }
    }
}
