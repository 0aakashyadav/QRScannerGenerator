package com.aakash.qrscanner.data

class HistoryRepository(private val dao: HistoryDao) {
    val items = dao.observeAll()
    suspend fun add(kind: HistoryKind, title: String, content: String) {
        dao.insert(HistoryEntity(kind = kind.name, title = title, content = content))
        dao.trimTo(500)
    }
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun clear() = dao.clear()
}
