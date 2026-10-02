# Micronaut Demo - Order Management API

API REST de gerenciamento de pedidos construída com **Micronaut 4**, compilada como
**GraalVM Native Image** e preparada para execução em **Kubernetes** com escalabilidade
horizontal (HPA), métricas **Prometheus** e segurança **JWT**.

---

## 📋 Sumário

- [Visão Geral](#-visão-geral)
- [Recursos Utilizados](#-recursos-utilizados)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Execução Local (JVM)](#-execução-local-jvm)
- [Build da Imagem Nativa](#-build-da-imagem-nativa)
- [Execução com Docker Compose](#-execução-com-docker-compose)
- [Deploy no Kubernetes (Minikube)](#-deploy-no-kubernetes-minikube)
- [Testando a API](#-testando-a-api)
- [Observabilidade](#-observabilidade)
- [Escalabilidade (HPA)](#-escalabilidade-hpa)
- [Solução de Problemas](#-solução-de-problemas)
- [Referências](#-referências)

---

## 🎯 Visão Geral

O projeto implementa um domínio simples de **pedidos** (`Order` + `OrderItem`), com
operações de CRUD, autenticação JWT e publicação de eventos no Kafka. A aplicação é
compilada como **imagem nativa GraalVM**, o que reduz o tempo de startup para **menos
de 200 ms** e o consumo de memória para **~60 MB por réplica**.

**Endpoints principais:**

| Método   | Caminho                                | Descrição                 | Autenticação |
| -------- | -------------------------------------- | ------------------------- | ------------ |
| `POST`   | `/login`                               | Autenticar e obter JWT    | Público      |
| `POST`   | `/api/v1/orders`                       | Criar pedido              | JWT          |
| `GET`    | `/api/v1/orders`                       | Listar pedidos (paginado) | JWT          |
| `GET`    | `/api/v1/orders/{id}`                  | Buscar pedido por ID      | JWT          |
| `GET`    | `/api/v1/orders/customer/{customerId}` | Pedidos por cliente       | JWT          |
| `GET`    | `/api/v1/orders/customers`             | IDs de clientes distintos | JWT          |
| `GET`    | `/api/v1/orders/status/{status}`       | Pedidos por status        | JWT          |
| `PUT`    | `/api/v1/orders/{id}/status`           | Atualizar status          | JWT          |
| `DELETE` | `/api/v1/orders/{id}`                  | Cancelar pedido           | JWT          |
| `GET`    | `/api/v1/orders/stats`                 | Estatísticas agregadas    | JWT + ADMIN  |
| `GET`    | `/health`                              | Health check (Micronaut)  | Público      |
| `GET`    | `/healthz`                             | Liveness/Readiness probe  | Público      |
| `GET`    | `/prometheus`                          | Métricas Prometheus       | Público      |
| `GET`    | `/swagger-ui/`                         | Swagger UI                | Público      |

---

## 🛠 Recursos Utilizados

### Framework e linguagem

- **Java 17**
- **Micronaut 4.9.5** (platform BOM `4.9.0`)
- **GraalVM Community 25.0.2** (compilação nativa)

### Persistência

- **Micronaut Data JDBC 4.13.1** (sem Hibernate — SQL direto)
- **HikariCP 6.3.0** (pool de conexões)
- **PostgreSQL 16** (produção/dev)
- **H2 2.3.232** (apenas testes, em memória)

### Mensageria

- **Micronaut Kafka 5.9.0**
- **Apache Kafka 3.9.1** + **Zookeeper**

### Segurança

- **Micronaut Security 4.13.0** com **JWT** (HS256)
- Usuários em memória: `user/password` (role `USER`) e `admin/admin` (role `ADMIN`)

### Observabilidade

- **Micronaut Micrometer 5.12.0**
- **Prometheus registry 1.15.0**
- **Logback 1.5.18** (com **AOT** para configuração programática)

### Documentação

- **Micronaut OpenAPI 6.17.1**
- **Swagger UI** (bundled)

### Build e containerização

- **Maven 3.9+** com `mvnw` (wrapper)
- **`native-maven-plugin` 1.1.9** (GraalVM)
- **`micronaut-maven-plugin` 4.8.0** (AOT)
- **Docker multi-stage** (imagem final ~85 MB)
- **Jib** (alternativa para imagem JVM)

### Orquestração

- **Docker Compose** (desenvolvimento local)
- **Kubernetes** (Minikube para desenvolvimento; qualquer cluster em produção)

---

## 🏗 Arquitetura

```
┌─────────────────────────────┐
│     Cliente (HTTP)          │
│   curl  /  navegador        │
└──────────────┬──────────────┘
│  HTTP :8080
▼
┌─────────────────────────────────────────────────────┐
│              Micronaut Native Image                 │
│                                                     │
│   ┌────────────┐   ┌──────────┐   ┌─────────────┐   │
│   │ Controller │──▶│ Service  │──▶│ Repository  │   │
│   │ REST + JWT │   │ domínio  │   │ JDBC+Hikari │   │
│   └────────────┘   └────┬─────┘   └──────┬──────┘   │
│                         │                │          │
│                         │                ▼          │
│                         │        ┌───────────────┐  │
│                         │        │ PostgreSQL 16 │  │
│                         │        └───────────────┘  │
│                         ▼                           │
│                  ┌──────────────┐                   │
│                  │Kafka Producer│───▶ Kafka + ZK    │
│                  └──────────────┘                   │
└─────────────────────────────────────────────────────┘
▲                                  ▲
│ scrape /prometheus               │ probe /healthz
│                                  │
┌────────────┐                    ┌────────────┐
│ Prometheus │                    │  Kubelet   │
└────────────┘                    └────────────┘

```
---

## ✅ Pré-requisitos

| Ferramenta     | Versão mínima | Observação               |
| -------------- | ------------- | ------------------------ |
| JDK            | 17            | para compilar            |
| Docker Engine  | 24+           | cliente e daemon         |
| Docker Compose | 2.20+         | plugin `docker compose`  |
| Minikube       | 1.32+         | para deploy local em K8s |
| kubectl        | 1.28+         | cliente K8s              |
| Maven          | 3.9+          | ou usar `./mvnw`         |
| `hey` ou `ab`  | —             | para testes de carga     |

> **Nota:** o projeto usa o **Docker Engine nativo**, não o Docker Desktop.
> Se você tem o Docker Desktop instalado, verifique com `docker context ls`
> qual contexto está ativo (`default` ou `desktop-linux`).

---

## 🖥 Execução Local (JVM)

### 1. Subir a infraestrutura (Postgres + Kafka)

```
docker compose up -d
```

Verifique se os containers estão saudáveis:

```
docker compose ps
```

### 2. Compilar e executar a aplicação

```
# Compilar (modo JVM, com AOT habilitado)
./mvnw clean package

# Executar
java -jar target/micronaut-demo.jar
```

A aplicação ficará disponível em http://localhost:8080.

### 3. Acessar o Swagger UI

http://localhost:8080/swagger-ui/

## 🚀 Build da Imagem Nativa

```
docker build --no-cache -t micronaut-demo:native .

# Confirmar o tamanho
docker images | grep micronaut-demo
```

Para tags únicas por build (recomendado):

```
TAG=$(date +%Y%m%d-%H%M%S)
docker build -t micronaut-demo:$TAG .
```

## 🐳 Execução com Docker Compose

### 1. Subir Postgres + Kafka

```
docker compose up -d
```

###2. Executar a aplicação nativa

```
TAG=$(date +%Y%m%d-%H%M%S)
docker build -t micronaut-demo:$TAG .

docker run --network host \
  -e JDBC_URL="jdbc:postgresql://localhost:5432/orderdb" \
  -e JDBC_USER=postgres \
  -e JDBC_PASSWORD=password \
  -e JDBC_DRIVER=org.postgresql.Driver \
  -e JDBC_DIALECT=POSTGRES \
  -e JDBC_SCHEMA_GENERATE=CREATE_DROP \
  -e KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  micronaut-demo:$TAG
```

**Atenção:** JDBC_SCHEMA_GENERATE=CREATE_DROP recria o schema a cada start.
Use apenas em desenvolvimento. Em produção, use NONE e migrações (Flyway/Liquibase).

## ☸ Deploy no Kubernetes (Minikube)

### 1. Iniciar o cluster

```
minikube start --driver=docker --cpus=4 --memory=8192
```

### 2. Construir a imagem e carregá-la no Minikube

Opção — Construir no host e carregar:

```
TAG=$(date +%Y%m%d-%H%M%S)

# Build no host
docker build -t micronaut-demo:$TAG .

# Carregar no Minikube
minikube image load micronaut-demo:$TAG

# Verificar
minikube image ls | grep micronaut-demo
```

### 3. Atualizar o deployment.yml

Edite k8s/deployment.yml e ajuste a imagem:

```
image: micronaut-demo:$TAG
imagePullPolicy: Never
```

Substitua $TAG pelo valor gerado (ex.: 20261002-085934).

### 4. Aplicar os manifestos

```
# Infraestrutura
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/kafka.yaml

# Aplicação
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/rbac.yaml
kubectl apply -f k8s/deployment.yml
```

### 5. Verificar os Pods

```
kubectl get pods -w
```

Você verá 2 Pods micronaut-demo-\*, 1 Postgres, 1 Kafka e 1 Zookeeper.

### 6. Expor a aplicação

```
kubectl port-forward service/micronaut-demo 8080:80
```

A aplicação estará em http://localhost:8080.

### 7. Script de automação (opcional)

Encontra-se em **./build-and-deploy.sh**

## 🧪 Testando a API

### 1. Health check (público)

```
curl -i http://localhost:8080/healthz
```

### 2. Login para obter o JWT

```
TOKEN=$(curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}' | jq -r '.access_token')

echo $TOKEN
```

### 3. Endpoints protegidos

```
# Listar pedidos (paginado)
curl -s "http://localhost:8080/api/v1/orders?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq

# Pedidos por cliente
curl -s http://localhost:8080/api/v1/orders/customer/carlos \
  -H "Authorization: Bearer $TOKEN" | jq

# Clientes distintos
curl -s http://localhost:8080/api/v1/orders/customers \
  -H "Authorization: Bearer $TOKEN" | jq

# Criar pedido
curl -s -X POST "http://localhost:8080/api/v1/orders?customerId=cust-42" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[{"productId":"p1","productName":"Teclado","quantity":1,"unitPrice":250.00}]' | jq

# Atualizar status
curl -s -X PUT "http://localhost:8080/api/v1/orders/1/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"PROCESSING"}' | jq

# Cancelar pedido
curl -s -X DELETE http://localhost:8080/api/v1/orders/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Endpoint ADMIN (requer role ADMIN)

```
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq -r '.access_token')

curl -s http://localhost:8080/api/v1/orders/stats \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

### 5. Sem autenticação (deve retornar 401)

```
curl -i http://localhost:8080/api/v1/orders
```

## 📊 Observabilidade

### Prometheus

```
curl -s http://localhost:8080/prometheus | head -30
```

**Métricas expostas:**

executor\_\* — pool de threads

jvm\_\* — memória, GC (quando disponível)

hikaricp\_\* — pool de conexões

http*server_requests*\* — requisições HTTP

kafka\_\* — métricas de producer/consumer

### Swagger UI

Acesse http://localhost:8080/swagger-ui/ no navegador. Clique em Authorize
e cole o token JWT (sem o prefixo Bearer) para testar endpoints autenticados.

### Health / Info

```
curl -s http://localhost:8080/health | jq
curl -s http://localhost:8080/info | jq
```

## 📈 Escalabilidade (HPA)

O projeto inclui um HorizontalPodAutoscaler em k8s/hpa.yaml.

### 1. Habilitar o metrics-server no Minikube

```
minikube addons enable metrics-server
```

Aguarde 1–2 minutos e valide:

```
kubectl top nodes
kubectl top pods
```

### 2. Aplicar o HPA

```
kubectl apply -f k8s/hpa.yaml
kubectl get hpa
```

Saída esperada:

```
Aguarde 1–2 minutos e valide:
```

### 3. Gerar carga

```
# Token
TOKEN=$(curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}' | jq -r '.access_token')

# Carga: 100 concorrentes por 5 minutos
hey -z 5m -c 100 \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/orders
```

### 4. Observar o scaling

```
watch -n 2 'kubectl get hpa && echo && kubectl get pods -l app=micronaut-demo'
```

O número de réplicas subirá de 2 para até 10, e depois voltará
gradualmente após o fim da carga.

## 📚 Referências

Micronaut Documentation

Micronaut Data JDBC

GraalVM Native Image

Micronaut AOT

Kubernetes HPA

GraalVM Reachability Metadata Repository

## 📝 Licença

Este projeto é um exemplo educacional e está disponível sob a licença MIT.
Sinta-se à vontade para usá-lo como referência para seus próprios projetos.
