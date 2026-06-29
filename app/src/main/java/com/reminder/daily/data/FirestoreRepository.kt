package com.reminder.daily.data

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val listId: String) {

    private val todosRef get() = Firebase.firestore
        .collection("lists").document(listId).collection("todos")

    fun getTodos(): Flow<List<Todo>> = callbackFlow {
        val listener = todosRef
            .orderBy("isCompleted", Query.Direction.ASCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val todos = snapshot.documents.mapNotNull { doc ->
                    Todo(
                        id = doc.id,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        addedBy = doc.getString("addedBy") ?: ""
                    )
                }
                trySend(todos)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTodo(title: String, addedByName: String) {
        todosRef.add(
            mapOf(
                "title" to title,
                "isCompleted" to false,
                "createdAt" to System.currentTimeMillis(),
                "addedBy" to addedByName
            )
        ).await()
    }

    suspend fun updateTodo(todo: Todo) {
        todosRef.document(todo.id).update("isCompleted", todo.isCompleted).await()
    }

    suspend fun deleteTodo(todo: Todo) {
        todosRef.document(todo.id).delete().await()
    }

    suspend fun getPendingCount(): Int =
        todosRef.whereEqualTo("isCompleted", false).get().await().size()

    companion object {
        suspend fun createList(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val code = (1..6).map { chars.random() }.joinToString("")
            Firebase.firestore.collection("lists").document(code)
                .set(mapOf("createdAt" to System.currentTimeMillis())).await()
            return code
        }

        suspend fun listExists(code: String): Boolean =
            Firebase.firestore.collection("lists").document(code).get().await().exists()
    }
}
