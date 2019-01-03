package com.tictactoe.fieldservice.dao

import com.tictactoe.fieldservice.domain.CellStore
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component

@Component
interface CellRepository : CrudRepository<CellStore, String>