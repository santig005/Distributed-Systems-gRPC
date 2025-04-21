# Sistema Distribuido con Microservicios, gRPC y MOM para Failover

## Integrantes

*   **Jacobo Zuluaga Jaramillo:** `jzuluagaj@eafit.edu.co`
*   **Santiago de Jesús Gómez Alzate:** `sjgomeza@eafit.edu.co`
*   **Victor Jesús Villadiego Álvarez:** `vjvilladia@eafit.edu.co`

## Introducción

Este proyecto, desarrollado en el marco del curso de Tópicos de Telemática, implementa una aplicación distribuida basada en microservicios que simula un sistema simple de gestión de usuarios, productos y pedidos. El objetivo principal es explorar y aplicar conceptos clave de la comunicación entre procesos remotos en entornos distribuidos, abordando desafíos como la eficiencia en la comunicación y la tolerancia a fallos.

La arquitectura sigue un patrón común: un **API Gateway** expone una interfaz RESTful a los clientes, mientras que los **microservicios** internos (Usuarios, Productos, Órdenes) se comunican entre sí, y con el API Gateway de manera eficiente utilizando **gRPC** con Protocol Buffers. Para garantizar la resiliencia del sistema ante fallos temporales de los microservicios, se integra un **Middleware Orientado a Mensajes (MOM)**, específicamente RabbitMQ, que actúa como mecanismo de failover, encolando las solicitudes que no pueden ser procesadas inmediatamente y permitiendo su posterior recuperación y procesamiento.

## Objetivos

*   Diseñar e implementar un API Gateway para la gestión centralizada de peticiones REST.
*   Aplicar gRPC para la comunicación eficiente y tipada entre microservicios.
*   Implementar un mecanismo de failover utilizando RabbitMQ para manejar la indisponibilidad temporal de microservicios.
*   Desarrollar la lógica necesaria en los microservicios para recuperar y procesar mensajes pendientes desde la cola de RabbitMQ.
*   Integrar y probar el funcionamiento completo del sistema distribuido.

## Arquitectura General

El sistema se compone de los siguientes contenedores principales:

1.  **API Gateway (Java - Spring Cloud Gateway):** Punto único de entrada. Recibe peticiones REST del cliente, las valida (parcialmente), interactúa con los microservicios vía gRPC y maneja errores (incluyendo el encolado en RabbitMQ para ciertos fallos).
2.  **User Service (Go):** Gestiona la información de los usuarios (obtener datos, actualizar saldo). Se comunica vía gRPC. Persiste datos en `users.json`.
3.  **Product Service (Node.js - Express/gRPC):** Gestiona la información de los productos (obtener detalles y precio). Se comunica vía gRPC.
4.  **Order Service (Node.js - gRPC):** Gestiona la creación y consulta de órdenes. Orquesta llamadas gRPC a User Service y Product Service. Implementa el consumidor de RabbitMQ para procesar órdenes pendientes. Persiste datos en `orders.json`.
5.  **RabbitMQ (MOM):** Broker de mensajes utilizado para el mecanismo de failover. Almacena temporalmente las solicitudes de creación de órdenes si el Order Service (o sus dependencias) fallan de forma recuperable.

Consulte la [Wiki de Arquitectura](https://github.com/santig005/Distributed-Systems-gRPC/wiki/Arquitectura-del-sistema) para más detalles y diagramas.

## Tecnologías Utilizadas

*   **API Gateway:** Java 17+, Spring Boot 3+, Spring Cloud Gateway
*   **User Service:** Go (última versión estable)
*   **Order Service:** Node.js (LTS)
*   **Product Service:** Node.js (LTS)
*   **Comunicación Inter-Servicios:** gRPC, Protocol Buffers
*   **Middleware de Mensajes (MOM):** RabbitMQ
*   **Contenerización:** Docker, Docker Compose
*   **Build Tools:** Maven (Java), Go Modules (Go), npm (Node.js)
*   **Protocolos:** REST/HTTP, AMQP

## Estructura del Proyecto
├── api-gateway/ # Código fuente del API Gateway (Java/Spring)
├── client/ # Ejemplo básico de cliente (opcional)
├── deployments/ # Archivos de despliegue (docker-compose.yml)
├── libs/ # Definiciones comunes
│ └── protobufs/ # Archivos .proto para gRPC
├── microservices/ # Código fuente de los microservicios
│ ├── order_service/ # Servicio de Órdenes (Node.js)
│ ├── product_service/# Servicio de Productos (Node.js)
│ └── user_service/ # Servicio de Usuarios (Go)
├── mom/ # (Vacío actualmente, podría contener config específica de MOM si crece)
├── tests/ # Scripts para pruebas E2E o de integración (opcional)
├── .gitignore
└── README.md # Este archivo
## Prerrequisitos

Para ejecutar este proyecto, necesitarás tener instaladas las siguientes herramientas:

*   **Docker y Docker Compose:** Únicamente para ejecutar RabbitMQ fácilmente. [Instalar Docker](https://www.docker.com/get-started).
*   **Java JDK 17+:** Para el API Gateway.
*   **Maven 3.8+:** Para construir y ejecutar el API Gateway.
*   **Go (última versión estable):** Para el User Service.
*   **Node.js (versión LTS recomendada):** Para el Order Service y Product Service.
*   **Compilador `protoc`:** Necesario para generar los stubs gRPC si modificas los archivos `.proto`. Los plugins específicos para cada lenguaje también deben estar instalados ([Java gRPC](https://github.com/grpc/grpc-java#quick-start), [Go gRPC](https://grpc.io/docs/languages/go/quickstart/), [Node.js gRPC](https://grpc.io/docs/languages/node/quickstart/)).
*   **Git:** Para clonar el repositorio.

## Configuración Inicial

Antes de ejecutar los servicios, asegúrate de configurar correctamente las URLs y puertos.

1.  **Clonar el repositorio:**
    ```bash
    git clone <URL_DEL_REPOSITORIO>
    cd <NOMBRE_DEL_REPOSITORIO>
    ```

2.  **Configurar URLs de Servicios:** Dado que los servicios correrán localmente en diferentes puertos, la comunicación entre ellos deberá usar `localhost` o `127.0.0.1`.
    *   **API Gateway (`api-gateway/src/main/resources/application.yml`):** Ajusta las secciones `services.*.host` a `localhost` y verifica que los `port` coincidan con los puertos donde *realmente* correrán los microservicios. Asegúrate que `spring.rabbitmq.host` sea `localhost` si vas a correr RabbitMQ con el comando Docker de abajo.
    *   **Order Service (`microservices/order_service/.env`):** Modifica `PRODUCT_SERVICE_URL`, `USER_SERVICE_URL` y `RABBITMQ_URL` para que apunten a `localhost:<puerto_correspondiente>`.
    *   **Product Service (`microservices/product_service/.env`):** Asegúrate que exista y tenga la configuración de puerto correcta.
    *   **User Service (`microservices/user_service/main.go`):** Verifica si las URLs de dependencias (si las tuviera) están configuradas (probablemente no llama a otros servicios).

3.  **Generar Código gRPC (Si es la primera vez o modificaste .proto):**
    *   **API Gateway (Java):**
        ```bash
        cd api-gateway
        mvn clean generate-sources
        cd ..
        ```
    *   **User Service (Go):**
        ```bash
        cd microservices/user_service
        # Asegúrate que la ruta al proto sea correcta desde esta carpeta
        protoc --go_out=. --go-grpc_out=. ../../libs/protobufs/users.proto
        # Descarga/actualiza dependencias si cambiaste el proto
        go mod tidy
        cd ../..
        ```
    *   **(Node.js):** No requiere un paso explícito de generación, `protoLoader` carga el `.proto` en tiempo de ejecución.

## Ejecución Manual de Servicios

Deberás abrir múltiples terminales, una para cada servicio y una para RabbitMQ.

1.  **Iniciar RabbitMQ (Usando Docker):**
    En una terminal, ejecuta:
    ```bash
    docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
    ```
    *   Esto descarga (si no existe) y ejecuta la imagen de RabbitMQ con la UI de administración expuesta en el puerto 15672.
    *   Puedes acceder a la UI en `http://localhost:15672` (user: `guest`, pass: `guest`).

2.  **Iniciar User Service (Go):**
    En una nueva terminal:
    ```bash
    cd microservices/user_service
    go run main.go
    ```
    *   Busca un mensaje indicando que el servidor gRPC está escuchando (probablemente en el puerto 50051).

3.  **Iniciar Product Service (Node.js):**
    En una nueva terminal:
    ```bash
    cd microservices/product_service
    npm install # Solo la primera vez o si cambian las dependencias
    npm start
    ```
    *   Busca un mensaje indicando que el servidor está escuchando (probablemente en el puerto 50053).

4.  **Iniciar Order Service (Node.js):**
    En una nueva terminal:
    ```bash
    cd microservices/order_service
    npm install # Solo la primera vez o si cambian las dependencias
    npm start
    ```
    *   Busca un mensaje indicando que el servidor gRPC está escuchando (probablemente en el puerto 50054) y que se conectó a RabbitMQ.

5.  **Iniciar API Gateway (Java):**
    En una nueva terminal:
    ```bash
    cd api-gateway
    # (Opcional) Limpiar y construir si no lo hiciste antes
    # mvn clean install -DskipTests
    mvn spring-boot:run
    ```
    *   Busca el logo de Spring y mensajes indicando que la aplicación inició y está escuchando (probablemente en el puerto 8080).

6.  **Verificar:**
    *   Prueba los endpoints del API Gateway usando `curl` o Postman (ver sección "Uso / API Endpoints"). Asegúrate de usar `http://localhost:8080` (o el puerto configurado para el Gateway).

7.  **Detener los Servicios:**
    *   Para detener cada servicio Node.js, Go y Java, ve a su terminal respectiva y presiona `Ctrl + C`.
    *   Para detener y eliminar el contenedor de RabbitMQ:
        ```bash
        docker stop rabbitmq
        docker rm rabbitmq
        ```
    ```

## Uso / API Endpoints (Expuestos por el API Gateway)

Asumiendo que el API Gateway corre en `http://localhost:8080` (verifica `application.yml` y `docker-compose.yml`):

*   **Obtener Usuario por ID:**
    ```bash
    curl http://localhost:8080/api/users/{userId}
    ```
    *Ejemplo:* `curl http://localhost:8080/api/users/1`

*   **Obtener Producto por ID:**
    ```bash
    curl http://localhost:8080/api/products/{productId}
    ```
    *Ejemplo:* `curl http://localhost:8080/api/products/1`

*   **Crear una Orden:**
    ```bash
    curl -X POST http://localhost:8080/api/orders \
         -H "Content-Type: application/json" \
         -d '{
               "userId": "1",
               "productIds": ["1", "2"]
             }'
    ```
    *Respuesta Exitosa:* `201 Created` con los detalles de la orden.
    *Respuesta por Failover:* `202 Accepted` si un servicio falló y se encoló.

*   **Obtener Orden por ID:**
    ```bash
    curl http://localhost:8080/api/orders/{orderId}
    ```
    *Ejemplo:* `curl http://localhost:8080/api/orders/1745194637551`

*   **Obtener Órdenes por ID de Usuario:**
    ```bash
    curl http://localhost:8080/api/orders-user/{userId}
    ```
    *Ejemplo:* `curl http://localhost:8080/api/orders-user/1`

Consulta la [Documentación de la API](URL_A_LA_WIKI_API_DOCS) para ver detalles completos de request/response y códigos de error.

## Demostración del Mecanismo de Failover

Puedes simular un fallo para ver el MOM en acción:

## Demostración del Mecanismo de Failover

Puedes simular un fallo para ver el MOM en acción:

1.  Inicia todos los servicios como se describe en "Ejecución Manual".
2.  Detén el **User Service** (presiona `Ctrl + C` en su terminal).
3.  Intenta **crear una orden** usando `curl`. Deberías recibir un `202 Accepted` del API Gateway.
4.  Verifica los **logs del Order Service** (en su terminal). Verás un error al intentar contactar `localhost:50051`.
5.  (Opcional) Revisa la cola `order-service-queue` en la UI de RabbitMQ (`http://localhost:15672`). Debería haber un mensaje pendiente.
6.  **Reinicia el User Service** (ejecuta `go run main.go` de nuevo en su terminal).
7.  Observa los **logs del Order Service**. El consumidor debería recoger el mensaje, procesarlo (ahora User Service responde) y confirmar el mensaje.
8.  **Verifica el estado final** (la orden existe, el saldo se actualizó) usando los endpoints GET.

Consulta la [Wiki: Análisis de Funcionamiento](URL_A_LA_WIKI_ANALISIS) para una descripción más detallada de este proceso.

## Testing

El proyecto incluye (o debería incluir) diferentes niveles de pruebas:

*   **Pruebas Unitarias:** En cada microservicio y el gateway. Ejecutar con `mvn test`, `go test ./...`, `npm test`.
*   **Pruebas de Integración/E2E:** (Potencialmente scripts en `/tests`).

Consulta la [Wiki: Estrategia de Pruebas](URL_A_LA_WIKI_TESTING) para más detalles.

## Documentación Adicional (Wiki)

*   [Requerimientos](URL_A_LA_WIKI_REQUERIMIENTOS)
*   [Arquitectura del Sistema](URL_A_LA_WIKI_ARQUITECTURA)
*   [Documentación de la API REST](URL_A_LA_WIKI_API_DOCS)
*   [Configuración y Despliegue Detallado](URL_A_LA_WIKI_SETUP)
*   [Mecanismo de Failover (MOM)](URL_A_LA_WIKI_FAILOVER)
*   [Estrategia de Pruebas y Análisis](URL_A_LA_WIKI_TESTING)

