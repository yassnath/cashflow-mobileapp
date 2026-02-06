package com.solvix.tabungan

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SupabaseClient {
  private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
  }

  val client = createSupabaseClient(
    supabaseUrl = "https://idxosoeqtsncwyjwsxeb.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlkeG9zb2VxdHNuY3d5andzeGViIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njk4MjQzNDYsImV4cCI6MjA4NTQwMDM0Nn0.qbsigizJL5nQpXSNd2M16GwHyPQoXp4eQMf2tGF3lVY",
  ) {
    install(Postgrest) {
      serializer = KotlinXSerializer(json)
    }
  }
}

@Serializable
data class SupabaseUser(
  val id: String = "",
  val name: String = "",
  val email: String = "",
  val country: String = "",
  val bio: String = "",
  val birthdate: String = "",
  @SerialName("created_at")
  val createdAt: String = "",
  val username: String = "",
  val password: String = "",
)

@Serializable
data class SupabaseMoneyEntry(
  val id: String = "",
  @SerialName("user_id")
  val userId: String = "",
  val type: String = "",
  val amount: Int = 0,
  val date: String = "",
  @SerialName("created_at")
  val createdAt: String = "",
  val category: String = "",
  val note: String = "",
  @SerialName("source_method")
  val sourceOrMethod: String = "",
  @SerialName("channel_bank")
  val channelOrBank: String = "",
)

@Serializable
data class SupabaseDreamEntry(
  val id: String = "",
  @SerialName("user_id")
  val userId: String = "",
  val title: String = "",
  val target: Int = 0,
  val current: Int = 0,
  val deadline: String = "",
  val note: String = "",
)
