# Makefile for managing Redis and the application

REDIS_IMAGE_NAME = es-processors-sagas-redis-server
REDIS_CONTAINER_NAME = redis-recovery
REDIS_PORT = 6379
REDIS_USER = es-processors-sagas
REDIS_PASSWORD = es-processors-sagas

.PHONY: redis-build redis-start redis-stop redis-status docker-build docker-run docker-stop docker-status

redis-stop:
	docker stop $(REDIS_CONTAINER_NAME) || true
	docker rm $(REDIS_CONTAINER_NAME) || true

redis-status:
	docker ps -f name=$(REDIS_CONTAINER_NAME)

redis-build:
	docker build -t $(REDIS_IMAGE_NAME) .

redis-start:
	docker run --name $(REDIS_CONTAINER_NAME) -p $(REDIS_PORT):6379 -d $(REDIS_IMAGE_NAME) redis-server --user $(REDIS_USER) on ">"$(REDIS_PASSWORD) "~*" "+@all"
