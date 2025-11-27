package com.example.adoptaunamigo.data

import android.provider.BaseColumns

object MascotaContract {
    object MascotaEntry : BaseColumns {
        const val TABLE_NAME = "mascotas"
        const val COLUMN_NAME_ID = BaseColumns._ID
        const val COLUMN_NAME_NOMBRE = "nombre"
        const val COLUMN_NAME_RAZA = "raza"
        const val COLUMN_NAME_EDAD = "edad"
        const val COLUMN_NAME_ESPECIE = "especie"
        const val COLUMN_NAME_FOTO_URI = "foto_uri"
        const val COLUMN_NAME_DESCRIPCION = "descripcion"
    }
}
