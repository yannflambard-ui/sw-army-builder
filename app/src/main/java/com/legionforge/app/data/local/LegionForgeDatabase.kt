package com.legionforge.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.legionforge.app.data.model.ArmyList
import com.legionforge.app.data.model.ArmyUnit
import com.legionforge.app.data.model.Faction
import com.legionforge.app.data.model.Keyword
import com.legionforge.app.data.model.OwnedUnit
import com.legionforge.app.data.model.UnitEntity
import com.legionforge.app.data.model.UnitKeywordCrossRef
import com.legionforge.app.data.model.UpgradeSlot
import com.legionforge.app.data.model.CatalogCardEntity
import com.legionforge.app.data.model.BuilderListEntity
import com.legionforge.app.data.model.BuilderEntryEntity
import com.legionforge.app.data.model.WikiSectionEntity
import com.legionforge.app.data.local.PolymorphicGameDao
import com.legionforge.app.data.local.WikiDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Faction::class,
        UnitEntity::class,
        UpgradeSlot::class,
        Keyword::class,
        UnitKeywordCrossRef::class,
        ArmyList::class,
        ArmyUnit::class,
        OwnedUnit::class,
        CatalogCardEntity::class,
        BuilderListEntity::class,
        BuilderEntryEntity::class,
        WikiSectionEntity::class
    ],
    version = 28,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LegionForgeDatabase : RoomDatabase() {
    abstract fun gameDataDao(): GameDataDao
    abstract fun armyListDao(): ArmyListDao
    abstract fun polymorphicGameDao(): PolymorphicGameDao
    abstract fun wikiDao(): WikiDao

    companion object {
        @Volatile
        private var INSTANCE: LegionForgeDatabase? = null

        fun getInstance(context: Context): LegionForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LegionForgeDatabase::class.java,
                    "legionforge.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_26_27, MIGRATION_27_28).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_cards ADD COLUMN names TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_cards ADD COLUMN shipStats TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_cards ADD COLUMN linkedUnit TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_cards ADD COLUMN legionStats TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS wiki_sections (id TEXT NOT NULL PRIMARY KEY, gameSystem TEXT NOT NULL, title TEXT NOT NULL, keywords TEXT NOT NULL, content TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_wiki_sections_gameSystem ON wiki_sections(gameSystem)")
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Purge wiki_sections : le seed (WikiRepository.seedWiki) se relance
                // car sectionCount()==0 et re-importe le nouveau wiki Legion (368 sections,
                // découpé proprement depuis le Core Rulebook officiel 2026).
                db.execSQL("DELETE FROM wiki_sections")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE builder_entries ADD COLUMN chosenSlot TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Vide catalog_cards pour forcer le re-seed (seedCatalog re-importe catalog.json
                // avec les nouvelles données, ex: mercenaryFactions des unités Mandaloriennes).
                // Préserve les listes d'armée et la collection (autres tables intactes).
                db.execSQL("DELETE FROM catalog_cards")
            }
        }
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Vide catalog_cards : force le re-seed (suppression Anakin 18 pts obsolète).
                db.execSQL("DELETE FROM catalog_cards")
            }
        }
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS catalog_cards (id TEXT NOT NULL PRIMARY KEY, gameSystem TEXT NOT NULL, kind TEXT NOT NULL, name TEXT NOT NULL, points INTEGER NOT NULL, factionId TEXT NOT NULL, legionRank TEXT, upgradeSlots TEXT NOT NULL, allowedUpgradeSlots TEXT NOT NULL, commander INTEGER NOT NULL, `unique` INTEGER NOT NULL, imageUrl TEXT, imageAssetPath TEXT, imageAssetPaths TEXT, rulesText TEXT, names TEXT DEFAULT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_cards_gameSystem ON catalog_cards(gameSystem)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_cards_factionId ON catalog_cards(factionId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS builder_lists (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, gameSystem TEXT NOT NULL, factionId TEXT NOT NULL, pointsLimit INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS builder_entries (instanceId TEXT NOT NULL PRIMARY KEY, listId TEXT NOT NULL, cardId TEXT NOT NULL, parentInstanceId TEXT, quantity INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_builder_entries_listId ON builder_entries(listId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_builder_entries_cardId ON builder_entries(cardId)")
            }
        }
    }
}
