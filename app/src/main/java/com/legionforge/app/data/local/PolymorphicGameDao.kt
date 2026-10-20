package com.legionforge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.legionforge.app.data.model.BuilderEntryEntity
import com.legionforge.app.data.model.BuilderListEntity
import com.legionforge.app.data.model.CatalogCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PolymorphicGameDao {
    @Query("SELECT * FROM catalog_cards WHERE gameSystem = :system AND (factionId = :factionId OR factionId = 'neutral' OR (factionId = 'mercenary' AND (',' || mercenaryFactions || ',') LIKE '%,' || :factionId || ',%')) ORDER BY name")
    fun observeCards(system: String, factionId: String): Flow<List<CatalogCardEntity>>

    @Query("SELECT * FROM catalog_cards WHERE gameSystem = :system ORDER BY factionId, name")
    fun observeCards(system: String): Flow<List<CatalogCardEntity>>

    /** Tout le catalogue, tous systêmes, pour l'écran Ma collection (Réglages). */
    @Query("SELECT * FROM catalog_cards ORDER BY CASE gameSystem WHEN 'LEGION_V2' THEN 0 ELSE 1 END, factionId, kind, name")
    fun observeAllCards(): Flow<List<CatalogCardEntity>>

    /** Recherche globale plein texte sur name + rulesText + legionStats + shipStats.
     *  Face aux règles : cherche le terme dans le texte des cartes, des stats, des mots-clés. */
    @Query("""SELECT * FROM catalog_cards
              WHERE name LIKE '%' || :q || '%'
                 OR rulesText LIKE '%' || :q || '%'
                 OR legionStats LIKE '%' || :q || '%'
                 OR shipStats LIKE '%' || :q || '%'
              ORDER BY CASE gameSystem WHEN 'LEGION_V2' THEN 0 ELSE 1 END, factionId, name
              LIMIT 100""")
    fun searchCards(q: String): Flow<List<CatalogCardEntity>>

    @Query("SELECT COUNT(*) FROM catalog_cards")
    suspend fun cardCount(): Int

    @Query("SELECT gameSystem, COUNT(*) AS cnt FROM catalog_cards GROUP BY gameSystem")
    suspend fun cardCountBySystem(): List<SystemCountRow>

    /** Nombre de vaisseaux Armada au catalogue. Le catalogue complet en bundle en contient 64.
     *  Une base migrée de l'ancienne version (46 vaisseaux, pas de Republic/Sep) doit être re-seedée. */
    @Query("SELECT COUNT(*) FROM catalog_cards WHERE gameSystem = 'ARMADA_V15' AND kind = 'ARMADA_SHIP'")
    suspend fun armadaShipCount(): Int

    /** Nombre de vaisseaux/escadrons Armada qui n'ont pas encore de stats (shipStats NULL/'').
     *  > 0 => il faut re-seeder pour charger les stats du mode partie. */
    @Query("SELECT COUNT(*) FROM catalog_cards WHERE gameSystem = 'ARMADA_V15' AND kind IN ('ARMADA_SHIP','ARMADA_SQUADRON') AND (shipStats IS NULL OR shipStats = '')")
        suspend fun armadaUnitsWithoutStats(): Int

        /** Nombre de cartes Armada sans noms localisés (names NULL/''). > 0 => re-seed pour charger les noms FR/DE/ES. */
        @Query("SELECT COUNT(*) FROM catalog_cards WHERE gameSystem = 'ARMADA_V15' AND (names IS NULL OR names = '')")
        suspend fun armadaCardsMissingNames(): Int

    data class SystemCountRow(val gameSystem: String, val cnt: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCards(cards: List<CatalogCardEntity>)

    /** Comme upsertCards mais retourne le nombre de lignes réellement écrites/affectées.
     *  @Insert Room renvoie les rowid (List<Long>); on les compte pour vérifier que l'écriture a réellement eu lieu. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCardsCounted(cards: List<CatalogCardEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: BuilderListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<BuilderEntryEntity>)

    @Query("DELETE FROM builder_entries WHERE listId = :listId")
    suspend fun deleteEntries(listId: String)

    @Query("SELECT * FROM builder_lists ORDER BY updatedAt DESC")
    fun observeLists(): Flow<List<BuilderListEntity>>

    @Query("SELECT * FROM builder_lists WHERE id = :listId LIMIT 1")
    suspend fun getList(listId: String): BuilderListEntity?

    @Query("SELECT * FROM builder_entries WHERE listId = :listId ORDER BY rowid")
    fun observeEntries(listId: String): Flow<List<BuilderEntryEntity>>

    @Transaction
    suspend fun replaceEntries(listId: String, entries: List<BuilderEntryEntity>) {
        deleteEntries(listId)
        if (entries.isNotEmpty()) upsertEntries(entries)
    }
}
