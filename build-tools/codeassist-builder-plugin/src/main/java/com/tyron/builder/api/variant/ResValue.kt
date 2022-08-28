package com.tyron.builder.api.variant

import java.io.Serializable

/**
 * Resource to be generated by the build system.
 */
class ResValue(
    /**
     * value for the resource
     */
    val value: String,

    /**
     * Optional comment.
     */
    val comment: String? = "Added from the variant API"): Serializable {

    /**
     * a generated resource is identified by its type+name as you can create
     * @string/foo and @dimen/foo,
     *
     * To create instances of [Key], use [Variant.makeResValueKey] function. This interface
     * should not be extended by third party plugin or build scripts.
     */
    interface Key: Serializable {
        /**
         * type of the resource like 'string'
         */
        val type: String

        /**
         * name of the resource.
         */
        val name: String
    }
}