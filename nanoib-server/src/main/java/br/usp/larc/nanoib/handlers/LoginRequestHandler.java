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

import br.usp.larc.nanoib.NanoIBHTTPD;
import br.usp.larc.nanoib.beans.LoginRequestBean;
import br.usp.larc.nanoib.beans.SessionCookieBean;

/**
 * Handler para o endpoint público POST "/login". Requer um objeto LoginRequestBean no corpo das
 * requisições.
 * 
 * @author Oscar
 */
public class LoginRequestHandler extends RequestHandler {
	
	//Handler's Endpoint
    //----------------------------------------------------------------------------------------------
	public static Endpoint endpoint = new Endpoint("/login", Method.POST, true);
	
	//----------------------------------------------------------------------------------------------
	public LoginRequestHandler(NanoIBHTTPD.DBConnInfo context) {
		super(context);
	}
	
	//----------------------------------------------------------------------------------------------
	@Override
	public Endpoint getEndpoint() {
		return LoginRequestHandler.endpoint;
	}
	
	//----------------------------------------------------------------------------------------------
	@Override
	public Response handle(
			IHTTPSession input, Tenant tenant, Connection dBConn
	) throws IOException {
		Response result = null;
		
		if (tenant != null) {
			result = new Response(Status.BAD_REQUEST, null, null, 0);
		} else {
			try {
				LoginRequestBean request = LoginRequestBean.fromJsonString(getBodyAsString(input));

				CallableStatement stmnt = dBConn.prepareCall("call get_user(?,?)");

				stmnt.setLong(1, request.accBranchId);
				stmnt.setLong(2, request.accId);

				if (stmnt.execute()) {
					ResultSet rs = stmnt.getResultSet();
					rs.first();
					
					String pwd = null;
					
					//TODO implementar o armazenamento e verificação de senhas direito
					{
						//byte[] salt = rs.getBytes(3);
						byte[] rawPwdBytes = rs.getBytes(2);
						
						int pwdLength = 0;
						while (pwdLength < rawPwdBytes.length && rawPwdBytes[pwdLength] != 0)
							pwdLength++;
							
						byte[] pwdBytes = new byte[pwdLength];
						
						System.arraycopy(rawPwdBytes, 0, pwdBytes, 0, pwdLength);
						
						pwd = new String(pwdBytes);
					}
					
					if (pwd.equals(request.usrPwd)) {
						tenant = new Tenant(
								request.accBranchId,
								request.accId,
								rs.getString(1)
						);
						
						byte[] responseBytes = new SessionCookieBean(
								tenant.accBranchId, 
								tenant.accId, 
								tenant.accHolder
						).toJsonString().getBytes();
						
						result = new Response(
								Status.OK,
								"application/json",
								new ByteArrayInputStream(responseBytes),
								responseBytes.length
						);
					} else {
						//wrong password
						result = new Response(Status.UNAUTHORIZED, null, null, 0);
					}
				} else {
					//user not found
					result = new Response(Status.UNAUTHORIZED, null, null, 0);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				result = new Response(Status.INTERNAL_ERROR, null, null, 0);
			}
		}
		
		return result;
	}
	
}
