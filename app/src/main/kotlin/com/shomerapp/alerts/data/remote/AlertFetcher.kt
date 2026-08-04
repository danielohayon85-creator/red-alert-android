package com.shomerapp.alerts.data.remote

/** Abstraction over "get the current raw response body from wherever alerts come from" — lets
 *  the polling repository run against the real oref endpoint or a mock source interchangeably. */
interface AlertFetcher {
    /** Raw response body, BOM-stripped, or null on network/HTTP failure. Blank string means "no alert". */
    suspend fun fetchRaw(): String?
}
