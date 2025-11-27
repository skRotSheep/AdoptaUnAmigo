package com.example.adoptaunamigo.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.adoptaunamigo.data.MascotaContract.MascotaEntry

private const val SQL_CREATE_ENTRIES = """
    CREATE TABLE ${MascotaEntry.TABLE_NAME} (
        ${MascotaEntry.COLUMN_NAME_ID} INTEGER PRIMARY KEY,
        ${MascotaEntry.COLUMN_NAME_NOMBRE} TEXT,
        ${MascotaEntry.COLUMN_NAME_RAZA} TEXT,
        ${MascotaEntry.COLUMN_NAME_EDAD} TEXT,
        ${MascotaEntry.COLUMN_NAME_ESPECIE} TEXT,
        ${MascotaEntry.COLUMN_NAME_FOTO_URI} TEXT,
        ${MascotaEntry.COLUMN_NAME_DESCRIPCION} TEXT)
"""

private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${MascotaEntry.TABLE_NAME}"

class MascotaDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE ${MascotaEntry.TABLE_NAME} ADD COLUMN ${MascotaEntry.COLUMN_NAME_DESCRIPCION} TEXT")
        }
    }

    companion object {
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "AdoptaUnAmigo.db"
    }
}
