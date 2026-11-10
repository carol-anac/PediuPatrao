# Pediu Patrão

Sistema web de pedidos para lanchonete e pizzaria. O projeto usa Java, Spring Boot, Spring Security, Thymeleaf e MongoDB.

## Sobre as branches

A branch `main` contém a versão-base do sistema. A implementação da atividade **Dashboard Administrativo** está na branch [`feature/dashboard-administrativo`](https://github.com/carol-anac/PediuPatrao/tree/feature/dashboard-administrativo), com os endpoints, a tela, as regras de pedidos e a documentação da atividade.

Para abrir a versão do dashboard depois de clonar o repositório:

```powershell
git fetch origin
git switch --track origin/feature/dashboard-administrativo
```

## Funcionalidades

Na versão-base (`main`):

- Login e páginas administrativas com Thymeleaf.
- Cadastro e consulta de clientes e produtos.
- Registro e consulta de pedidos.
- API REST para pedidos, clientes, produtos e usuários.

Na branch do dashboard:

- Indicadores de vendas, pedidos considerados, ticket médio e pedidos em andamento.
- Gráfico de vendas por dia ou mês e gráfico de pedidos por situação.
- Ranking de produtos mais vendidos.
- Filtro por período e atualização automática do dashboard.
- Avanço de status conforme o fluxo do pedido; desconto limitado a 20% e cancelamento com justificativa, restritos ao GERENTE.

## Tecnologias

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring Data MongoDB
- Thymeleaf
- Bootstrap e Chart.js

## Requisitos para executar

- JDK 21
- MongoDB local ou uma instância MongoDB acessível pela aplicação

## Executar a branch do dashboard no Windows

1. Inicie o MongoDB. Por padrão, a aplicação usa `mongodb://localhost:27017/pediupatrao`.
2. Se necessário, configure outra URI no PowerShell:

   ```powershell
   $env:MONGODB_URI = "mongodb://localhost:27017/pediupatrao"
   ```

3. Na raiz do projeto, inicie a aplicação:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Acesse `http://localhost:8080/login`.

Para abrir o dashboard, entre com um usuário ADMIN ou GERENTE e acesse `http://localhost:8080/dashboard`. Se o banco estiver vazio, cadastre clientes e produtos e registre pedidos para alimentar os gráficos.

### Usuários de demonstração

Na primeira execução em um banco sem esses usuários, a branch do dashboard cria:

| Usuário | Perfil | Senha padrão de desenvolvimento |
| --- | --- | --- |
| `admin` | ADMIN | `teste123` |
| `gerente` | GERENTE | `gerente123` |
| `atendente` | ATENDENTE | `atendente123` |

As senhas podem ser definidas pelas variáveis `SEED_ADMIN_PASSWORD`, `SEED_GERENTE_PASSWORD` e `SEED_ATENDENTE_PASSWORD` antes da primeira execução. As senhas padrão são para demonstração local.

## API do dashboard

Os endpoints abaixo estão na branch `feature/dashboard-administrativo` e exigem autenticação com perfil ADMIN ou GERENTE. `inicio` e `fim` são datas no formato `AAAA-MM-DD`.

| Endpoint | Retorno |
| --- | --- |
| `GET /api/dashboard/resumo` | Total de vendas, pedidos considerados, ticket médio e pedidos em andamento |
| `GET /api/dashboard/vendas?agrupamento=dia` | Série de vendas por dia; também aceita `agrupamento=mes` |
| `GET /api/dashboard/pedidos-por-status` | Quantidade de pedidos por situação |
| `GET /api/dashboard/produtos-mais-vendidos?limite=5` | Ranking por quantidade vendida |

Os endpoints aceitam `inicio` e `fim`; sem datas, usam os últimos 30 dias. A tela inicia com os últimos sete dias e atualiza os indicadores automaticamente a cada 60 segundos. Pedidos cancelados não entram no faturamento nem no ranking, mas aparecem na distribuição por status.

O detalhamento dos requisitos e exemplos JSON está em [`docs/dashboard-administrativo.md`](https://github.com/carol-anac/PediuPatrao/blob/feature/dashboard-administrativo/docs/dashboard-administrativo.md).

## Testes

Na branch do dashboard, execute os testes unitários com:

```powershell
.\mvnw.cmd test
```

Os testes do serviço de dashboard ficam em `src/test/java/com/umc/pediupatrao/service/DashboardServiceTest.java`.
