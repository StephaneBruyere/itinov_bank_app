# bankApp

Application de démonstration d’une banque, bâtie sur Spring Boot (Java 21), exposant des API REST pour gérer clients, comptes et transactions. Le projet illustre une architecture simple avec séparation nette entre couche web, services, accès aux données et modèle de domaine. La sécurité s’appuie sur OpenID Connect (Keycloak) et l’application publie une documentation OpenAPI.
Note: Ce projet est un POC (Proof of Concept) destiné aux démonstrations et essais; il utilise une base de données H2 en mémoire par défaut.

## Sommaire
- Fonctionnalités
- Architecture
- Sécurité et authentification
- Démarrage rapide
- Configuration
- Documentation API
- Jeux de données et initialisation
- Tests
- Outils pour développeurs

## Fonctionnalités
- Gestion des clients (CRUD)
- Gestion des comptes 
- Historique et opérations de transactions (crédit, débit, transfert)
- Sécurisation par OAuth2/OIDC avec JWT
- Documentation OpenAPI/Swagger
- Collection Postman fournie pour les appels d’API
- Jeux de tests unitaires et d’intégration

## Architecture

Couche par couche:
- Web (Controllers)
    - Expose les endpoints REST, gère la validation et les codes d’erreur, centralise la gestion des exceptions.
- Service
    - Contient la logique métier (transactions, validations, règles).
- Accès aux données (Repositories)
    - Requêtes et persistance via Spring Data JPA.
- Domaine (Model)
    - Entités métier: Client, Compte, Transaction et énumérations associées (p. ex. devise, type d’opération).
- DTO et Mappers
    - Objets de transfert (DTO) et mapping entre entités/DTO (mapper dédié).
- Configuration
    - Sécurité (OAuth2/JWT), OpenAPI/Swagger, initialisation de données au démarrage.

Points notables:
- Gestion des erreurs centralisée via un advice global.
- Séparation stricte entités/DTO pour maîtriser les contrats d’API.
- Services orientés cas d’usage, testables et découplés des contrôleurs.

## Sécurité et authentification

- Fournisseur d’identité: Keycloak (OpenID Connect).
- Flux typique:
    1) Un jeton JWT est obtenu pour un utilisateur (via Keycloak).
    2) Les appels API sont effectués avec Authorization: Bearer <token>.
- Un contrôleur d’authentification fournit un endpoint dédié au jeton pour les besoins locaux. En production, privilégier les flux standard Keycloak/OIDC.

Astuce:
- Un fichier d’import Keycloak est fourni pour créer realm, clients et rôles nécessaires.

## Démarrage rapide

Prérequis:
- Java 21
- Maven 3.9+
- Docker et Docker Compose (pour keycloak)

Étapes:
1) Construire
    - mvn clean install
2) Lancer l’infrastructure Keycloak (via Docker Compose)
    - docker compose up -d
3) Démarrer l’application
    - mvn spring-boot:run
4) Accéder à la documentation API (voir section “Documentation API”)

Arrêt:
- docker compose down pour arrêter l’infrastructure démarrée par Docker Compose (Keycloak).

## Configuration

Variables et propriétés courantes (adapter selon votre environnement):
- Profil Spring: SPRING_PROFILES_ACTIVE
- Base de données: URL/JDBC, utilisateur, mot de passe
- Sécurité/OIDC:
    - Keycloak
- Base de données par défaut: H2 en mémoire (idéale pour le POC et le développement local). Vous pouvez la remplacer par un SGBD persistant en ajustant les propriétés.

Les propriétés se définissent via:
- application.yml
- Variables d’environnement
- Lignes de commande Maven (-D...)

## Documentation API

- OpenAPI/Swagger UI:
    - http://localhost:8080/swagger-ui.html
    - ou http://localhost:8080/swagger-ui/index.html
- Spécification OpenAPI (JSON/YAML):
    - http://localhost:8080/v3/api-docs
    - http://localhost:8080/v3/api-docs.yaml

Les endpoints principaux couvrent la gestion des clients, comptes et transactions. Les schémas de requête/réponse, codes d’erreurs et exemples sont consultables dans Swagger UI.

## Jeux de données et initialisation

- Un initialiseur de données précharge des entités de démonstration au démarrage.
- Un fichier d’import Keycloak est disponible pour provisionner realm/clients/roles (à utiliser lors du démarrage du conteneur Keycloak).

## Tests

- Tests unitaires pour la logique de mapping, services et repository.
- Tests d’intégration pour les contrôleurs (scénarios end-to-end avec contexte Spring).
- Lancer la suite:
    - mvn test

## Outils pour développeurs

- Postman: un fichier de collection est fourni pour exécuter et documenter les requêtes les plus fréquentes (authentification, clients, comptes, transactions).
- OpenAPI: générer des clients ou vérifier rapidement les contrats d’API via Swagger UI.


