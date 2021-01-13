package br.usp.larc.nanoib;

import java.util.HashMap;
import java.util.Map;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.util.IHandler;

import br.usp.larc.nanoib.handlers.BalanceRequestHandler;
import br.usp.larc.nanoib.handlers.LoginRequestHandler;
import br.usp.larc.nanoib.handlers.RequestHandler;
import br.usp.larc.nanoib.handlers.StatementRequestHandler;
import br.usp.larc.nanoib.handlers.TransferRequestHandler;
import br.usp.larc.nanoib.handlers.RequestHandler.Endpoint;

/**
 * Classe principal do servidor da aplicação nanoIB. Parametrizável com os dados de hostname e porta
 * da aplicação e dados de conexão à base de dados. Mais de uma instância pode ser criada, desde que 
 * escutem por conexões em portas distintas.
 * 
 * Esta classe é implementada como uma extensão do mini-servidor de aplicação NanoHTTPD, na qual são
 * registrados interceptores de requisição e um handler, que despacha o atendimento a diferentes 
 * RequestHandlers em função do endpoint de cada requisição.
 * 
 * @author oscar
 */
public class NanoIBHTTPD extends NanoHTTPD {
    
    //Class attributes
    //----------------------------------------------------------------------------------------------
    public static class DBConnInfo {
        public String host;
        public int port;
        public String name;
        public String user;
        public String pwd;
        
        public DBConnInfo(String host, int port, String name, String user, String pwd) {
            this.host = host;
            this.port = port;
            this.name = name;
            this.user = user;
            this.pwd = pwd;
        }
    }
    
    //Object attributes
    //----------------------------------------------------------------------------------------------
    private DBConnInfo dBConnInfo;
    
    private Map<RequestHandler.Endpoint, RequestHandler> requestHandlers;
    
    //Constructors
    //----------------------------------------------------------------------------------------------
    public NanoIBHTTPD(
            int port, String dBHost, int dBPort, String dBName, String dBUser, String dBPwd
    ) {
        this(null, port, dBHost, dBPort, dBName, dBUser, dBPwd);
    }
    
    public NanoIBHTTPD(
            String hostName, int port,
            String dBHost, int dBPort, String dBName, String dBUser, String dBPwd
    ) {
        super(hostName, port);
        
        dBConnInfo = new DBConnInfo(dBHost, dBPort, dBName, dBUser, dBPwd);
        
        requestHandlers = new HashMap<RequestHandler.Endpoint, RequestHandler>();
        
        requestHandlers.put(
                LoginRequestHandler.endpoint,
                new LoginRequestHandler(dBConnInfo)
        );
        requestHandlers.put(
                BalanceRequestHandler.endpoint,
                new BalanceRequestHandler(dBConnInfo)
        );
        requestHandlers.put(
                StatementRequestHandler.endpoint,
                new StatementRequestHandler(dBConnInfo)
        );
        requestHandlers.put(
                TransferRequestHandler.endpoint,
                new TransferRequestHandler(dBConnInfo)
        );
        
        addHTTPInterceptor(knownEndpointsInterceptor);
        
        setHTTPHandler(requestDispatcher);
    }
    
    //Interceptors
    //----------------------------------------------------------------------------------------------
    /**
     * Filtro de endpoints conhecidos
     */
    private IHandler<IHTTPSession, Response> knownEndpointsInterceptor = 
            new IHandler<IHTTPSession, Response>() {
        @Override
        public Response handle(IHTTPSession input) {
            Response response = null;
            
            Endpoint key = new Endpoint(input.getUri(), input.getMethod());
            if (!requestHandlers.containsKey(key))
                response = new Response(Status.NOT_FOUND, null, null, 0);
                
            return response;
        }
    };
    
    //Dispatcher
    //----------------------------------------------------------------------------------------------
    /**
     * Redireciona o atendimento da requisição ao RequestHandler responsável pelo endpoint 
     */
    private IHandler<IHTTPSession, Response> requestDispatcher = 
            new IHandler<IHTTPSession, Response>() {
        
        @Override
        public Response handle(IHTTPSession input) {
            return requestHandlers.get(
                    new RequestHandler.Endpoint(
                            input.getUri(),
                            input.getMethod()
                    )
            ).handle(input);
        }
        
    };
    
}
