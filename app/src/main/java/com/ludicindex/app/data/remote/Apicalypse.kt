package com.ludicindex.app.data.remote

/**
 * Minimal builder for IGDB's Apicalypse query language.
 * See https://api-docs.igdb.com/#reference — the whole query is the POST body as text/plain.
 */
class Apicalypse private constructor() {

    private val clauses = mutableListOf<String>()

    fun fields(vararg names: String) = apply {
        clauses += "fields ${names.joinToString(",")};"
    }

    fun where(clause: String) = apply {
        clauses += "where $clause;"
    }

    fun sort(field: String, descending: Boolean = true) = apply {
        clauses += "sort $field ${if (descending) "desc" else "asc"};"
    }

    fun search(term: String) = apply {
        clauses += "search \"${escape(term)}\";"
    }

    fun limit(count: Int) = apply {
        clauses += "limit $count;"
    }

    fun offset(count: Int) = apply {
        clauses += "offset $count;"
    }

    fun build(): String = clauses.joinToString(" ")

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    companion object {
        fun builder(): Apicalypse = Apicalypse()
    }
}
