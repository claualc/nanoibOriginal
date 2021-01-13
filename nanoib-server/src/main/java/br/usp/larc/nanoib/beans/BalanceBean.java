package br.usp.larc.nanoib.beans;

import java.math.BigDecimal;

import org.json.JSONObject;

/**
 * Bean de resposta na requisição de obtenção de saldo
 *  
 * @author Oscar
 */
public class BalanceBean {
    
    public BigDecimal balance;
    public long opCount;
    
    public BalanceBean(BigDecimal balance, long opCount) {
        this.balance = balance;
        this.opCount = opCount;
    }

    public String toJsonString() {
        JSONObject obj = new JSONObject();
        
        obj.put("balance", balance);
        obj.put("opCount", opCount);
        
        return obj.toString();
    }
    
}
