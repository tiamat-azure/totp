.DEFAULT_GOAL := help
.PHONY: help build start stop restart status logs log test

COMPOSE := docker compose

help: ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

build: ## Construit les images Docker
	$(COMPOSE) build

start: ## Demarre la pile
	$(COMPOSE) up -d
	@echo "Front → http://localhost:8090"

stop: ## Arrete la pile
	$(COMPOSE) down

restart: stop start ## Redemarre la pile

status: ## Etat des conteneurs
	$(COMPOSE) ps

logs: ## Suit les journaux
	$(COMPOSE) logs -f

log: logs ## Alias de logs

test: ## Execute les tests du back-end
	docker run --rm -v "$(CURDIR)/backend:/app" -w /app maven:3.9-eclipse-temurin-21 mvn -B test
