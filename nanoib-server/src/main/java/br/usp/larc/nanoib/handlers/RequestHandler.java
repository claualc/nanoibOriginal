package br.usp.larc.nanoib.handlers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.util.IHandler;

import br.usp.larc.nanoib.NanoIBHTTPD;
//import br.usp.larc.nanoib.beans.SessionCookieBean;
import br.usp.larc.nanoib.beans.SessionCookieBean;

/**
 * RequestHandler abstrato que todos os demais handlers devem estender
 * 
 * @author Oscar
 */
public abstract class RequestHandler implements IHandler<IHTTPSession, Response> {
    
	//----------------------------------------------------------------------------------------------
    /**
     * This class represents the endpoint to which a handler, an implementation of this abstract 
     * class, is required to in 
     * */
    public static class Endpoint {
        String path;
        Method method;
        boolean _public;
        
        public Endpoint(String path, Method method) {
            this.path = path;
            this.method = method;
            this._public = false;
        }
        
        Endpoint(String path, Method method, boolean _public) {
            this.path = path;
            this.method = method;
            this._public = _public;
        }
        
        public boolean isPublic() {
        	return _public;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(path, method);
        }
        
        @Override
        public boolean equals(Object obj) {
            boolean result = false;
            
            if (obj instanceof Endpoint) {
                Endpoint another = (Endpoint) obj;
                
                result = path.equals(another.path) && method == another.method;
            }
            
            return result;
        }
    }
    
    //----------------------------------------------------------------------------------------------    
    /**
     * This class represents the user to which a request is associated.
     */
    protected static class Tenant {
        long accBranchId;
        long accId;
        String accHolder;
        
		public Tenant(long accBranchId, long accId, String accHolder) {
			this.accBranchId = accBranchId;
			this.accId = accId;
			this.accHolder = accHolder;
		}
		
		public Tenant(SessionCookieBean bean) {
			this.accBranchId = bean.accBranchId;
			this.accId = bean.accId;
			this.accHolder = bean.accHolder;
		}
    }
    
    //----------------------------------------------------------------------------------------------
    protected NanoIBHTTPD.DBConnInfo dBConnInfo;
    
    //----------------------------------------------------------------------------------------------
    @SuppressWarnings("unused")
    private RequestHandler() {
    }
    
    protected RequestHandler(NanoIBHTTPD.DBConnInfo dBConnInfo) {
        this.dBConnInfo = dBConnInfo;
    }
    
    //----------------------------------------------------------------------------------------------
    @Override
    public final Response handle(IHTTPSession input) {
    	Response result = null;
    	
    	Connection dBConn = null;
    	
        try {
        	dBConn = getDBConnection();
        	
            Tenant tenant = null;
            
            if (input.getHeaders().containsKey("nib-ten")) {
                String ten = input.getHeaders().get("nib-ten");
                //TODO testar a consistência do cookie
                tenant = new Tenant(
                		SessionCookieBean.fromJSONString(ten)
                );
            }
            
            result = handle(input, tenant, dBConn);
        } catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
            if (dBConn != null) {
                try {
                    dBConn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return result;
    }

    //----------------------------------------------------------------------------------------------    
    public abstract Response handle(
    		IHTTPSession input, Tenant tenant, Connection dBConn
    ) throws IOException;
    
    //----------------------------------------------------------------------------------------------    
    protected String getBodyAsString(IHTTPSession input) throws IOException {
    	int bodyLenght = Integer.parseInt(input.getHeaders().get("content-length"));
    	
    	StringBuffer sb = new StringBuffer();
    	
    	//TODO this is not the best implementation but it doesn't matter now
    	InputStreamReader isr = new InputStreamReader(input.getInputStream());
    	for (int i = 0; i < bodyLenght; i++)
    		sb.append((char) isr.read());
    	
    	return sb.toString();
    }
    
    //----------------------------------------------------------------------------------------------    
    protected Connection getDBConnection() throws SQLException {
    	final String dBConnString =  String.format(
    			"jdbc:mariadb://%s:%d/%s?user=%s&password=%s", 
    			dBConnInfo.host,
    			dBConnInfo.port,
    			dBConnInfo.name,
    			dBConnInfo.user,
    			dBConnInfo.pwd
    	);

    	return DriverManager.getConnection(dBConnString);
    }
    
    //----------------------------------------------------------------------------------------------
//    protected void setRemoteTenant(Response response, Tenant tenant) {
//    	response.addHeader(
//				"Set-Cookie",
//				"TEN = " + 
//				new SessionCookieBean(
//						tenant.accBranchId, tenant.accId, tenant.accHolder
//				).toJsonString()
//		);
//    }
    
    //----------------------------------------------------------------------------------------------
    public abstract Endpoint getEndpoint();
    
    //----------------------------------------------------------------------------------------------    
    @Override
    public int hashCode() {
        return getEndpoint().hashCode();
    }
    
    //----------------------------------------------------------------------------------------------    
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        
        if (obj instanceof RequestHandler)
            result = getEndpoint().equals(((RequestHandler) obj).getEndpoint());
        
        return result;
    }
    
}
