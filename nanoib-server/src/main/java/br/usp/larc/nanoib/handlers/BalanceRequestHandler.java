package br.usp.larc.nanoib.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import br.usp.larc.nanoib.NanoIBHTTPD.DBConnInfo;
import br.usp.larc.nanoib.beans.BalanceBean;

/**
 * Handler para o endpoint privado GET "/balance".
 * 
 * @author Oscar
 */
public class BalanceRequestHandler extends RequestHandler {
	
	//Handler's Endpoint
    //----------------------------------------------------------------------------------------------
	public static Endpoint endpoint = new Endpoint("/balance", Method.GET, false);
	
	//----------------------------------------------------------------------------------------------
	public BalanceRequestHandler(DBConnInfo context) {
		super(context);
	}

	//----------------------------------------------------------------------------------------------	
	@Override
	public Endpoint getEndpoint() {
		return BalanceRequestHandler.endpoint;
	}

	//----------------------------------------------------------------------------------------------
	@Override
	public Response handle(
			IHTTPSession input, Tenant tenant, Connection dBConn
	) throws IOException {
		Response response = null;
		
		try {
			CallableStatement stmnt = dBConn.prepareCall("call get_balance(?,?)");

			stmnt.setLong(1, tenant.accBranchId);
			stmnt.setLong(2, tenant.accId);
			
			if (stmnt.execute()) {
				ResultSet rs = stmnt.getResultSet();
				rs.first();
				
				byte[] responseBytes = new BalanceBean(
						rs.getBigDecimal(1),
						rs.getLong(2)
				).toJsonString().getBytes();
				
				ByteArrayInputStream bbis = new ByteArrayInputStream(responseBytes);
				
				response = new Response(Status.OK, "application/json", bbis, responseBytes.length);
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
