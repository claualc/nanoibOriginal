package br.usp.larc.nanoib.beans;

import org.json.JSONObject;

/**
 * Resposta do serviço de login, com informações da conta, usuário titular, e o token de sessão a 
 * ser enviado em consultas subsequentes
 *  
 * @author Oscar
 */
public class LoginResponseBean {
    
    public long accBranchId;
    public long accId;
    public String accHolderName;
    public String sessToken;
    
    public LoginResponseBean(long accBranchId, long accId, String accHolder, String sessToken) {
        this.accBranchId = accBranchId;
        this.accId = accId;
        this.accHolderName = accHolder;
        this.sessToken = sessToken;
    }
    
    public static LoginResponseBean fromJSONString(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        
        return new LoginResponseBean(
                obj.getLong("accBranchId"),
                obj.getLong("accId"),
                obj.getString("accHolder"),
                obj.getString("sessToken")
        );
    }

    public String toJsonString() {
        JSONObject obj = new JSONObject();
        
        obj.put("accBranchId", accBranchId);
        obj.put("accId", accId);
        obj.put("accHolderName", accHolderName);
        obj.put("sessToken", sessToken);
        
        return obj.toString();
    }
    
}
