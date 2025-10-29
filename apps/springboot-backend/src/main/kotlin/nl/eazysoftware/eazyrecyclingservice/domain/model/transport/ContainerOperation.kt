package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

/**
 * Container operation type
 *
 * Describes the action to perform with a waste container
 *
 * Exchange: Exchange a waste container for a new one
 * Pick up: Pick up a waste container and don't return it
 * Empty: Empty a waste container and return it empty to the pickup location
 * Delivery: Deliver a waste container
 */
enum class ContainerOperation {
  EXCHANGE,
  PICKUP,
  EMPTY,
  DELIVERY,
}

