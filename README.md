# Sistema Distribuido con Microservicios, gRPC y MOM para Failover

## Integrantes

*   **Jacobo Zuluaga Jaramillo:** `email2@example.com`
*   **Santiago de Jesús Gómez Alzate:** `sjgomeza@eafit.edu.co`
*   **Victor Jesús Villadiego Álvarez:** `email2@example.com`

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


## Instalación y Ejecución (Usando Docker Compose)

Esta es la forma recomendada para levantar todo el sistema fácilmente.

1.  **Clonar el repositorio:**
    ```bash
    git clone <URL_DEL_REPOSITORIO>
    cd <NOMBRE_DEL_REPOSITORIO>
    ```

2.  **Construir e Iniciar los Contenedores:**
    Desde la raíz del proyecto, ejecuta:
    ```bash
    docker-compose -f deployments/docker-compose.yml up --build
    ```
    *   `--build` fuerza la reconstrucción de las imágenes si el código ha cambiado. La primera vez puede tardar varios minutos.
    *   `-f deployments/docker-compose.yml` especifica la ubicación del archivo compose.

3.  **Verificar:**
    *   Observa los logs en la terminal. Deberías ver mensajes indicando que cada servicio (API Gateway, User, Product, Order, RabbitMQ) se ha iniciado correctamente y está escuchando en sus respectivos puertos.
    *   Puedes acceder a la UI de RabbitMQ (si se expuso en `docker-compose.yml`, usualmente en `http://localhost:15672` con credenciales `guest`/`guest`).
    *   Prueba los endpoints del API Gateway (ver sección "Uso / API Endpoints").

4.  **Detener los Contenedores:**
    Presiona `Ctrl + C` en la terminal donde ejecutaste `docker-compose up`. Para asegurarte de que los contenedores y redes se eliminan:
    ```bash
    docker-compose -f deployments/docker-compose.yml down
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

1.  Inicia todo con `docker-compose -f deployments/docker-compose.yml up`.
2.  Detén el servicio de usuarios: `docker-compose -f deployments/docker-compose.yml stop user_service`.
3.  Intenta crear una orden usando el comando `curl` de arriba. Deberías recibir un `202 Accepted` del API Gateway.
4.  Verifica los logs del `order_service`: `docker-compose -f deployments/docker-compose.yml logs order_service`. Verás un error al intentar contactar a `user_service`.
5.  (Opcional) Revisa la cola `order-service-queue` en la UI de RabbitMQ (`http://localhost:15672`). Debería haber un mensaje pendiente.
6.  Inicia el servicio de usuarios nuevamente: `docker-compose -f deployments/docker-compose.yml start user_service`.
7.  Observa los logs del `order_service` de nuevo. El consumidor debería recoger el mensaje de la cola, procesarlo exitosamente (ahora que `user_service` está disponible) y confirmar (`ack`) el mensaje.
8.  Verifica que la orden se creó y el saldo del usuario se actualizó usando los endpoints GET correspondientes.

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

