package br.com.mtmogi.mtmogi.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import com.google.gson.Gson;

import br.com.mtmogi.mtmogi.dto.SalarioDescontosDTO;
import br.com.mtmogi.mtmogi.dto.ServidorDTO;
import br.com.mtmogi.mtmogi.model.SalarioDesconto;
import br.com.mtmogi.mtmogi.model.Servidor;
import br.com.mtmogi.mtmogi.repository.MtmogiRepository;
import br.com.mtmogi.mtmogi.repository.SalarioRepositorio;
import br.com.mtmogi.mtmogi.utils.GeneralInfoServidores;
import br.com.mtmogi.mtmogi.utils.GeneralJsonInfoSalario;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor

@Controller
public class CrawlerController {

	private final RestTemplate restTemplate;
	private final MtmogiRepository servidorRepositorio;
	private final SalarioRepositorio salarioRepositorio;

	@GetMapping("/admin/force_update")
	public @ResponseBody String teste() {
		String todosColaboradores = restTemplate
				.getForObject("http://www.licitacao.pmmc.com.br/Transparencia/vencimentos2", String.class);
		String detalhamentoSalario = "";
		List<SalarioDesconto> salarios = new ArrayList<>();
		List<Servidor> todosServidores = new ArrayList<>();

		Gson jsonDetalhamento = new Gson();

		Gson jsonServidores = new Gson();
		GeneralInfoServidores generalInfoServ = jsonServidores.fromJson(todosColaboradores,
				GeneralInfoServidores.class);
		
		
		for (ServidorDTO servidor : generalInfoServ.getServidores()) {
			Servidor servidorNovo = new Servidor();
			servidorNovo.setCargo(servidor.getCargo());
			servidorNovo.setNome(servidor.getNome());
			servidorNovo.setRgf(servidor.getRgf());
			todosServidores.add(servidorNovo);

			detalhamentoSalario = restTemplate.getForObject(
					"http://www.licitacao.pmmc.com.br/Transparencia/detalhamento?rgf=" + servidor.getRgf(),
					String.class);
			GeneralJsonInfoSalario generalInfoDeta = jsonDetalhamento.fromJson(detalhamentoSalario,
					GeneralJsonInfoSalario.class);

			salarios.addAll(detalhamentoCompleto(servidorNovo, "RENDIMENTOS", generalInfoDeta.rendimentos,
					generalInfoDeta.regime, generalInfoDeta.referencia));

			salarios.addAll(detalhamentoCompleto(servidorNovo, "DESCONTOS", generalInfoDeta.descontos,
					generalInfoDeta.regime, generalInfoDeta.referencia));

			salarios.addAll(detalhamentoCompleto(servidorNovo, "OUTROS_DESCONTOS", generalInfoDeta.outros,
					generalInfoDeta.regime, generalInfoDeta.referencia));
			System.out.println("Servidor salvo com sucesso!");
		}
		servidorRepositorio.saveAll(todosServidores);
		salarioRepositorio.saveAll(salarios);
		System.out.println("Webcrawler concluido com sucesso");
		
		return "WebCrawler concluído";
	}

	private List<SalarioDesconto> detalhamentoCompleto(Servidor servidorNovo, String tipo,
			List<SalarioDescontosDTO> generalInfoDeta, String regime, String referencia) {

		List<SalarioDesconto> vencimentosDescontos = new ArrayList<SalarioDesconto>();
		if (!(generalInfoDeta == null)) {
			for (SalarioDescontosDTO vencimentos : generalInfoDeta) {
				SalarioDesconto vencimentosNovo = new SalarioDesconto();
				vencimentosNovo.setRegime(regime);
				vencimentosNovo.setDescricao(vencimentos.getNome());
				vencimentosNovo.setValor(new BigDecimal(vencimentos.getValor().replace(".", "").replace(",", ".")));
				vencimentosNovo.setServidor(servidorNovo);
				vencimentosNovo.setTipo(tipo);
				vencimentosNovo.setReferencia(referencia);
				vencimentosNovo.setDataAtualizacao(Calendar.getInstance());
				vencimentosDescontos.add(vencimentosNovo);
			}
			return vencimentosDescontos;
		}
		return vencimentosDescontos;
	}
}
