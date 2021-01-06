package br.usp.larc.nanoib.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import br.usp.larc.nanoib.NanoIBHTTPD.DBConnInfo;
import br.usp.larc.nanoib.beans.StatementItemBean;

/**
 * Handler para o endpoint privado GET "/statement".
 * 
 * @author Oscar
 */
public class StatementRequestHandler extends RequestHandler {
	
	//Handler's Endpoint
    //----------------------------------------------------------------------------------------------
	public static Endpoint endpoint = new Endpoint("/statement", Method.GET, false);
	
	//----------------------------------------------------------------------------------------------
	public StatementRequestHandler(DBConnInfo context) {
		super(context);
	}

	//----------------------------------------------------------------------------------------------
	@Override
	public Endpoint getEndpoint() {
		return StatementRequestHandler.endpoint;
	}

	//----------------------------------------------------------------------------------------------
	@Override
	public Response handle(
			IHTTPSession input, Tenant tenant, Connection dBConn
	) throws IOException {
		Response response = null;
		
		try {
			CallableStatement stmnt = dBConn.prepareCall("call get_statement_items(?,?,0,9999)");

			stmnt.setLong(1, tenant.accBranchId);
			stmnt.setLong(2, tenant.accId);
			
			if (stmnt.execute()) {
				List<StatementItemBean> result = new ArrayList<StatementItemBean>();
				
				ResultSet rs = stmnt.getResultSet();
				
				while (rs.next())
					result.add(
							new StatementItemBean(
									rs.getLong(1),
									rs.getTimestamp(2).toString(),
									rs.getBoolean(3),
									rs.getBigDecimal(4),
									rs.getString(5),
									rs.getLong(6),
									rs.getLong(7),
									rs.getString(8)
									)
							);
				
				byte[] responseBytes = StatementItemBean.toJsonString(result).getBytes();
				
				response = new Response(
						Status.OK,
						"application/json",
						new ByteArrayInputStream(responseBytes),
						responseBytes.length
				);
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
