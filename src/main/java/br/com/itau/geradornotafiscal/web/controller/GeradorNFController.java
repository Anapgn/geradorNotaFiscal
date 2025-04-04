package br.com.itau.geradornotafiscal.web.controller;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@Slf4j
@RequestMapping("/api/pedido")
public class GeradorNFController {

	@Autowired
	private GeradorNotaFiscalService notaFiscalService;



	@PostMapping("/gerarNotaFiscal")
	public ResponseEntity<NotaFiscal> gerarNotaFiscal(@RequestBody Pedido pedido) {
		// Lógica de processamento do pedido
		// Aqui você pode realizar as operações desejadas com o objeto Pedido
		// Exemplo de retorno

		StopWatch watch = new StopWatch();
		watch.start();
		log.info("Iniciando processo de geracao nota fiscal");

		NotaFiscal notaFiscal = notaFiscalService.gerarNotaFiscal(pedido);

		log.info("Nota fiscal gerada com sucesso para o pedido: {} " , pedido.getIdPedido());
		watch.stop();
		log.info("Tempo de processamento total: {} ms", watch.getTotalTimeNanos() / 1_000_000.0);
		return new ResponseEntity<>(notaFiscal, HttpStatus.OK);

	}

}
