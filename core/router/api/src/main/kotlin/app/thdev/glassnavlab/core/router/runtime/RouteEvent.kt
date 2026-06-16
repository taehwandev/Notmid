package app.thdev.glassnavlab.core.router.runtime

/**
 * Open marker for feature-owned route events.
 *
 * Core keeps this open because feature API modules own their own closed event
 * families, usually as sealed interfaces.
 */
interface RouteEvent
