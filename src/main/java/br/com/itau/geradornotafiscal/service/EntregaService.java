package br.com.itau.geradornotafiscal.service;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.port.out.EntregaIntegrationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class EntregaService {
    public void agendarEntrega(NotaFiscal notaFiscal) {

        try {
            //Simula o agendamento da entrega
            log.info("Agendando a entrega");
            Thread.sleep(150);
            new EntregaIntegrationPort().criarAgendamentoEntrega(notaFiscal);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
