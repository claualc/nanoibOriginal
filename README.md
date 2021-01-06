# nanoIB

nanoIB é uma aplicação web fictícia, que simula o funcionamento básico de aplicações de Internet/Mobile Banking. Destina-se a assistir o desenvolvimento de técnicas de detecção e mitigação de backdoors em aplicações web, servindo como um objeto de análise livre das complexidades operacionais de uma aplicação real.

A arquitetura do nanoIB é formada por três elementos:

* um conjunto de páginas HTML dinâmicas;
* uma aplicação servidora que executa um web service RESTful que é consultado pelas páginas dinâmicas;
* um conjunto de stored procedures executadas em DBMS, simulando o funcionamento de transações classicamente executadas em mainframes de ambientes computacionais bancários.

A aplicação servidora é standalone, e foi escrita em Java 1.8. Dada a necessidade de manter baixa a complexidade da aplicação, não foi feito o uso de nenhum framework de desenvolvimento Web. Em vez disso, a aplicação servidora foi desenvolvida como uma extensão do [NanoHttpd](https://github.com/NanoHttpd/nanohttpd), um servidor extensível de aplicações Web Java, construído diretamente sobre a API Sockets Java.

## Estrutura do repositório

O repositório do projeto tem a seguinte estrutura:

* /json-java: biblioteca [JSON-Java](https://github.com/stleary/JSON-java) para serialização e deserialização de strings de objetos JSON 
* /nanohttpd-core: módulo central da aplicação [NanoHttpd](https://github.com/NanoHttpd/nanohttpd)
* /nanoibserver: aplicação servidora que estende o NanoHTTPD com os Web Services REST do nanoIB
* /resources: páginas HTML e script de criação da base de dados

Optou-se por diretamente copiar neste repositório conteúdo dos repositórios dos projetos JSON-Java e NanoHTTPD, de forma a evitar a complexidade da utilização de submódulos git.

## Requisitos

O nanoIB foi desenvolvido em ambiente computacional formado pelos elementos seguintes (assume-se que, para executar e estender a aplicação, devem ser utilizados elementos compatíveis com esses):

* Ubuntu 20.04
* Apache2
* Eclipse 3.18
* MariaDB 10.3
* JDK 1.8
* Maven 3.6.3

## Instalação e Execução

Depois de clonar o repositório, deve-se executar o arquivo de criação da base de dados "dbcreation.sql", ao, por exemplo, executar os seguintes comandos:

```bash
cd <repo-parent-folder>/nanoib/resources
sudo mysql < dbcreation.sql
```

Em seguida, deve-se habilitar o modo reverse-proxy do Apache2:

```bash
sudo a2enmod proxy
sudo a2enmod proxy_http
```

E então deve-se editar o arquivo de configuração do Apache2:

```bash
sudo nano /etc/apache2/sites-available/000-default.conf
```

Adicionando-se o seguinte conteúdo ao elemento "VirtualHost":

```xml
<VirtualHost *:80>
    ...
    ProxyPreserveHost On
    
    ProxyPass /nanoib/svcs http://127.0.0.1:8080
    ProxyPassReverse /nanoib/svcs http://127.0.0.1:8080
    ...
</VirtualHost>
```

Então, deve-se reiniciar o serviço do Apache:

```bash
sudo systemctl restart apache2
```

Em seguida, deve-se copiar os arquivos HTML para o diretório "www" do Apache, e ajustar-se a permissão de leitura dos mesmos:

```bash
sudo cp * /var/www/html/nanoib
sudo mkdir /var/www/html/
cd /var/www/html/nanoib
sudo chmod 644 *
```

Por fim, deve-se importar o código da aplicação servidora no Eclipse como um projeto Maven. Então, deve-se clicar com o botão direito sobre o projeto "nanoib-project", escolher a opção "Run as -> Maven build", com goal "compile". Depois de compilada, deve-se rodar a aplicação a partir da classe "Main", e acessá-la através de "http://localhost/nanoib". Pode ser necessário ajustar configurações de firewall, caso se queira acessar a aplicação através da rede. Credenciais de teste podem ser lidos do script de criação da base de dados.