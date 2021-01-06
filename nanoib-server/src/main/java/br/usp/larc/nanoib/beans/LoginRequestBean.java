package br.usp.larc.nanoib.beans;

import org.json.JSONObject;

/**
 * Bean da requisicão de login 
 *  
 * @author oscar
 */
public class LoginRequestBean {
	
	public long accBranchId;
	public long accId;
	public String usrPwd;
	
	public LoginRequestBean(long accBranchId, long accId, String usrPwd) {
		this.accBranchId = accBranchId;
		this.accId = accId;
		this.usrPwd = usrPwd;
	}

	public static LoginRequestBean fromJsonString(String jsonString) {
		JSONObject obj = new JSONObject(jsonString);
		
		return new LoginRequestBean(
				obj.getLong("accBranchId"),
				obj.getLong("accId"),
				obj.getString("usrPwd")
		);
	}
	
}
