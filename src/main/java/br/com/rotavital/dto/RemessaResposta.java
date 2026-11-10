package br.com.rotavital.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import br.com.rotavital.model.*;

/** Visão completa da remessa usada pela API e pelas telas de rota e monitoramento. */
public record RemessaResposta(
    Long id,
    String codigo,
    Long requisicaoId,
    Long usuarioId,
    String hospital,
    TipoComponente componente,
    TipoSanguineo tipoSanguineo,
    List<BolsaRemessa> bolsas,
    StatusRemessa status,
    ModeloCaixa caixa,
    int quantidadeCaixas,
    FaixaTermica faixa,
    LocalDateTime monitoramentoIniciadoEm,
    LocalDateTime monitoramentoEncerradoEm,
    LocalDateTime saidaEm,
    LocalDateTime entregueEm,
    LocalDateTime desvioIniciadoEm,
    PlanoDeRota plano,
    AlertaTermico alerta,
    LeituraResposta ultimaLeitura,
    List<LeituraResposta> leituras
) {
    public static RemessaResposta de(Remessa remessa) {
        Requisicao requisicao = remessa.getRequisicao();
        AlertaTermico alerta = remessa.isEmAlerta()
            ? new AlertaTermico(remessa.getAlertaDesvioIniciadoEm(), remessa.getAlertaEmitidoEm(),
                remessa.getAlertaReconhecidoEm(), remessa.getAlertaReconhecidoPor())
            : null;
        List<LeituraResposta> leituras = remessa.getLeituras().stream().map(LeituraResposta::de).toList();
        return new RemessaResposta(remessa.getId(), remessa.getCodigo(), requisicao.getId(),
            requisicao.getUsuario() == null ? null : requisicao.getUsuario().getId(),
            requisicao.getHospital(), requisicao.getComponente(), requisicao.getTipoSanguineo(),
            requisicao.getBolsas().stream().map(BolsaRemessa::de).toList(),
            remessa.getStatus(), remessa.getCaixa(), remessa.getQuantidadeCaixas(), remessa.getFaixa(),
            remessa.getMonitoramentoIniciadoEm(), remessa.getMonitoramentoEncerradoEm(),
            remessa.getSaidaEm(), remessa.getEntregueEm(), remessa.getDesvioIniciadoEm(),
            remessa.getPlano(), alerta, leituras.isEmpty() ? null : leituras.get(leituras.size() - 1),
            leituras);
    }

    public int getQuantidadeBolsas() {
        return bolsas.size();
    }

    public int getCapacidade() {
        return caixa.getCapacidade() * quantidadeCaixas;
    }

    public String getDescricaoDaCaixa() {
        return quantidadeCaixas == 1 ? caixa.getRotulo() : quantidadeCaixas + " × " + caixa.getRotulo();
    }

    public boolean isPermitePlanejamento() {
        return status.permitePlanejamento();
    }

    public boolean isMonitorada() {
        return status.monitorada();
    }

    public boolean isEmAlerta() {
        return alerta != null;
    }

    public boolean isEmDesvio() {
        return desvioIniciadoEm != null;
    }

    /** Minutos entre o início do desvio atual e a última leitura; nulo sem desvio em andamento. */
    public Long getMinutosForaDaFaixa() {
        if (desvioIniciadoEm == null || ultimaLeitura == null) {
            return null;
        }
        return Duration.between(desvioIniciadoEm, ultimaLeitura.registradaEm()).toMinutes();
    }

    /** Duração do desvio em texto: "há menos de 1 minuto", "há 1 minuto", "há 5 minutos". */
    public String getTempoForaDaFaixa() {
        Long minutos = getMinutosForaDaFaixa();
        if (minutos == null) {
            return "";
        }
        return minutos < 1 ? "há menos de 1 minuto" : minutos == 1 ? "há 1 minuto" : "há " + minutos + " minutos";
    }

    /** Códigos das bolsas em texto corrido: "B1", "B1 e B2", "B1, B2 e B3". */
    public String getCodigosBolsasTexto() {
        List<String> codigos = bolsas.stream().map(BolsaRemessa::codigo).toList();
        if (codigos.size() <= 1) {
            return String.join("", codigos);
        }
        return String.join(", ", codigos.subList(0, codigos.size() - 1)) + " e " + codigos.get(codigos.size() - 1);
    }

    public List<EtapaRemessa> getEtapas() {
        boolean bloqueada = status == StatusRemessa.PENDENTE_AVALIACAO;
        List<EtapaRemessa> etapas = new ArrayList<>();
        etapas.add(new EtapaRemessa("Reserva e início do monitoramento", monitoramentoIniciadoEm, "concluida"));
        etapas.add(new EtapaRemessa("Rota planejada", plano == null ? null : plano.getPlanejadoEm(),
            plano != null ? "concluida" : bloqueada ? "bloqueada" : "atual"));
        etapas.add(new EtapaRemessa("Saída do hemocentro", saidaEm,
            saidaEm != null ? "concluida" : bloqueada ? "bloqueada" : status == StatusRemessa.PLANEJADA ? "atual" : "pendente"));
        etapas.add(new EtapaRemessa("Entrega no hospital", entregueEm,
            entregueEm != null ? "concluida" : bloqueada ? "bloqueada"
                : status == StatusRemessa.EM_TRANSPORTE ? "atual" : "pendente"));
        return etapas;
    }
}
