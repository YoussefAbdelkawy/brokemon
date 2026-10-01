package com.joecode.brokemon

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType

fun testBro(
    id: Long = 1,
    name: String = "Sam",
    type1: BroType = BroType.HYPE,
    type2: BroType? = null,
    catchDate: Long = 0L,
    lastCheckIn: Long? = null,
) = Bro(
    id = id,
    name = name,
    type1 = type1.name,
    type2 = type2?.name,
    catchDate = catchDate,
    lastCheckIn = lastCheckIn,
    avatarSeed = id * 31,
)
