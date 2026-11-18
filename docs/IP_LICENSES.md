# Licences et provenance des données

## Données de jeu (catalogues)

Les listes d'unités, coûts, mots-clés et règles incluses dans l'application
proviennent des publications officielles Star Wars Legion v2 et Star Wars Armada 1.5
(Atomic Mass Games / Lucasfilm).

Le propriétaire du projet a confirmé le 18 novembre 2026 disposer des autorisations
nécessaires à la redistribution de ces données pour la publication de l'application.
Les justificatifs formels doivent être conservés par le propriétaire et fournis au
Play Store si celui-ci les demande. Aucun numéro de licence n'est consigné ici :
il n'a pas été communiqué et ne doit pas être inventé.

## Tokens tabletop Legion

Les 177 tokens ronds du plateau (`app/src/main/assets/legion_tokens/`) sont
générés par `scripts/make_legion_tokens.py` à partir des illustrations des cartes
déjà embarquées dans l'application. Aucun token tiers (Devin Night, dumps Reddit,
etc.) n'est redistribué : ces sources ont été écartées pour raisons de licence.

## Illustrations d'interface

Icônes Material Design (Apache 2.0) via androidx.compose.material:material-icons-core.

## À vérifier avant publication

- Conserver les justificatifs d'autorisation AMG/Lucasfilm.
- Déclarer l'application comme non officielle dans le listing (déjà fait dans
  docs/store-listing.md).
- Renseigner un contact éditeur valide dans la fiche Play Console.
