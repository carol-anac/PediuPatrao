# Dashboard Administrativo do Pediu Patrão

## Escopo

O dashboard consulta os pedidos persistidos no MongoDB e apresenta vendas, pedidos por situação e produtos mais vendidos. O período padrão da API são os últimos 30 dias. A tela inicia com os últimos sete dias, permite alterar as datas e atualiza os dados novamente a cada minuto. Os pedidos cancelados não entram no faturamento, na quantidade de pedidos considerados, no ticket médio nem no ranking de produtos. A distribuição por status inclui os cancelados. O valor vendido por produto é bruto, calculado pelos subtotais dos itens antes do desconto do pedido.

## Requisitos

### Funcionais

- RF01 — Exibir total de vendas, quantidade de pedidos considerados, ticket médio e quantidade de pedidos em andamento.
- RF02 — Exibir vendas agrupadas por dia em gráfico de linha.
- RF03 — Exibir quantidade de pedidos por status em gráfico de rosca.
- RF04 — Exibir os cinco produtos mais vendidos por quantidade, com valor bruto vendido.
- RF05 — Permitir filtrar as consultas por data inicial e final. A API também permite escolher agrupamento diário ou mensal e o limite do ranking.
- RF06 — Consumir os endpoints REST da própria aplicação, protegidos por autenticação e perfil.
- RF07 — Atualizar ao aplicar o filtro e automaticamente a cada 60 segundos; apresentar mensagem se uma consulta falhar.

### Não funcionais

- RNF01 — Restringir a tela e os endpoints do dashboard a ADMIN e GERENTE, com autorização no Spring Security.
- RNF02 — Consultar documentos da coleção `pedidos` no MongoDB usando o repositório do projeto.
- RNF03 — Aceitar datas ISO `AAAA-MM-DD`, rejeitar início posterior ao fim e limitar cada consulta a 366 dias.
- RNF04 — Retornar valores monetários em decimal com duas casas; não arredondar valores para números inteiros.
- RNF05 — Não retornar dados pessoais de clientes nos indicadores.

## Endpoints

Todos os endpoints exigem usuário autenticado com perfil ADMIN ou GERENTE. Os parâmetros `inicio` e `fim` são opcionais; se ambos forem omitidos, a API consulta os últimos 30 dias, incluindo hoje. Se apenas um for informado, a API completa o período com 30 dias. O limite máximo do intervalo é 366 dias.

### `GET /api/dashboard/resumo`

Parâmetros: `inicio`, `fim` (opcionais; formato `AAAA-MM-DD`).

Exemplo: `GET /api/dashboard/resumo?inicio=2026-11-01&fim=2026-11-10`

Resposta `200`:

```json
{
  "totalVendas": 842.50,
  "quantidadePedidos": 18,
  "ticketMedio": 46.81,
  "pedidosEmAndamento": 6
}
```

`totalVendas` soma o total líquido dos pedidos não cancelados. `pedidosEmAndamento` conta pedidos não cancelados e ainda não finalizados.

### `GET /api/dashboard/vendas`

Parâmetros: `inicio`, `fim` (opcionais); `agrupamento` (opcional: `dia` ou `mes`, padrão `dia`).

Exemplo: `GET /api/dashboard/vendas?inicio=2026-11-01&fim=2026-11-03&agrupamento=dia`

Resposta `200`:

```json
[
  { "periodo": "2026-11-01", "totalVendas": 200.00, "quantidadePedidos": 4 },
  { "periodo": "2026-11-02", "totalVendas": 0.00, "quantidadePedidos": 0 },
  { "periodo": "2026-11-03", "totalVendas": 95.50, "quantidadePedidos": 2 }
]
```

Com `agrupamento=mes`, `periodo` usa o formato `AAAA-MM`.

### `GET /api/dashboard/pedidos-por-status`

Parâmetros: `inicio`, `fim` (opcionais).

Exemplo: `GET /api/dashboard/pedidos-por-status?inicio=2026-11-01&fim=2026-11-10`

Resposta `200`:

```json
[
  { "status": "RECEBIDO", "quantidade": 2 },
  { "status": "EM_PREPARACAO", "quantidade": 3 },
  { "status": "CANCELADO", "quantidade": 1 }
]
```

A resposta contém todos os status existentes no sistema, inclusive os que têm quantidade zero.

### `GET /api/dashboard/produtos-mais-vendidos`

Parâmetros: `inicio`, `fim` (opcionais); `limite` (opcional, de 1 a 20, padrão 5).

Exemplo: `GET /api/dashboard/produtos-mais-vendidos?inicio=2026-11-01&fim=2026-11-10&limite=5`

Resposta `200`:

```json
[
  {
    "produtoId": "produto-123",
    "nomeProduto": "Pizza de Calabresa",
    "quantidadeVendida": 12,
    "valorVendido": 540.00
  }
]
```

O ranking soma a quantidade e o subtotal de cada item em pedidos não cancelados. O subtotal é bruto e não distribui o desconto do pedido pelos produtos.


