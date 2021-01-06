package br.usp.larc.nanoib.beans;

import org.json.JSONObject;

/**
 * Bean do cookie de sessão, a ser enviado como resposta na requisição de login (no corpo e no
 * header "set-cookie", e evidentemente no header "cookie" das requisições subsequentes 
 * 
 * TODO adicionar campo binário do MAC
 *  
 * @author Oscar
 */
public class SessionCookieBean {
	
	public long accBranchId;
	public long accId;
	public String accHolder;
	
	public SessionCookieBean(long accBranchId, long accId, String accHolder) {
		this.accBranchId = accBranchId;
		this.accId = accId;
		this.accHolder = accHolder;
	}
	
	public static SessionCookieBean fromJSONString(String jsonString) {
		JSONObject obj = new JSONObject(jsonString);
		
		return new SessionCookieBean(
				obj.getLong("accBranchId"),
				obj.getLong("accId"),
				obj.getString("accHolder")
		);
	}

	public String toJsonString() {
		JSONObject obj = new JSONObject();
		
		obj.put("accBranchId", accBranchId);
		obj.put("accId", accId);
		obj.put("accHolder", accHolder);
		
		return obj.toString();
	}
	
}
