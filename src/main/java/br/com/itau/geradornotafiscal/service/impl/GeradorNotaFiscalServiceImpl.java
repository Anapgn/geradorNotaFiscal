package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.*;
import br.com.itau.geradornotafiscal.service.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@AllArgsConstructor
public class GeradorNotaFiscalServiceImpl implements GeradorNotaFiscalService {
	@Autowired
	CalculadoraAliquotaProdutoService calculadoraAliquotaProdutoService;
	@Autowired
	private EstoqueService estoqueService;

	@Autowired
	private RegistroService registroService;

	@Autowired
	private EntregaService entregaService;

	@Autowired
	private FinanceiroService financeiroService;

	@Override
	public NotaFiscal gerarNotaFiscal(Pedido pedido) {

		List<ItemNotaFiscal> itemNotaFiscalList = new ArrayList<>();
		Destinatario destinatario = pedido.getDestinatario();
		TipoPessoa tipoPessoa = destinatario.getTipoPessoa();

		// Calculo valor total itens
		double valorTotalItensCalculado = calcularValorTotalItens(pedido.getItens());

		// Obter Aliquota
		double aliquota = obterAliquotaPorTipoPessoa(destinatario, valorTotalItensCalculado);
		itemNotaFiscalList = calculadoraAliquotaProdutoService.calcularAliquota(pedido.getItens(), aliquota);

		// Calculo para frete
		double valorFrete = pedido.getValorFrete();
		double valorFreteComPercentual = calcularFretePorRegiao(destinatario, valorFrete);


		// Criacao Nota Fiscal
		String idNotaFiscal = UUID.randomUUID().toString();

		NotaFiscal notaFiscal = NotaFiscal.builder()
				.idNotaFiscal(idNotaFiscal)
				.data(LocalDateTime.now())
				.valorTotalItens(valorTotalItensCalculado)
				.valorFrete(valorFreteComPercentual)
				.itens(itemNotaFiscalList)
				.destinatario(pedido.getDestinatario())
				.build();

		// Execucao sincrona processa pedido em media de 1.5 segundos
        /* estoqueService.enviarNotaFiscalParaBaixaEstoque(notaFiscal);
        registroService.registrarNotaFiscal(notaFiscal);
        entregaService.agendarEntrega(notaFiscal);
        financeiroService.enviarNotaFiscalParaContasReceber(notaFiscal);
        */

		processarServicosNotaFiscal(notaFiscal);

		return notaFiscal;
	}


	private double obterAliquotaPorTipoPessoa(Destinatario destinatario, double valorTotalItensCalculado) {
		TipoPessoa tipoPessoa = destinatario.getTipoPessoa();
		double aliquota;

		log.info("Obtendo aliquota por tipo de pessoa: {}", tipoPessoa);

		if (tipoPessoa == TipoPessoa.FISICA) {
			aliquota = obterAliquotaPessoaFisica(valorTotalItensCalculado);
		} else if (tipoPessoa == TipoPessoa.JURIDICA) {
			aliquota = obterAliquotaPessoaJuridica(destinatario, valorTotalItensCalculado);
		} else {
			aliquota = 0.0;
		}

		log.info("Obtencao aliquota por tipo de pessoa finalizado: Aliquota {}", aliquota);
		return aliquota;
	}

	private double obterAliquotaPessoaFisica(double valorTotalItensCalculado) {

		if (valorTotalItensCalculado < 500) {
			return 0.0;
		} else if (valorTotalItensCalculado <= 2000) {
			return 0.12;
		} else if (valorTotalItensCalculado <= 3500) {
			return 0.15;
		} else {
			return 0.17;
		}
	}

	private double obterAliquotaPessoaJuridica(Destinatario destinatario, double valorTotalItensCalculado) {
		RegimeTributacaoPJ regimeTributacao = destinatario.getRegimeTributacao();

		switch (regimeTributacao) {
			case SIMPLES_NACIONAL:
				if (valorTotalItensCalculado < 1000) {
					return 0.03;
				} else if (valorTotalItensCalculado <= 2000) {
					return 0.07;
				} else if (valorTotalItensCalculado <= 5000) {
					return 0.13;
				} else {
					return 0.19;
				}
			case LUCRO_REAL:
				if (valorTotalItensCalculado < 1000) {
					return 0.03;
				} else if (valorTotalItensCalculado <= 2000) {
					return 0.09;
				} else if (valorTotalItensCalculado <= 5000) {
					return 0.15;
				} else {
					return 0.20;
				}
			case LUCRO_PRESUMIDO:
				if (valorTotalItensCalculado < 1000) {
					return 0.03;
				} else if (valorTotalItensCalculado <= 2000) {
					return 0.09;
				} else if (valorTotalItensCalculado <= 5000) {
					return 0.16;
				} else {
					return 0.20;
				}
			default:
				throw new IllegalArgumentException("Regime de tributação desconhecido: " + regimeTributacao);
		}
	}


	private double calcularFretePorRegiao(Destinatario destinatario, double valorFrete) {


		Regiao regiao = destinatario.getEnderecos().stream()
				.filter(endereco -> endereco.getFinalidade() == Finalidade.ENTREGA || endereco.getFinalidade() == Finalidade.COBRANCA_ENTREGA)
				.map(Endereco::getRegiao)
				.findFirst()
				.orElse(null);

		log.info("Calculando Frete com Regiao: {}", regiao);

		double valorFreteComPercentual = 0;

		if (regiao == Regiao.NORTE) {
			valorFreteComPercentual = valorFrete * 1.08;
		} else if (regiao == Regiao.NORDESTE) {
			valorFreteComPercentual = valorFrete * 1.085;
		} else if (regiao == Regiao.CENTRO_OESTE) {
			valorFreteComPercentual = valorFrete * 1.07;
		} else if (regiao == Regiao.SUDESTE) {
			valorFreteComPercentual = valorFrete * 1.048;
		} else if (regiao == Regiao.SUL) {
			valorFreteComPercentual = valorFrete * 1.06;
		}

		log.info("Calculo Frete com Regiao finalizado: R${}", valorFreteComPercentual);
		return valorFreteComPercentual;


	}

	public double calcularValorTotalItens(List<Item> itens) {
		double total = 0.0;

		for (Item item : itens) {
			total += item.getValorUnitario() * item.getQuantidade();
		}
		return total;
	}

	@Async
	public void processarServicosNotaFiscal(NotaFiscal notaFiscal) {
		// Execucao assincrona processa pedido em media de  milissegundos
		CompletableFuture.runAsync(() -> estoqueService.enviarNotaFiscalParaBaixaEstoque(notaFiscal));
		CompletableFuture.runAsync(() -> registroService.registrarNotaFiscal(notaFiscal));
		CompletableFuture.runAsync(() -> entregaService.agendarEntrega(notaFiscal));
		CompletableFuture.runAsync(() -> financeiroService.enviarNotaFiscalParaContasReceber(notaFiscal));


	}
}