package io.github.nomisRev.kafka

import java.util.Properties
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.CreateTopicsOptions
import org.apache.kafka.clients.admin.DeleteTopicsOptions
import org.apache.kafka.clients.admin.DescribeTopicsOptions
import org.apache.kafka.clients.admin.ListTopicsOptions
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.TopicDescription

/**
 * <!--- TEST_NAME AdminSpec -->
 *
 * Construct an [AutoCloseable] [Admin] with [AdminSettings]. Always consume safely with
 * [kotlin.use], or arrow.fx.coroutines.Resource.
 *
 * <!--- INCLUDE
 * import arrow.continuations.SuspendApp
 * import io.github.nomisRev.kafka.Admin
 * import io.github.nomisRev.kafka.AdminSettings
 * import io.github.nomisRev.kafka.await
 * import io.github.nomisRev.kafka.createTopic
 * import io.github.nomisRev.kafka.deleteTopic
 * import org.apache.kafka.clients.ClientDnsLookup
 * import org.apache.kafka.clients.admin.AdminClientConfig.CLIENT_DNS_LOOKUP_CONFIG
 * import org.apache.kafka.clients.admin.NewTopic
 * import java.util.Properties
 * -->
 * ```kotlin
 * fun main() = SuspendApp {
 *   val settings = AdminSettings(
 *     Kafka.container.bootstrapServers,
 *     Properties().apply {
 *       put(CLIENT_DNS_LOOKUP_CONFIG, ClientDnsLookup.USE_ALL_DNS_IPS)
 *     }
 *   )
 *   Admin(settings).use { admin ->
 *     admin.createTopic(NewTopic("admin-settings-example", 1, 1))
 *     val topics = admin.listTopics().namesToListings().await()
 *     println(topics)
 *     admin.deleteTopic("admin-settings-example")
 *   }
 * }
 * ```
 * <!--- KNIT example-admin-01.kt -->
 * <!--- TEST lines.isNotEmpty() -->
 */
public fun Admin(settings: AdminSettings): Admin = Admin.create(settings.properties())

/** Extension method on [Admin] to create a Topic in a suspending way. */
public suspend fun Admin.createTopic(
  topic: NewTopic,
  option: CreateTopicsOptions = CreateTopicsOptions(),
): Unit = createTopic(listOf(topic), option)

/** Extension method on [Admin] to create Topics in a suspending way. */
public suspend fun Admin.createTopic(
  topic: Iterable<NewTopic>,
  option: CreateTopicsOptions = CreateTopicsOptions(),
): Unit {
  createTopics(topic.toList(), option).all().await()
}

public suspend fun Admin.topicExists(
  topic: NewTopic,
  listTopicsOptions: ListTopicsOptions = ListTopicsOptions(),
): Boolean = listTopics(listTopicsOptions).names().await().contains(topic.name())

/** Extension method on [Admin] to delete a single Topic in a suspending way. */
public suspend fun Admin.deleteTopic(
  name: String,
  options: DeleteTopicsOptions = DeleteTopicsOptions(),
): Unit {
  deleteTopics(listOf(name), options).all().await()
}

/** Extension method to describe a single Topic */
public suspend fun Admin.describeTopic(
  name: String,
  options: DescribeTopicsOptions = DescribeTopicsOptions(),
): TopicDescription? =
  describeTopics(listOf(name), options).topicNameValues().getOrDefault(name, null)?.await()

/**
 * Typed data class for creating a valid [Admin] instance. The only required parameter is the
 * [bootstrapServer], and all other optional parameters can be added using the [props] parameter.
 *
 * @see [AdminClientConfig] for additional kafka [Admin] parameters
 *
 * If you want to request an important _stable_ kafka parameter to be added to the data class,
 * please open an issue on the kotlin-kafka repo.
 */
public data class AdminSettings(
  val bootstrapServer: String,
  private val props: Properties? = null,
) {
  public fun properties(): Properties =
    Properties().apply {
      props?.let { putAll(it) }
      put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    }
}
