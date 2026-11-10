# Pediu Patrão

Sistema de pedidos para lanchonete/pizzaria desenvolvido com Java 21, Spring Boot, Spring Security, Thymeleaf e MongoDB.

## Executar localmente

1. Inicie o MongoDB em `mongodb://localhost:27017/pediupatrao` ou defina a variável `MONGODB_URI`.
2. Execute na raiz do projeto:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. Acesse `http://localhost:8080/login`. O banco precisa estar disponível para a aplicação iniciar.

## Dashboard administrativo

Entre com um usuário ADMIN ou GERENTE e acesse `http://localhost:8080/dashboard`. A página consome os endpoints `/api/dashboard/**` e filtra os indicadores por data.


## Testes

Execute os testes unitários do serviço de dashboard com:

```powershell
.\mvnw.cmd test
```
