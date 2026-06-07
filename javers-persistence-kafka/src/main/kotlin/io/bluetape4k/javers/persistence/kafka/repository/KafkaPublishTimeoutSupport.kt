package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.support.requireGt
import java.time.Duration

internal fun Duration.requirePositivePublishTimeout(parameterName: String = "publishTimeout"): Duration =
    requireGt(Duration.ZERO, parameterName)
