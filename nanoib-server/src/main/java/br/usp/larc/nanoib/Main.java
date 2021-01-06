package br.usp.larc.nanoib;

import org.nanohttpd.util.ServerRunner;

/**
 * Classe de entrada para configuração da aplicação por linha de comando
 * 
 * TODO basicamente tudo 
 * 
 * @author Oscar
 */
public class Main {
	
	public static void main(String[] args) {
		ServerRunner.executeInstance(
				new NanoIBHTTPD(8080, "localhost", 3306, "nanoib", "nanoibserver", "pwd")
		);
	}
	
}
