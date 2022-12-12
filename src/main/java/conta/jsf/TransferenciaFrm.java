package conta.jsf;

import conta.to.ContaTO;
import conta.to.NumeroTO;
import conta.to.TransferenciaTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.*;

// Adaptador JSF
@Named
@SessionScoped
public class TransferenciaFrm implements Serializable {

    private Integer conta1;
    private String descricao1;
    private Integer conta2;
    private String descricao2;
    private BigDecimal valor;

    // Url do sistema rest
    private static final String URL = "http://localhost:8080/";
    // Consumidor rest com json
    private RestTemplate rest;

    public TransferenciaFrm() {
        // configuração do rest para processar json.
        rest = new RestTemplate();
        rest.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    // operações privadas de apoio

    private void limpar1() {
        conta1 = null;
        descricao1 = null;
    }

    private void limpar2() {
        conta2 = null;
        descricao2 = null;
    }

    private void limpar() {
        limpar1();
        limpar2();
    }

    private void erro(String mensagem) {
        var fc = FacesContext.getCurrentInstance();
        var fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, mensagem, "");
        fc.addMessage(null, fm);
    }

    private void aviso(String mensagem) {
        var fc = FacesContext.getCurrentInstance();
        var fm = new FacesMessage(FacesMessage.SEVERITY_INFO, mensagem, "");
        fc.addMessage(null, fm);
    }

    // operações publicas eventos jsf

    public void pesquisaConta1() {
        try {
            // Novo get via rest ----------------
            var to = new NumeroTO(conta1);
            var resp = rest.postForEntity(URL + "/transferencia/getconta", to, ContaTO.class);
            if (resp.getStatusCode() == OK) {
                ContaTO conta = resp.getBody();
                if (conta.correntista == null) {
                    limpar1();
                } else {
                    descricao1 = toIso(conta.getCorrentista()) + " - Saldo R$ " + conta.getSaldo();
                }
            }
            // --------------------
        } catch (Exception e) {
            tratarErroRest(e.getMessage());
        }
    }

    public void pesquisaConta2() {
        try {
            // Novo get via rest ----------------
            var to = new NumeroTO(conta2);
            var resp = rest.postForEntity(URL + "/transferencia/getconta", to, ContaTO.class);
            if (resp.getStatusCode() == OK) {
                ContaTO conta = resp.getBody();
                if (conta.correntista == null) {
                    limpar2();
                } else {
                    descricao2 = toIso(conta.getCorrentista()) + " - Saldo R$ " + conta.getSaldo();
                }
            }
            // --------------------
        } catch (Exception e) {
            tratarErroRest(e.getMessage());
        }
    }

    public void transferir() {
        try {
            try {
                // Nova transfencia via rest ----------------
                var to = new TransferenciaTO(conta1, conta2, valor);
                var req = new HttpEntity<TransferenciaTO>(to, new HttpHeaders());
                var resp = rest.exchange(new URI(URL + "/transferencia/transferir"), PUT, req, String.class);
                if (resp.getStatusCode() == NO_CONTENT) {
                    limpar1();
                    limpar2();
                    valor = null;
                    aviso("Transferência feita com sucesso!");
                }
            } catch (HttpClientErrorException e) {
                var erro = new ResponseEntity<>(e.getResponseBodyAsString(), BAD_REQUEST);
                if (e.getStatusCode().value() == 400) {
                    erro(toIso(erro.getBody()));
                } else {
                    tratarErroRest(toIso(erro.getBody()));
                }
            } catch (Exception e) {
                tratarErroRest(e.getMessage());
            }
            //-----------------------------------------------
        } catch (Exception e) {
            erro(e.getMessage());
        }
    }

    // Novo trata mensgem da comunicação http ----------------
    public void tratarErroRest(String erro) {
        if (erro.contains("Connection refused") || erro.contains("404") || erro.contains("405")) {
            erro("Sistema fora de ar, tenta mais tarde.");
        } else {
            erro("Erro não tratado:" + erro);
        }
    }
    // ---------------------------------------

    // Novo trata string utf8 ----------------
    public static String toIso(String str) {
        var utf8 = Charset.forName("UTF-8");
        var iso88591 = Charset.forName("ISO-8859-1");
        var ib = ByteBuffer.wrap(str.getBytes());
        var data = utf8.decode(ib);
        var outputBuffer = iso88591.encode(data);
        var outputData = outputBuffer.array();
        return new String(outputData);
    }
    // ---------------------------------------

    // gets e sets

    public Integer getConta1() {
        return conta1;
    }

    public void setConta1(Integer conta1) {
        this.conta1 = conta1;
    }

    public String getDescricao1() {
        return descricao1;
    }

    public void setDescricao1(String descricao1) {
        this.descricao1 = descricao1;
    }

    public Integer getConta2() {
        return conta2;
    }

    public void setConta2(Integer conta2) {
        this.conta2 = conta2;
    }

    public String getDescricao2() {
        return descricao2;
    }

    public void setDescricao2(String descricao2) {
        this.descricao2 = descricao2;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
