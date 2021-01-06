package br.usp.larc.nanoib.beans;

import java.math.BigDecimal;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Bean de item de lançamento de extrato, enviado em lista no corpo da resposta à requisição de
 * obtenção de extrato 
 * 
 * @author Oscar
 */
public class StatementItemBean {
	
    public long opCount;
    public String timestamp;
    public boolean in_out;
    public BigDecimal value;
    public String comment;
    public long cptpAccBranchId;
    public long cptpAccId;
    public String ctptName;
    
	public StatementItemBean(
			long opCount, String timestamp, boolean in_out, BigDecimal value, String comment,
			long cptpAccBranchId, long cptpAccId, String ctptName
	) {
		this.opCount = opCount;
		this.timestamp = timestamp;
		this.in_out = in_out;
		this.value = value;
		this.comment = comment;
		this.cptpAccBranchId = cptpAccBranchId;
		this.cptpAccId = cptpAccId;
		this.ctptName = ctptName;
	}
	
	public static String toJsonString(List<StatementItemBean> listToJsonize) {
		JSONArray arr = new JSONArray();
		
		for (StatementItemBean item : listToJsonize) {
			JSONObject obj = new JSONObject();

			obj.put("opCount", item.opCount);
			obj.put("timestamp", item.timestamp);
			obj.put("in_out",item.in_out);
			obj.put("value",item.value);
			obj.put("comment",item.comment);
			obj.put("cptpAccBranchId",item.cptpAccBranchId);
			obj.put("cptpAccId",item.cptpAccId);
			obj.put("ctptName",item.ctptName);
			
			arr.put(obj);
		}
		
		return arr.toString();
	}
	
}
