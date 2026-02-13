package com.powerlifting.server.data.repository.mapper

import com.powerlifting.server.db.tables.UsersTable
import com.powerlifting.server.domain.model.User
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toUser() = User(
    id = this[UsersTable.id].value,
    firebaseUid = this[UsersTable.firebaseUid],
    email = this[UsersTable.email],
    displayName = this[UsersTable.displayName]
)
