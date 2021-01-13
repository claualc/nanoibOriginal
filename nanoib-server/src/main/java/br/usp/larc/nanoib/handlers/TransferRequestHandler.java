package br.usp.larc.nanoib.handlers;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import br.usp.larc.nanoib.NanoIBHTTPD.DBConnInfo;
import br.usp.larc.nanoib.beans.TransferRequestBean;

/**
 * Handler para o endpoint privado POST "/transfer". Requer um objeto do tipo TransferRequestBean
 * no corpo da requisição.
 * 
 * @author Oscar
 */
public class TransferRequestHandler extends RequestHandler {

    //Handler's Endpoint
    //----------------------------------------------------------------------------------------------
    public static Endpoint endpoint = new Endpoint("/transfer", Method.POST, false);
    
    //----------------------------------------------------------------------------------------------
    public TransferRequestHandler(DBConnInfo context) {
        super(context);
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public Endpoint getEndpoint() {
        return TransferRequestHandler.endpoint;
    }
    
    //----------------------------------------------------------------------------------------------
    @Override
    public Response handle(
            IHTTPSession input, RemoteTenant tenant, Connection dBConn
    ) throws IOException {
        Response response = null;
        
        try {
            TransferRequestBean trb = TransferRequestBean.fromJsonString(getBodyAsString(input)); 
            
            CallableStatement stmnt = dBConn.prepareCall("call transfer(?,?,?,?,?,?)");

            stmnt.setLong(1, tenant.accBranchId);
            stmnt.setLong(2, tenant.accId);
            stmnt.setLong(3, trb.destAccBranchId);
            stmnt.setLong(4, trb.destAccId);
            stmnt.setBigDecimal(5, trb.value);
            stmnt.setString(6, trb.comment);
            
            if (stmnt.execute()) {
                response = new Response(Status.OK, "application/json", null, 0);
            } else {
                response = new Response(Status.INTERNAL_ERROR, null, null, 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response = new Response(Status.INTERNAL_ERROR, null, null, 0);
        }
        
        return response;        
    }
    
}
