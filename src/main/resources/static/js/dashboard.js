(function () {
    const form = document.getElementById('filtroDashboard');
    if (!form) return;

    const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const erro = document.getElementById('dashboardErro');
    const carregando = document.getElementById('dashboardCarregando');
    let chartVendas;
    let chartStatus;

    function dataLocal(data) {
        const ano = data.getFullYear();
        const mes = String(data.getMonth() + 1).padStart(2, '0');
        const dia = String(data.getDate()).padStart(2, '0');
        return `${ano}-${mes}-${dia}`;
    }

    async function buscar(url) {
        const resposta = await fetch(url, { headers: { Accept: 'application/json' } });
        if (!resposta.ok) {
            const detalhe = await resposta.text();
            throw new Error(detalhe || `Falha ao consultar a API (${resposta.status}).`);
        }
        return resposta.json();
    }

    function queryPeriodo() {
        const params = new URLSearchParams({
            inicio: document.getElementById('inicio').value,
            fim: document.getElementById('fim').value
        });
        return params.toString();
    }

    function desenharVendas(dados) {
        const contexto = document.getElementById('graficoVendas').getContext('2d');
        if (chartVendas) chartVendas.destroy();
        chartVendas = new Chart(contexto, {
            type: 'line',
            data: {
                labels: dados.map(item => item.periodo.slice(8, 10) + '/' + item.periodo.slice(5, 7)),
                datasets: [{
                    label: 'Vendas',
                    data: dados.map(item => Number(item.totalVendas)),
                    borderColor: '#e56b2f',
                    backgroundColor: 'rgba(229,107,47,.13)',
                    pointBackgroundColor: '#e56b2f',
                    borderWidth: 3,
                    pointRadius: 3,
                    fill: true,
                    lineTension: .3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                legend: { display: false },
                scales: {
                    yAxes: [{ ticks: { beginAtZero: true, callback: valor => brl.format(valor) }, gridLines: { color: '#eef0f3' } }],
                    xAxes: [{ gridLines: { display: false } }]
                },
                tooltips: { callbacks: { label: item => brl.format(Number(item.yLabel)) } }
            }
        });
    }

    function desenharStatus(dados) {
        const nomes = {
            RECEBIDO: 'Recebido', EM_PREPARACAO: 'Em preparação', PRONTO: 'Pronto',
            SAIU_PARA_ENTREGA: 'Saiu para entrega', RETIRADO: 'Retirado',
            FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado'
        };
        const cores = ['#e56b2f', '#4776c7', '#39a47a', '#9165bd', '#24a2ad', '#697586', '#d84b4b'];
        const contexto = document.getElementById('graficoStatus').getContext('2d');
        if (chartStatus) chartStatus.destroy();
        chartStatus = new Chart(contexto, {
            type: 'doughnut',
            data: {
                labels: dados.map(item => nomes[item.status] || item.status),
                datasets: [{ data: dados.map(item => item.quantidade), backgroundColor: cores, borderWidth: 0 }]
            },
            options: { responsive: true, maintainAspectRatio: false, legend: { position: 'bottom', labels: { boxWidth: 12, padding: 14 } }, cutoutPercentage: 66 }
        });
    }

    function desenharProdutos(dados) {
        const corpo = document.getElementById('produtosMaisVendidos');
        if (!dados.length) {
            corpo.innerHTML = '<tr><td colspan="4" class="text-muted text-center py-4">Nenhuma venda no período.</td></tr>';
            return;
        }
        corpo.innerHTML = dados.map((produto, indice) => `
            <tr><td><span class="rank-badge">${indice + 1}</span></td>
                <td class="fw-semibold">${escapeHtml(produto.nomeProduto || 'Produto sem nome')}</td>
                <td class="text-end">${produto.quantidadeVendida}</td>
                <td class="text-end">${brl.format(Number(produto.valorVendido))}</td></tr>`).join('');
    }

    function escapeHtml(texto) {
        return String(texto).replace(/[&<>"']/g, caractere => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[caractere]));
    }

    async function atualizar() {
        erro.classList.add('d-none');
        carregando.classList.remove('d-none');
        try {
            const periodo = queryPeriodo();
            const [resumo, vendas, status, produtos] = await Promise.all([
                buscar(`/api/dashboard/resumo?${periodo}`),
                buscar(`/api/dashboard/vendas?${periodo}&agrupamento=dia`),
                buscar(`/api/dashboard/pedidos-por-status?${periodo}`),
                buscar(`/api/dashboard/produtos-mais-vendidos?${periodo}&limite=5`)
            ]);
            document.getElementById('totalVendas').textContent = brl.format(Number(resumo.totalVendas));
            document.getElementById('quantidadePedidos').textContent = resumo.quantidadePedidos;
            document.getElementById('ticketMedio').textContent = brl.format(Number(resumo.ticketMedio));
            document.getElementById('pedidosEmAndamento').textContent = resumo.pedidosEmAndamento;
            desenharVendas(vendas);
            desenharStatus(status);
            desenharProdutos(produtos);
        } catch (e) {
            erro.textContent = `Não foi possível atualizar os dados: ${e.message}`;
            erro.classList.remove('d-none');
        } finally {
            carregando.classList.add('d-none');
        }
    }

    form.addEventListener('submit', evento => {
        evento.preventDefault();
        if (form.reportValidity()) atualizar();
    });

    const hoje = new Date();
    const inicio = new Date(hoje);
    inicio.setDate(hoje.getDate() - 6);
    document.getElementById('inicio').value = dataLocal(inicio);
    document.getElementById('fim').value = dataLocal(hoje);
    atualizar();
    window.setInterval(atualizar, 60000);
})();
