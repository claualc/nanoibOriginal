package br.usp.larc.nanoib.beans;

import java.math.BigDecimal;

import org.json.JSONObject;

/**
 * Bean enviado no corpo da requisição de transferência entre contas
 * 
 * @author Oscar
 */
public class TransferRequestBean {
	
	public long destAccBranchId;
	public long destAccId;
	public BigDecimal value;
	public String comment;
	
	public TransferRequestBean(
			long destAccBranchId, long destAccId, BigDecimal value, String comment
	) {
		this.destAccBranchId = destAccBranchId;
		this.destAccId = destAccId;
		this.value = value;
		this.comment = comment;
	}
	
	public static TransferRequestBean fromJsonString(String jsonString) {
		JSONObject obj = new JSONObject(jsonString);
		
		return new TransferRequestBean(
				obj.getLong("destAccBranchId"),
				obj.getLong("destAccId"),
				obj.getBigDecimal("value"),
				obj.getString("comment")
		);
	}
	
}
