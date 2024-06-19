/*
 * Automatically generated by jrpcgen 1.0.7+ on 6/19/24, 7:09 PM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 *
 * This version of jrpcgen adopted by dCache project
 * See http://www.dCache.ORG for details
 */
package org.dcache.rquota.xdr;
import org.dcache.oncrpc4j.rpc.*;
import org.dcache.oncrpc4j.rpc.net.*;
import org.dcache.oncrpc4j.xdr.*;
import java.io.IOException;

public class setquota_args implements XdrAble {
    public int sqa_qcmd;
    public String sqa_pathp;
    public int sqa_id;
    public sq_dqblk sqa_dqblk;

    public setquota_args() {
    }

    public setquota_args(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        xdr.xdrEncodeInt(sqa_qcmd);
        xdr.xdrEncodeString(sqa_pathp);
        xdr.xdrEncodeInt(sqa_id);
        sqa_dqblk.xdrEncode(xdr);
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        sqa_qcmd = xdr.xdrDecodeInt();
        sqa_pathp = xdr.xdrDecodeString();
        sqa_id = xdr.xdrDecodeInt();
        sqa_dqblk = new sq_dqblk(xdr);
    }

}
// End of setquota_args.java
