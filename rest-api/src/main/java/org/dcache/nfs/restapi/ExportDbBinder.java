package org.dcache.nfs.restapi;

import org.dcache.nfs.ExportDB;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 *
 */
public class ExportDbBinder extends AbstractBinder {

    private final ExportDB exportDB;
    public ExportDbBinder(ExportDB exportDB) {
        this.exportDB = exportDB;
    }

    @Override
    protected void configure() {
        bind(exportDB).to(ExportDB.class);
    }

}
